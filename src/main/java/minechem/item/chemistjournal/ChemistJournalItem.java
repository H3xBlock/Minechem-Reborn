package minechem.item.chemistjournal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import minechem.registry.ItemRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleMenuProvider;

/**
 * The chemist journal: it keeps the item it was last open on (the active item) and its owner. Right-click sets the owner to
 * the player's name on the server, and opens the journal's page (ChemistJournalMenu, ChemistJournalScreen). The journal is
 * in the creative tab.
 */
public class ChemistJournalItem extends Item {
    public ChemistJournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ItemStack journal = player.getItemInHand(hand);
            setOwner(journal, player.getName().getString());
            List<String> discovered = journal.getOrDefault(ItemRegistry.JOURNAL_DISCOVERED.get(), List.of());
            String active = activeName(journal) == null ? "" : activeName(journal);
            serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ChemistJournalMenu(id, inventory, discovered, active, hand), Component.literal("Chemist's Journal")),
                buffer -> ChemistJournalMenu.writeOpenData(buffer, discovered, active));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        for (String line : ChemistJournalTooltip.lines(activeName(stack), owner(stack))) {
            builder.accept(Component.literal(line));
        }
    }

    /**
     * Writes a discovered chemical into the journal's list, once; the first one is kept.
     * Returns true when it was added.
     */
    public static boolean addDiscovered(ItemStack journal, String name) {
        List<String> discovered = new ArrayList<>(journal.getOrDefault(ItemRegistry.JOURNAL_DISCOVERED.get(), List.of()));
        if (discovered.contains(name)) {
            return false;
        }
        discovered.add(name);
        journal.set(ItemRegistry.JOURNAL_DISCOVERED.get(), discovered);
        return true;
    }

    /** The name of the active item, or null when none is selected. */
    public static String activeName(ItemStack journal) {
        String name = journal.get(ItemRegistry.JOURNAL_ACTIVE.get());
        return name == null || name.isEmpty() ? null : name;
    }

    public static void setActive(ItemStack journal, String name) {
        journal.set(ItemRegistry.JOURNAL_ACTIVE.get(), name);
    }

    /** The owner's name, or null when the journal was never opened. */
    public static String owner(ItemStack journal) {
        return journal.get(ItemRegistry.JOURNAL_OWNER.get());
    }

    public static void setOwner(ItemStack journal, String name) {
        journal.set(ItemRegistry.JOURNAL_OWNER.get(), name);
    }
}
