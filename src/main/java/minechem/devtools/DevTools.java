package minechem.devtools;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import minechem.Compendium;
import minechem.registry.BlockRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Development tools, registered only by the dev client run (system property minechem.devtools, set in build.gradle): the dev
 * pickaxe, and the command {@code /minechem uranium [radius]} that lists the nearest uranium ore (an x-ray in chat). Nothing here
 * is in the game's data or the published behaviour.
 */
public final class DevTools {
    private static final int DEFAULT_RADIUS = 48;
    private static final int MAX_RADIUS = 64;
    private static final int LISTED = 10;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Compendium.Naming.id);
    public static final DeferredItem<DevPickaxeItem> DEV_PICKAXE = ITEMS.registerItem("dev_pickaxe", DevPickaxeItem::new);

    private DevTools() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> registerCommands(event.getDispatcher()));
    }

    private static void registerCommands(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("minechem")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("devpickaxe").executes(DevTools::givePickaxe))
            .then(Commands.literal("uranium")
                .executes(context -> listUranium(context, DEFAULT_RADIUS))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
                    .executes(context -> listUranium(context, IntegerArgumentType.getInteger(context, "radius"))))));
    }

    private static int givePickaxe(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        player.getInventory().add(new ItemStack(DEV_PICKAXE.get()));
        return 1;
    }

    /** Lists the nearest uranium ore within the radius, in loaded chunks only (nothing is loaded by the scan). */
    private static int listUranium(CommandContext<CommandSourceStack> context, int radius) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        BlockPos centre = player.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        int minY = Math.max(level.getMinY(), centre.getY() - radius);
        int maxY = Math.min(level.getMaxY(), centre.getY() + radius);
        for (int x = centre.getX() - radius; x <= centre.getX() + radius; x++) {
            for (int z = centre.getZ() - radius; z <= centre.getZ() + radius; z++) {
                if (!level.hasChunkAt(new BlockPos(x, centre.getY(), z))) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(BlockRegistry.URANIUM_ORE.get())) {
                        found.add(pos);
                    }
                }
            }
        }
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(centre)));
        if (found.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No uranium ore within " + radius + " blocks (loaded chunks only)."), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Uranium ore: " + found.size() + " blocks within " + radius + ", nearest first:"), false);
        for (BlockPos pos : found.subList(0, Math.min(LISTED, found.size()))) {
            int distance = (int) Math.round(Math.sqrt(pos.distSqr(centre)));
            source.sendSuccess(() -> Component.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ() + "  (" + distance + " blocks)"), false);
        }
        return found.size();
    }
}
