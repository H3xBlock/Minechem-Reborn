package minechem.item.chemistjournal;

import java.util.List;
import minechem.registry.MenuRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * The journal's menu. It has no slots: the journal GUI only shows data and never moves items. The journal opens from the
 * item, so the menu needs no block.
 */
public class ChemistJournalMenu extends AbstractContainerMenu {
    private final List<String> discovered;
    private String active;
    /** The hand that holds the journal this menu was opened from (the server's menu; the client's never needs it). */
    private final InteractionHand hand;

    /** @param discovered the discovered chemicals by name; @param active the active chemical's name, or "" when none. */
    public ChemistJournalMenu(int id, Inventory playerInventory, List<String> discovered, String active) {
        this(id, playerInventory, discovered, active, InteractionHand.MAIN_HAND);
    }

    /** @param hand the hand that holds the journal (ChemistJournalItem.use opens it from either hand). */
    public ChemistJournalMenu(int id, Inventory playerInventory, List<String> discovered, String active, InteractionHand hand) {
        super(MenuRegistry.CHEMIST_JOURNAL.get(), id);
        this.discovered = List.copyOf(discovered);
        this.active = active;
        this.hand = hand;
    }

    /** Shows another active chemical on the client's page (the server gets the same choice through clickMenuButton). */
    public void setActiveLocal(String name) {
        this.active = name;
    }

    /**
     * A click on the list makes the item at that index the journal's active item. The journal is the one in the hand it
     * was opened from.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= discovered.size()) {
            return false;
        }
        ItemStack journal = player.getItemInHand(hand);
        if (!(journal.getItem() instanceof ChemistJournalItem)) {
            return false;
        }
        ChemistJournalItem.setActive(journal, discovered.get(id));
        return true;
    }

    /** The discovered chemicals, in discovery order: what the client's journal page lists. */
    public List<String> discovered() {
        return discovered;
    }

    /** The active chemical's name, or "" when none is selected. */
    public String active() {
        return active;
    }

    /** Writes what the client needs to open the journal: the discovered names, then the active name. */
    public static void writeOpenData(FriendlyByteBuf buffer, List<String> discovered, String active) {
        buffer.writeCollection(discovered, FriendlyByteBuf::writeUtf);
        buffer.writeUtf(active);
    }

    /** Reads what {@link #writeOpenData} wrote. */
    public static ChemistJournalMenu read(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        List<String> discovered = buffer.readCollection(java.util.ArrayList::new, FriendlyByteBuf::readUtf);
        return new ChemistJournalMenu(id, playerInventory, discovered, buffer.readUtf());
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** Shift-click moves nothing. */
    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
