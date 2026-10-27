package minechem.radiation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import minechem.Compendium;
import minechem.chemical.ChemicalBase;
import minechem.chemical.Element;
import minechem.chemical.Molecule;
import minechem.fluid.ChemicalBuckets;
import minechem.helper.Jenkins;
import minechem.item.chemical.ChemicalItem;
import minechem.registry.ItemRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Each server tick, the radioactive stacks of the player's inventory, and of the open container, are updated. When a stack's life
 * has passed it does its damage to the player and decays: an element into the element below (Z - 1), a molecule into its atoms, a
 * bucket of an element into the bucket of the element below, a bucket of a molecule into a bucket of its first atom and the other
 * atoms as items. The stack keeps its size, and the player gets a chat warning.
 *
 * <p>The open container decides what is updated. A no-decay container ({@link INoDecay}) keeps its own stacks: only their clocks
 * move on. A shielding container ({@link IRadiationShield}) reduces the damage of everything updated while it is open. A container
 * that is neither is not updated here. A container that is not no-decay gets only its own stacks updated, not the player's.
 *
 * <p>Molecule components come from the formula (see {@link FormulaParser}). A molecule bucket gives the atoms of one bucket (8
 * units), the first atom as a bucket and the rest as items. The shield factors of the armour are added to the container's, and the
 * total is applied once. The armour loses no durability. The only shield armour is the hazmat suit. The bucket is looked up from
 * its fluid on every update.
 */
@EventBusSubscriber(modid = Compendium.Naming.id)
public final class RadiationTicker {

