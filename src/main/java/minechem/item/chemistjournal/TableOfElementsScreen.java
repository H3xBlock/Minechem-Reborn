package minechem.item.chemistjournal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import minechem.Compendium;
import minechem.chemical.Element;
import minechem.helper.Jenkins;
import minechem.radiation.RadiationEnum;
import minechem.radiation.RadiationRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Shows the periodic table as a journal page: the table's background (the TableGUI texture, drawn at a 876 x 600 GUI size)
 * and a button back to the journal it was opened from. The journal screen has a Table button that opens this screen.
 *
 * <p>The whole texture is shown, scaled to fit the window with its proportions kept, and centred.
 *
 * <p>Each element is a cell of the table. Hovering a cell shows its name, its form and its radioactivity. Clicking a discovered
 * element opens its page in the journal; an element not yet discovered is dimmed and gives no page. The texture shows elements
 * 1 to 113 only, so elements 114 to 118 are drawn as extra cells in the empty places of the seventh row.
 */
public class TableOfElementsScreen extends Screen {
    public static final int GUI_WIDTH = 876;
    public static final int GUI_HEIGHT = 600;
    private static final Identifier TABLE = Identifier.fromNamespaceAndPath(Compendium.Naming.id, "textures/gui/table.png");
    private static final int TEXTURE_WIDTH = 545;
    private static final int TEXTURE_HEIGHT = 272;

    /** Cell grid in texture pixels, measured on the texture: column 1 starts at 17, each column is 28.83 wide. */
    private static final double COLUMN_LEFT = 17.0;
    private static final double COLUMN_PITCH = 28.83;
    /** Row 1 starts at 18, each main row is 24.9 high; the lanthanide and actinide rows follow at 214 and 238. */
    private static final double ROW_TOP = 18.0;
    private static final double ROW_PITCH = 24.9;
    private static final double LANTHANIDE_TOP = 214.0;
    private static final double ACTINIDE_TOP = 238.0;
    private static final double ROW_HEIGHT = 23.5;
    private static final double CELL_WIDTH = COLUMN_PITCH - 1.5;

    private static final int DIM = 0xAA000000;
    private static final int HOVER = 0x55FFFFFF;
    private static final int EXTRA_CELL = 0xFF666B70;

    /** Every element with its cell in texture pixels; built once, from the chemistry data. */
    private static final List<Cell> CELLS = buildCells();

    private record Cell(Element element, double x, double y, double width, double height) {
        boolean contains(double tx, double ty) {
            return tx >= x && tx < x + width && ty >= y && ty < y + height;
        }
    }

    private final Screen previous;

