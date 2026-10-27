package minechem.apparatus.prefab.gui.tab;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * A side tab on the left edge of a machine screen. Clicking its header opens or closes the panel; the panel slides out
 * to the left.
 *
 * <p>No icon is drawn: the header is a coloured bar with the tab's label, drawn with the game's font.
 */
public final class SideTab {
    private static final int HEADER_WIDTH = 16;
    private static final int HEADER_HEIGHT = 16;
    private static final int PANEL_WIDTH = 128;
    private static final int TEXT_COLOUR = 0xFF000000;
    private static final int HEADER_TEXT = 0xFFFFFFFF;
    private static final float STEP = 0.15F;

    private final Component label;
    private final IntSupplier accent;
    private final Supplier<List<FormattedCharSequence>> lines;
    private boolean open;
    private float progress;

    /**
     * @param label  the header text (shown in the bar)
     * @param accent the header and panel colour, read each frame (the state tab changes colour with the machine)
     * @param lines  the panel text, read each frame
     */
    public SideTab(Component label, IntSupplier accent, Supplier<List<FormattedCharSequence>> lines) {
        this.label = label;
        this.accent = accent;
        this.lines = lines;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    /** Called once per client tick. */
    public void tick() {
        float target = open ? 1.0F : 0.0F;
        if (progress < target) {
            progress = Math.min(target, progress + STEP);
        } else if (progress > target) {
            progress = Math.max(target, progress - STEP);
        }
    }

    /** Draws the tab with its right edge at {@code x} (the screen's left edge) and its top at {@code y}. */
    public void extract(GuiGraphicsExtractor graphics, Font font, int x, int y) {
        int width = HEADER_WIDTH + Math.round((PANEL_WIDTH - HEADER_WIDTH) * progress);
        int left = x - width;
        int height = HEADER_HEIGHT + Math.round(panelHeight(font) * progress);
        graphics.fill(left, y, x, y + height, accent.getAsInt());
        graphics.text(font, label.getString().substring(0, 1), x - HEADER_WIDTH / 2 - 3, y + 4, HEADER_TEXT, true);
        if (progress < 1.0F) {
            return;
        }
        graphics.text(font, label, left + 4, y + 4, HEADER_TEXT, true);
        int textY = y + HEADER_HEIGHT + 4;
        for (FormattedCharSequence line : lines.get()) {
            graphics.text(font, line, left + 4, textY, TEXT_COLOUR, false);
            textY += font.lineHeight;
        }
    }

    /** Height of the open panel: the header plus the lines it shows (at least one line of space). */
    private int panelHeight(Font font) {
        return HEADER_HEIGHT + 8 + Math.max(1, lines.get().size()) * font.lineHeight;
    }

    /** Returns true when the click was on the header, which toggles the panel. */
    public boolean mouseClicked(double mouseX, double mouseY, int x, int y) {
        if (mouseX >= x - HEADER_WIDTH && mouseX < x && mouseY >= y && mouseY < y + HEADER_HEIGHT) {
            open = !open;
            return true;
        }
        return false;
    }
}
