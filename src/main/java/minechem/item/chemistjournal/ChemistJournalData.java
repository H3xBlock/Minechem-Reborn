package minechem.item.chemistjournal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * The pure data of the chemist journal: the discovered items, the active item the journal is open on, the owner, and the
 * cloning result. Generic over the item type T; the caller says when two items are the same (equality ignores the
 * count).
 *
 * <p>The cloning recipe always gives two journals, whatever the count of the journal it was made from. An item is
 * discovered once, and the first discovery is kept.
 */
public final class ChemistJournalData<T> {


    /** One journal per slot. */
    public static final int JOURNAL_SLOT_STACK_LIMIT = 1;

    /** Only a journal may be put in a journal slot. */
    public static boolean acceptsInJournalSlot(boolean isJournal) {
        return isJournal;
    }

    /** The output count of the cloning recipe. */
    public static final int CLONE_RESULT_COUNT = 2;

    private final BiPredicate<T, T> sameItem;
    private final List<T> discovered = new ArrayList<>();
    private T activeStack;
    private String owner = "";

    public ChemistJournalData(BiPredicate<T, T> sameItem) {
        this.sameItem = sameItem;
    }

    /** Adds the item unless one of the same kind was already discovered. */
    public boolean addDiscovered(T item) {
        if (hasDiscovered(item)) {
            return false;
        }
        discovered.add(item);
        return true;
    }

    public boolean hasDiscovered(T item) {
        for (T known : discovered) {
            if (sameItem.test(known, item)) {
                return true;
            }
        }
        return false;
    }

    /** The discovered items in the order they were found. */
    public List<T> discovered() {
        return List.copyOf(discovered);
    }

    /** Sets the item the journal is open on. */
    public void setActive(T item) {
        this.activeStack = item;
    }

    /** The active item; empty when the journal has none. */
    public Optional<T> active() {
        return Optional.ofNullable(activeStack);
    }

    public String owner() {
        return owner;
    }

    /** The journal's owner, set when the journal is first opened. */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /** The count of the journals the cloning recipe gives. */
    public static int cloneResultCount() {
        return CLONE_RESULT_COUNT;
    }
}
