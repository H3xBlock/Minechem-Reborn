package minechem.helper;

import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Item drop helpers. throwItemStack uses Containers.dropItemStack, which spawns the item with a random offset and a small
 * upward motion.
 */
public class ItemHelper {
    public static void throwItemStack(Level level, ItemStack stack, double x, double y, double z) {
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, x, y, z, stack);
        }
    }

    /**
     * Gives the stack to the player, or drops it at their feet if the inventory is full.
     */
    public static void giveToPlayer(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack);
        }
    }
}