    /** The largest stack the atoms of a molecule are split into. */
    private static final int MAX_STACK = 64;
    /** Units of a molecule in one bucket: 8. */
    private static final int UNITS_PER_BUCKET = 8;
    /** The slots of the armour the player wears. */
    private static final EquipmentSlot[] ARMOUR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private RadiationTicker() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        update(player, openContainer(player), player.level().getGameTime(), player.level().dimension().identifier().toString());
    }

    /**
     * The open container of the player: the first container of an open menu slot that is not the player's own inventory and is
     * no-decay or shielding, or null (the player's inventory menu, or a plain container).
     */
    public static Container openContainer(Player player) {
        for (Slot slot : player.containerMenu.slots) {
            Container container = slot.container;
            if (container != player.getInventory() && (container instanceof INoDecay || container instanceof IRadiationShield)) {
                return container;
            }
        }
        return null;
    }

    /**
     * Updates the radioactive stacks at world time {@code now}, in dimension {@code dimension}, for the player with the open container
     * {@code open} (null when none). Returns the damage done to the player.
     */
    public static int update(Player player, Container open, long now, String dimension) {
        Container inventory = player.getInventory();
        float armour = armourReduction(player);
        if (open == null) {
            return updateStacks(player, inventory, armour, now, dimension);
        }
        float reduction = armour + (open instanceof IRadiationShield shield ? shield.radiationReductionFactor() : 0F);
        if (open instanceof INoDecay) {
            advanceClocks(open, now, dimension);
            return updateStacks(player, inventory, reduction, now, dimension);
        }
        return updateStacks(player, open, reduction, now, dimension);
    }

    /**
     * The shield factors of the armour the player wears: each piece whose item is an {@link IRadiationShield} adds its factor
     * The hazmat suit pieces are the shield armour.
     */
    static float armourReduction(Player player) {
        float reduction = 0F;
        for (EquipmentSlot slot : ARMOUR_SLOTS) {
            if (player.getItemBySlot(slot).getItem() instanceof IRadiationShield shield) {
                reduction += shield.radiationReductionFactor();
            }
        }
        return reduction;
    }

    /** Updates the stacks of one container (the player's inventory, or an open container), returns the damage done to the player. */
    private static int updateStacks(Player player, Container container, float reduction, long now, String dimension) {
        int damage = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            ChemicalBase chemical = chemicalOf(stack);
            if (stack.isEmpty() || chemical == null) {
                continue;
            }
            RadiationEnum level = levelOf(chemical);
            if (level == RadiationEnum.stable) {
                continue;
            }
            RadiationState before = stack.get(ItemRegistry.RADIATION.get());
            long time = before == null ? 0 : now - before.decayStarted();
            RadiationDecay.Result result = RadiationDecay.step(before, level, stack.getCount(), now, dimension);
            if (!result.decayed()) {
                setState(stack, level, result.state());
                continue;
            }
            List<ItemStack> produced = isBucket(stack) ? decayBucket(chemical, stack.getCount()) : decay(chemical, stack.getCount());
            ItemStack first = produced.isEmpty() ? ItemStack.EMPTY : produced.get(0);
            if (!first.isEmpty()) {
                setState(first, levelOf(chemicalOf(first)), result.state());
            }
            container.setItem(slot, first);
            for (ItemStack rest : produced.subList(Math.min(1, produced.size()), produced.size())) {
                giveToPlayer(player, rest);
            }
            int applied = RadiationDecay.reduce(result.damage(), reduction);
            damage += applied;
            if (applied > 0) {
                player.hurt(player.damageSources().generic(), applied);
            }
            String into = first.isEmpty() ? "nothing" : chemicalOf(first).fullName;
            player.sendSystemMessage(Component.literal(String.format(
                "Radiation Warning: Element %s decayed into %s after %s.", chemical.fullName, into, time)));
        }
        return damage;
    }

    /** The clocks of the radioactive stacks of a no-decay container move on, but nothing decays. */
    private static void advanceClocks(Container container, long now, String dimension) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            ChemicalBase chemical = chemicalOf(stack);
            if (stack.isEmpty() || chemical == null) {
                continue;
            }
            RadiationEnum level = levelOf(chemical);
            if (level == RadiationEnum.stable) {
                continue;
            }
            RadiationState before = stack.get(ItemRegistry.RADIATION.get());
            RadiationState state = before == null
                ? RadiationDecay.step(null, level, stack.getCount(), now, dimension).state()
                : new RadiationState(before.decayStarted() + (now - before.lastDecayUpdate()), now, before.dimension());
            stack.set(ItemRegistry.RADIATION.get(), state);
        }
    }

    /**
     * The chemical a stack stands for: a chemical item by its data component, a bucket by its fluid (the molecule and element
     * fluids). Null for anything else, including the vanilla water bucket.
     */
    static ChemicalBase chemicalOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof BucketItem) {
            return ChemicalBuckets.chemicalOfBucket(stack);
        }
        return ChemicalItem.getChemicalBase(stack);
    }

    private static boolean isBucket(ItemStack stack) {
        return stack.getItem() instanceof BucketItem;
    }

    /** The radioactivity of a chemical: an element by its symbol, a molecule by its formula. */
    public static RadiationEnum levelOf(ChemicalBase chemical) {
        if (chemical instanceof Element element) {
            return RadiationRules.ofElement(element.shortName);
        }
        if (chemical instanceof Molecule molecule) {
            return RadiationRules.ofFormula(molecule.getFormula());
        }
        return RadiationEnum.stable;
    }

    /**
     * What a decaying stack becomes. An element becomes the element below. A molecule
     * becomes its atoms: each radioactive element of the formula goes one step down, and the atoms are made into stacks of at most
     * 64, the first of them replacing the stack.
     */
    static List<ItemStack> decay(ChemicalBase chemical, int count) {
        List<ItemStack> produced = new ArrayList<>();
        if (chemical instanceof Element element) {
            produced.add(stackOf(Jenkins.get(element.atomicNumber - 1), count));
            return produced;
        }
        Map<Integer, Integer> atoms = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : FormulaParser.parse(chemical.getFormula()).entrySet()) {
            Element element = elementBySymbol(entry.getKey());
            if (element == null) {
                continue;
            }
            int atomicNumber = element.atomicNumber;
            if (RadiationRules.ofElement(element.shortName) != RadiationEnum.stable) {
                atomicNumber--;
            }
            atoms.merge(atomicNumber, entry.getValue() * count, Integer::sum);
        }
        for (Map.Entry<Integer, Integer> entry : atoms.entrySet()) {
            Element element = Jenkins.get(entry.getKey());
            for (int left = entry.getValue(); left > 0; left -= MAX_STACK) {
                produced.add(stackOf(element, Math.min(MAX_STACK, left)));
            }
        }
        return produced;
    }

    /**
     * What a decaying bucket becomes. An element bucket becomes the bucket of the element below. A molecule bucket becomes the atoms of one bucket's worth of the molecule: the first
     * atom as a bucket, the others as items.
     */
    static List<ItemStack> decayBucket(ChemicalBase chemical, int count) {
        List<ItemStack> produced = new ArrayList<>();
        if (chemical instanceof Element element) {
            ItemStack bucket = ChemicalBuckets.bucketOf(Jenkins.get(element.atomicNumber - 1));
            bucket.setCount(count);
            produced.add(bucket);
            return produced;
        }
        List<ItemStack> atoms = decay(chemical, UNITS_PER_BUCKET * count);
        ItemStack first = atoms.get(0);
        // The first stack becomes the bucket, with its size divided by 8 (a stack of 8 atoms is one bucket).
        ItemStack bucket = ChemicalBuckets.bucketOf(chemicalOf(first));
        bucket.setCount(first.getCount() / UNITS_PER_BUCKET);
        if (!bucket.isEmpty()) {
            produced.add(bucket);
        }
        produced.addAll(atoms.subList(1, atoms.size()));
        return produced;
    }

    private static ItemStack stackOf(ChemicalBase chemical, int count) {
        ItemStack stack = ChemicalItem.stackFor(chemical);
        stack.setCount(count);
        return stack;
    }

    private static Element elementBySymbol(String symbol) {
        for (ChemicalBase chemical : Jenkins.getAll()) {
            if (chemical instanceof Element element && element.shortName.equals(symbol)) {
                return element;
            }
        }
        return null;
    }

    /** Puts a stack into the player's inventory, and drops what does not fit. */
    private static void giveToPlayer(Player player, ItemStack stack) {
        ItemStack rest = stack;
        player.getInventory().add(rest);
        if (!rest.isEmpty()) {
            player.drop(rest, false);
        }
    }

    /** A stack of a container that decayed in one update: what it was, what it became (null for nothing) and its damage. */
    public record Decay(ChemicalBase before, ChemicalBase after, int damage) {
    }

    /**
     * Decays the radioactive stacks of a container that has no player, such as a turtle's inventory. A decayed stack is replaced
     * by its first product, and the other products go to the free slots, and are lost when there is none. The damage is only
     * reported: a container cannot be hurt. Returns the decays of this update.
     */
    public static List<Decay> decayContainer(Container container, long now, String dimension) {
        List<Decay> decays = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            ChemicalBase chemical = chemicalOf(stack);
            if (stack.isEmpty() || chemical == null) {
                continue;
            }
            RadiationEnum level = levelOf(chemical);
            if (level == RadiationEnum.stable) {
                continue;
            }
            RadiationState before = stack.get(ItemRegistry.RADIATION.get());
            RadiationDecay.Result result = RadiationDecay.step(before, level, stack.getCount(), now, dimension);
            if (!result.decayed()) {
                setState(stack, level, result.state());
                continue;
            }
            List<ItemStack> produced = isBucket(stack) ? decayBucket(chemical, stack.getCount()) : decay(chemical, stack.getCount());
            ItemStack first = produced.isEmpty() ? ItemStack.EMPTY : produced.get(0);
            if (!first.isEmpty()) {
                setState(first, levelOf(chemicalOf(first)), result.state());
            }
            container.setItem(slot, first);
            for (ItemStack rest : produced.subList(Math.min(1, produced.size()), produced.size())) {
                placeInFreeSlot(container, rest);
            }
            decays.add(new Decay(chemical, first.isEmpty() ? null : chemicalOf(first), result.damage()));
        }
        return decays;
    }

    private static void placeInFreeSlot(Container container, ItemStack stack) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                container.setItem(slot, stack);
                return;
            }
        }
    }

    private static void setState(ItemStack stack, RadiationEnum level, RadiationState state) {
        if (level == RadiationEnum.stable) {
            stack.remove(ItemRegistry.RADIATION.get());
        } else {
            stack.set(ItemRegistry.RADIATION.get(), state);
        }
    }
}
