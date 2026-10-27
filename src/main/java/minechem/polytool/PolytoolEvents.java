package minechem.polytool;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import minechem.Compendium;
import minechem.apparatus.decomposer.DecomposerBlockEntity;
import minechem.apparatus.decomposer.DecomposerRecipes;
import minechem.item.chemical.ChemicalItem;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The break speed of the held polytool, and the drops of the mobs it kills (silicon decomposes the drops, nitrogen cooks the
 * zombies, calcium adds bones to the skeletons, beryllium drops heads).
 */
@EventBusSubscriber(modid = Compendium.Naming.id)
public final class PolytoolEvents {
    private static final Random RANDOM = new Random();
    /** The atomic numbers the drop effects read. */
    static final int SILICON = 14;
    static final int NITROGEN = 7;
    static final int CALCIUM = 20;
    static final int BERYLLIUM = 4;

    private PolytoolEvents() {
    }

    /** The mining speed of the held polytool, through the block's break speed. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack held = event.getEntity().getMainHandItem();
        if (held.getItem() instanceof PolytoolItem) {
            event.setNewSpeed(PolytoolItem.getDigSpeed(held, event.getState()));
        }
    }

    @SubscribeEvent
    public static void onDrop(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || !event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof PolytoolItem)) {
            return;
        }
        LivingEntity enemy = event.getEntity();
        float silicon = PolytoolItem.getPowerOfType(stack, SILICON);
        if (silicon > 0) {
            decomposeDrops(event, silicon);
        }
        if (enemy instanceof Zombie) {
            float nitrogen = PolytoolItem.getPowerOfType(stack, NITROGEN);
            if (nitrogen > 0) {
                int amount = (int) Math.ceil(RANDOM.nextDouble() * nitrogen);
                addDrop(event, new ItemStack(Items.COOKED_BEEF, amount));
                event.getDrops().removeIf(drop -> drop.getItem().is(Items.ROTTEN_FLESH));
            }
        }
        if (enemy instanceof AbstractSkeleton) {
            float calcium = PolytoolItem.getPowerOfType(stack, CALCIUM);
            if (calcium > 0) {
                int amount = (int) Math.ceil(RANDOM.nextDouble() * calcium);
                for (ItemEntity drop : event.getDrops()) {
                    if (drop.getItem().is(Items.BONE)) {
                        drop.getItem().grow(amount);
                    }
                }
            }
        }
        float beheading = PolytoolItem.getPowerOfType(stack, BERYLLIUM);
        while (beheading > 5) {
            if (beheading > 0 && RANDOM.nextInt(5) < beheading * 10) {
                if (enemy instanceof AbstractSkeleton skeleton) {
                    addDrop(event, new ItemStack(skeleton instanceof WitherSkeleton ? Items.WITHER_SKELETON_SKULL : Items.SKELETON_SKULL));
                } else if (enemy instanceof Zombie) {
                    addDrop(event, new ItemStack(Items.ZOMBIE_HEAD));
                } else if (enemy instanceof Player) {
                    // The head has no owner: the player's name is not set on the stack.
                    addDrop(event, new ItemStack(Items.PLAYER_HEAD));
                }
            }
            // More head drops if level>5
            beheading--;
        }
    }

    /** Silicon effect: each dropped stack is decomposed into its recipe's chemicals, while the random roll allows it. */
    private static void decomposeDrops(LivingDropsEvent event, float power) {
        if (RANDOM.nextInt(16) >= 1 + power) {
            return;
        }
        List<ItemEntity> result = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack item = drop.getItem();
            while (item.getCount() > 0) {
                DecomposerRecipes.Recipe recipe = DecomposerBlockEntity.RECIPES.get(DecomposerBlockEntity.keyFor(item));
                if (recipe == null) {
                    result.add(drop);
                    break;
                }
                for (DecomposerRecipes.Output output : recipe.outputs()) {
                    result.add(new ItemEntity(drop.level(), drop.getX(), drop.getY(), drop.getZ(),
                        ChemicalItem.stackFor(output.chemical()).copyWithCount(output.amount())));
                }
                item.shrink(1);
            }
        }
        event.getDrops().clear();
        event.getDrops().addAll(result);
    }

    /** Adds a drop that waits ten ticks before it can be picked up. */
    private static void addDrop(LivingDropsEvent event, ItemStack stack) {
        ItemEntity drop = new ItemEntity(event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), stack);
        drop.setPickUpDelay(10);
        event.getDrops().add(drop);
    }
}
