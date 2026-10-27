package minechem.item.chemical;

import minechem.chemical.ChemicalBase;
import minechem.chemical.Element;
import minechem.fluid.ElementFluids;
import minechem.fluid.MoleculeFluids;
import minechem.helper.Jenkins;
import minechem.potion.PharmacologyEffects;
import minechem.radiation.FluidRadiationData;
import minechem.radiation.RadiationEnum;
import minechem.radiation.RadiationState;
import minechem.radiation.RadiationRules;
import minechem.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Consumer;

/**
 * One item that stands for any element or molecule, chosen by a data component.
 */
public class ChemicalItem extends Item {
    /** Vials of one molecule per pour. */
    private static final int VIALS_PER_POUR = 8;
    private static final String WATER = "Water";

    public ChemicalItem(Properties properties) {
        super(properties);
    }

    public static ItemStack stackFor(ChemicalBase chemical) {
        ItemStack stack = new ItemStack(ItemRegistry.CHEMICAL.get());
        stack.set(ItemRegistry.CHEMICAL_NAME.get(), chemical.fullName);
        return stack;
    }

    public static ChemicalBase getChemicalBase(ItemStack stack) {
        String name = stack.get(ItemRegistry.CHEMICAL_NAME.get());
        return name == null ? null : Jenkins.get(name);
    }

    /**
     * The name shows a molecule's formula after it, so the complex formula is read without opening the tooltip. Elements keep
     * their name (the symbol is drawn on the item).
     */
    @Override
    public Component getName(ItemStack stack) {
        ChemicalBase chemical = getChemicalBase(stack);
        if (chemical == null) {
            return Component.literal("Generic ChemicalItem");
        }
        if (chemical.isElement()) {
            return Component.literal(chemical.fullName);
        }
        return Component.literal(chemical.fullName + " (" + chemical.getFormula() + ")");
    }

    /** A molecule is drunk (16 ticks, the drink animation). Elements are not drunk. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ChemicalBase chemical = getChemicalBase(player.getItemInHand(hand));
        if (chemical == null || chemical.isElement()) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 16;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    /**
     * The item is used up (not in creative), the effects apply on the server, and the burp plays (volume 0.5, a random
     * pitch).
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ChemicalBase chemical = getChemicalBase(stack);
        stack.consume(1, entity);
        if (level.isClientSide() || chemical == null) {
            return stack;
        }
        PharmacologyEffects.apply(chemical.fullName, entity);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F,
            level.getRandom().nextFloat() * 0.1F + 0.9F);
        return stack;
    }

    /**
     * A vial pours its chemical's liquid into the block in front of the clicked face. Water places the vanilla water. A solid
     * block is not replaced; any other block there (a fluid, a plant) is broken first. A vial's radioactivity goes into the
     * placed liquid (as the dispenser does). In survival, eight vials are used up (from the whole inventory) and eight empty
     * tubes come back. The vials are checked before the block is broken.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        ChemicalBase chemical = getChemicalBase(stack);
        if (player == null || chemical == null || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Block liquid = liquidOf(chemical);
        Level level = context.getLevel();
        BlockPos target = context.getClickedPos().relative(context.getClickedFace());
        BlockState state = level.getBlockState(target);
        if (liquid == null || (!state.isAir() && state.isSolid())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean survival = !player.getAbilities().instabuild;
        if (survival && !takeVials(player, chemical, VIALS_PER_POUR)) {
            return InteractionResult.PASS;
        }
        if (!state.isAir()) {
            level.destroyBlock(target, true);
        }
        level.setBlock(target, liquid.defaultBlockState(), Block.UPDATE_ALL);
        RadiationState radiation = stack.get(ItemRegistry.RADIATION.get());
        if (radiation != null && level instanceof ServerLevel server) {
            FluidRadiationData.get(server).put(target, radiation);
        }
        if (survival) {
            ItemStack tubes = new ItemStack(ItemRegistry.EMPTY_TUBE.get(), VIALS_PER_POUR);
            player.getInventory().add(tubes);
            if (!tubes.isEmpty()) {
                player.drop(tubes, false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The liquid block a chemical pours: water is the vanilla water, an element or a molecule has its own (null when none). */
    private static Block liquidOf(ChemicalBase chemical) {
        if (chemical.fullName.equals(WATER)) {
            return Blocks.WATER;
        }
        if (chemical instanceof Element element) {
            return ElementFluids.blockFor(element.shortName);
        }
        return MoleculeFluids.blockFor(chemical.fullName);
    }

    /** Uses up {@code amount} vials of one molecule from the inventory, if there are that many. */
    private static boolean takeVials(Player player, ChemicalBase chemical, int amount) {
        Inventory inventory = player.getInventory();
        int found = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (isVialOf(inventory.getItem(slot), chemical)) {
                found += inventory.getItem(slot).getCount();
            }
        }
        if (found < amount) {
            return false;
        }
        int left = amount;
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isVialOf(stack, chemical)) {
                int taken = Math.min(left, stack.getCount());
                inventory.removeItem(slot, taken);
                left -= taken;
            }
        }
        return true;
    }

    private static boolean isVialOf(ItemStack stack, ChemicalBase chemical) {
        ChemicalBase other = getChemicalBase(stack);
        return stack.getItem() instanceof ChemicalItem && other != null && other.fullName.equals(chemical.fullName);
    }

    /**
     * The radiation clock writes the stack's RADIATION component on every player tick (RadiationTicker), so the client gets a
     * new stack each tick; NeoForge's default would replay the hand's re-equip animation each time. Only a change of slot,
     * of item or of chemical re-equips.
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged || !newStack.is(this)) {
            return true;
        }
        ChemicalBase before = getChemicalBase(oldStack);
        ChemicalBase after = getChemicalBase(newStack);
        return before == null ? after != null : after == null || !before.fullName.equals(after.fullName);
    }

    /** The chemical's own tooltip lines (form, then the formula for a molecule). */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        ChemicalBase chemical = getChemicalBase(stack);
        if (chemical != null) {
            for (String line : chemical.getToolTip()) {
                builder.accept(Component.literal(line));
            }
        }
        // A radioactive element names its level, in the level's colour.
        if (chemical instanceof Element element) {
            RadiationEnum level = RadiationRules.ofElement(element.shortName);
            if (level != RadiationEnum.stable) {
                Component line = Component.translatable("element.property." + level.name()).withStyle(level.getColour());
                RadiationState state = stack.get(ItemRegistry.RADIATION.get());
                // The tooltip context's level (the client's level when a player hovers); this class also runs on a dedicated
                // server, where the client's Minecraft class does not exist.
                Level world = context.level();
                if (state != null && world != null) {
                    long left = remainingTicks(state, level, world.getGameTime());
                    if (left > 0) {
                        line = line.copy().append(Component.literal(" (" + formatTicks(left) + ")"));
                    }
                }
                builder.accept(line);
            }
        }
    }

    /** The ticks before a stack of this level decays, from its clock: life less the time since the start. */
    public static long remainingTicks(RadiationState state, RadiationEnum level, long now) {
        return level.getLife() - (now - state.decayStarted());
    }

    /** The time left as hours, minutes and seconds. */
    public static String formatTicks(long ticks) {
        long hours = ticks / 72000L;
        long minutes = (ticks % 72000L) / 1200L;
        long seconds = (ticks % 1200L) / 20L;
        if (hours > 0) {
            return hours + "h " + minutes + "min";
        }
        if (minutes > 0) {
            return minutes + "min " + seconds + "s";
        }
        return seconds + "s";
    }
}