    public TableOfElementsScreen(Screen previous) {
        super(Component.translatable("gui.minechem.table"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.minechem.journal"), button -> minecraft.setScreen(previous))
            .bounds(width / 2 - 40, height - 28, 80, 20)
            .build());
    }

    /** The scale that fits the whole texture in the window, keeping its proportions (a fit between the title and the button). */
    private float scale() {
        return Math.min((width - 32.0F) / TEXTURE_WIDTH, (height - 64.0F) / TEXTURE_HEIGHT);
    }

    private int left() {
        return (width - Math.round(TEXTURE_WIDTH * scale())) / 2;
    }

    private int top() {
        return (height - Math.round(TEXTURE_HEIGHT * scale())) / 2;
    }

    /** The table is drawn in extractRenderState, so the overlays of the cells are on top of it (no background of its own). */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    /** The whole texture, drawn at its own size and scaled with the pose (a blit sized to the screen would sample only the top-left part). */
    private void drawTable(GuiGraphicsExtractor graphics) {
        float scale = scale();
        graphics.pose().pushMatrix();
        graphics.pose().translate(left(), top());
        graphics.pose().scale(scale, scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TABLE, 0, 0, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        graphics.pose().popMatrix();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.nextStratum();
        drawTable(graphics);
        graphics.nextStratum();
        Font font = minecraft.font;
        graphics.text(font, title, (width - font.width(title)) / 2, 6, 0xFFFFFFFF, true);
        float scale = scale();
        int left = left();
        int top = top();
        List<String> discovered = discovered();
        Cell hovered = cellAt(mouseX, mouseY);
        for (Cell cell : CELLS) {
            int x1 = left + Math.round((float) (cell.x() * scale));
            int y1 = top + Math.round((float) (cell.y() * scale));
            int x2 = left + Math.round((float) ((cell.x() + cell.width()) * scale));
            int y2 = top + Math.round((float) ((cell.y() + cell.height()) * scale));
            if (cell.element().atomicNumber > 113) {
                graphics.fill(x1, y1, x2, y2, EXTRA_CELL);
                graphics.centeredText(font, cell.element().shortName, (x1 + x2) / 2, (y1 + y2) / 2 - 4, 0xFFFFFFFF);
            }
            if (!discovered.contains(cell.element().fullName)) {
                graphics.fill(x1, y1, x2, y2, DIM);
            }
            if (cell == hovered) {
                graphics.fill(x1, y1, x2, y2, HOVER);
            }
        }
        if (hovered != null) {
            graphics.setTooltipForNextFrame(font, tooltipOf(hovered.element(), discovered), Optional.empty(), mouseX, mouseY);
        }
    }

    /**
     * A click on a discovered element opens its page in the journal this table came from. An undiscovered element gives no page
     * (the journal lists only what was found).
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Cell cell = cellAt(event.x(), event.y());
        if (cell != null && previous instanceof ChemistJournalScreen journal && discovered().contains(cell.element().fullName)) {
            if (journal.openChemical(cell.element().fullName)) {
                minecraft.setScreen(journal);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** The journal's discovered names, or none when this table was not opened from the journal. */
    private List<String> discovered() {
        return previous instanceof ChemistJournalScreen journal ? journal.discoveredKeys() : List.of();
    }

    /** The cell under the screen point, or null (the point is converted to texture pixels first). */
    private Cell cellAt(double screenX, double screenY) {
        float scale = scale();
        double tx = (screenX - left()) / scale;
        double ty = (screenY - top()) / scale;
        for (Cell cell : CELLS) {
            if (cell.contains(tx, ty)) {
                return cell;
            }
        }
        return null;
    }

    /** The hover text: the name, the form (and formula) lines, the radioactivity, and a note for an undiscovered element. */
    private static List<Component> tooltipOf(Element element, List<String> discovered) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(element.fullName + " (" + element.shortName + ")"));
        for (String line : element.getToolTip()) {
            lines.add(Component.literal(line));
        }
        RadiationEnum level = RadiationRules.ofElement(element.shortName);
        if (level != RadiationEnum.stable) {
            lines.add(Component.translatable("element.property." + level.name()).withStyle(level.getColour()));
        }
        if (!discovered.contains(element.fullName)) {
            lines.add(Component.translatable("gui.minechem.table.undiscovered"));
        }
        return lines;
    }

    /** Places every element of 1 to 118 on the texture's grid, using its period and group (the standard layout). */
    private static List<Cell> buildCells() {
        List<Cell> cells = new ArrayList<>();
        for (int atomicNumber = 1; atomicNumber <= 118; atomicNumber++) {
            Element element = Jenkins.get(atomicNumber);
            if (element == null) {
                continue;
            }
            int[] slot = slotOf(atomicNumber);
            double x = COLUMN_LEFT + (slot[1] - 1) * COLUMN_PITCH;
            double y = switch (slot[0]) {
                case LANTHANIDE_ROW -> LANTHANIDE_TOP;
                case ACTINIDE_ROW -> ACTINIDE_TOP;
                default -> ROW_TOP + (slot[0] - 1) * ROW_PITCH;
            };
            cells.add(new Cell(element, x, y, CELL_WIDTH, ROW_HEIGHT));
        }
        return cells;
    }

    private static final int LANTHANIDE_ROW = 8;
    private static final int ACTINIDE_ROW = 9;

    /** The row slot (1 to 7 for the main table, 8 lanthanides, 9 actinides) and the column (1 to 18) of an element. */
    static int[] slotOf(int z) {
        if (z == 57) {
            return new int[] {6, 3};
        }
        if (z == 89) {
            return new int[] {7, 3};
        }
        if (z >= 58 && z <= 71) {
            return new int[] {LANTHANIDE_ROW, z - 55};
        }
        if (z >= 90 && z <= 103) {
            return new int[] {ACTINIDE_ROW, z - 87};
        }
        if (z <= 2) {
            return new int[] {1, z == 1 ? 1 : 18};
        }
        if (z <= 4) {
            return new int[] {2, z - 2};
        }
        if (z <= 10) {
            return new int[] {2, z + 8};
        }
        if (z <= 12) {
            return new int[] {3, z - 10};
        }
        if (z <= 18) {
            return new int[] {3, z};
        }
        if (z <= 20) {
            return new int[] {4, z - 18};
        }
        if (z <= 36) {
            return new int[] {4, z - 18};
        }
        if (z <= 38) {
            return new int[] {5, z - 36};
        }
        if (z <= 54) {
            return new int[] {5, z - 36};
        }
        if (z <= 56) {
            return new int[] {6, z - 54};
        }
        if (z <= 86) {
            return new int[] {6, z - 68};
        }
        if (z <= 88) {
            return new int[] {7, z - 86};
        }
        return new int[] {7, z - 100};
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
