package minechem.item.chemistjournal;

import java.util.ArrayList;
import java.util.List;
import minechem.Compendium;
import minechem.apparatus.decomposer.DecomposerBlockEntity;
import minechem.apparatus.decomposer.DecomposerRecipes;
import minechem.apparatus.synthesis.SynthesisRecipes;
import minechem.item.chemical.ChemicalItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The journal page: the discovered list with its search bar and scroll, the active item's name, the decomposer grid (with
 * the slideshow of a select recipe) and the synthesis grid, and the help text.
 *
 * <p>The recipe grids show the entries of the Synthesis and Decomposer tables.
 */
public class ChemistJournalScreen extends AbstractContainerScreen<ChemistJournalMenu> {
    private static final Identifier GUI = Identifier.fromNamespaceAndPath(Compendium.Naming.id, "textures/gui/chemist_journal.png");

    /** The page size. The base class keeps its own 176 x 166 size, so the page is laid out here. */
    private static final int GUI_WIDTH = 302;
    private static final int GUI_HEIGHT = 191;
    private static final int TABLE_BUTTON_WIDTH = 110;
    /** The texture is 512 square; the page is the top-left part of it, drawn 1:1. */
    private static final int TEXTURE_SIZE = 512;

    /** The list: a 6-column grid of 18-pixel cells, clipped to its box. */
    private static final int LIST_LEFT = 9;
    private static final int LIST_TOP = 26;
    private static final int LIST_WIDTH = 140;
    private static final int LIST_HEIGHT = 156;
    private static final int COLUMNS = 6;
    private static final int CELL = 18;
    private static final int ORIGIN_X = 18;
    private static final int ORIGIN_Y = 28;
    /** The visible part of the list when it scrolls. */
    private static final int VISIBLE_HEIGHT = 130;

    /** The grids' frames (texture cells of 54 pixels) and the ghost items' cells. */
    private static final int DECOMPOSER_X = 197;
    private static final int DECOMPOSER_Y = 41;
    private static final int SYNTHESIS_X = 197;
    private static final int SYNTHESIS_Y = 121;
    private static final int GRID_FRAME = 54;

    /** The scroll bar's slider (at (128, 14), 157 long, sprite 12 by 15 from allitems.png). */
    private static final Identifier SPRITES = Identifier.fromNamespaceAndPath(Compendium.Naming.id, "textures/gui/allitems.png");
    private static final int SCROLL_X = 128;
    private static final int SCROLL_Y = 14;
    private static final int SCROLL_LENGTH = 157;
    private static final int SLIDER_WIDTH = 12;
    private static final int SLIDER_HEIGHT = 15;
    private static final int SLIDER_U = 232;
    private static final int SLIDER_V = 0;
    private static final int SPRITE_SHEET = 256;

    /** Slide length: 8 seconds at 20 ticks per second. */
    private static final int SLIDE_TICKS = 160;

    /** One visible list entry: its index in the menu's discovered list, its key and its stack. */
    private record Entry(int index, String key, ItemStack stack) {
    }

    private final List<Entry> visible = new ArrayList<>();
    private EditBox searchBar;
    private float scrollValue;
    private String activeKey;
    private DecomposerRecipes.Recipe decomposerRecipe;
    private SynthesisRecipes.Recipe synthesisRecipe;
    private List<ItemStack> synthesisGrid = emptyGrid();
    private List<ItemStack> decomposerGrid = emptyGrid();
    private int slideTimer;
    /** Whether the slider is being dragged. */
    private boolean draggingSlider;
    private int currentSlide;

    public ChemistJournalScreen(ChemistJournalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        activeKey = menu.active();
        showRecipes(activeKey);
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - GUI_WIDTH) / 2;
        topPos = (height - GUI_HEIGHT) / 2;
        searchBar = new EditBox(font, leftPos + 20, topPos + 14, 100, 12, Component.empty());
        searchBar.setResponder(text -> populateList());
        addRenderableWidget(searchBar);
        // Below the book, centred: the page's top corners hold the item name and the title.
        addRenderableWidget(Button.builder(Component.translatable("gui.minechem.table"),
                button -> minecraft.setScreen(new TableOfElementsScreen(this)))
            .bounds(leftPos + (GUI_WIDTH - TABLE_BUTTON_WIDTH) / 2, topPos + GUI_HEIGHT + 6, TABLE_BUTTON_WIDTH, 18)
            .build());
        populateList();
    }

    /** Rebuilds the visible list from the discovered names, filtered by the search text. */
    private void populateList() {
        visible.clear();
        String text = searchBar == null ? "" : searchBar.getValue();
        List<String> discovered = menu.discovered();
        for (int i = 0; i < discovered.size(); i++) {
            String key = discovered.get(i);
            ItemStack stack = JournalEntries.stackFor(key);
            if (stack.isEmpty()) {
                continue;
            }
            if (!text.isEmpty() && !stack.getHoverName().getString().toLowerCase().contains(text)) {
                continue;
            }
            visible.add(new Entry(i, key, stack));
        }
        if (visible.size() == 1 && !visible.get(0).key().equals(activeKey)) {
            select(visible.get(0));
        }
    }

    /** The entry becomes active, here and on the server. */
    private void select(Entry entry) {
        menu.setActiveLocal(entry.key());
        showRecipes(entry.key());
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, entry.index());
        }
    }

    /** The discovered names, as the journal lists them (the table of elements marks an element found when its name is here). */
    public List<String> discoveredKeys() {
        return menu.discovered();
    }

    /**
     * Opens the page of a discovered chemical, here and on the server, as a click on its list entry does. Returns false when the
     * journal has not discovered it, so no page is opened.
     */
    public boolean openChemical(String key) {
        int index = menu.discovered().indexOf(key);
        ItemStack stack = JournalEntries.stackFor(key);
        if (index < 0 || stack.isEmpty()) {
            return false;
        }
        select(new Entry(index, key, stack));
        return true;
    }

    /** Loads the synthesis and decomposer recipes of the key, if they exist. */
    private void showRecipes(String key) {
        activeKey = key;
        slideTimer = 0;
        currentSlide = 0;
        synthesisRecipe = key.isEmpty() ? null : SynthesisRecipes.RECIPES.get(key);
        decomposerRecipe = key.isEmpty() ? null : DecomposerBlockEntity.RECIPES.get(key);
        synthesisGrid = synthesisRecipe == null ? emptyGrid() : synthesisGrid(synthesisRecipe);
        decomposerGrid = decomposerRecipe == null || isSelect(decomposerRecipe)
            ? emptyGrid()
            : outputGrid(decomposerRecipe);
    }

    private static boolean isSelect(DecomposerRecipes.Recipe recipe) {
        return !recipe.alternatives().isEmpty();
    }

    private static List<ItemStack> emptyGrid() {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            grid.add(ItemStack.EMPTY);
        }
        return grid;
    }

    /** The ingredients of a synthesis recipe, in grid order (shaped in place, shapeless in order). */
    private static List<ItemStack> synthesisGrid(SynthesisRecipes.Recipe recipe) {
        List<ItemStack> grid = emptyGrid();
        List<SynthesisRecipes.Ingredient> ingredients = recipe.ingredients();
        for (int i = 0; i < Math.min(9, ingredients.size()); i++) {
            SynthesisRecipes.Ingredient ingredient = ingredients.get(i);
            if (ingredient != null) {
                grid.set(i, ChemicalItem.stackFor(ingredient.chemical()).copyWithCount(ingredient.amount()));
            }
        }
        return grid;
    }

    /** The outputs of a decomposer recipe, one per cell. */
    private static List<ItemStack> outputGrid(DecomposerRecipes.Recipe recipe) {
        List<ItemStack> grid = emptyGrid();
        List<DecomposerRecipes.Output> outputs = recipe.outputs();
        for (int i = 0; i < Math.min(9, outputs.size()); i++) {
            DecomposerRecipes.Output output = outputs.get(i);
            grid.set(i, ChemicalItem.stackFor(output.chemical()).copyWithCount(output.amount()));
        }
        return grid;
    }

    /** The alternatives of a select recipe take turns, every 8 seconds. */
    private void updateSlideshow() {
        if (decomposerRecipe == null || !isSelect(decomposerRecipe)) {
            return;
        }
        List<DecomposerRecipes.Recipe> alternatives = decomposerRecipe.alternatives();
        if (slideTimer >= SLIDE_TICKS) {
            slideTimer = 0;
            currentSlide++;
            if (currentSlide >= alternatives.size()) {
                currentSlide = 0;
            }
        }
        if (slideTimer == 0) {
            decomposerGrid = outputGrid(alternatives.get(currentSlide));
        }
        slideTimer++;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        if (activeKey.isEmpty()) {
            return;
        }
        updateSlideshow();
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI, leftPos + DECOMPOSER_X, topPos + DECOMPOSER_Y,
            51, 192, GRID_FRAME, GRID_FRAME, TEXTURE_SIZE, TEXTURE_SIZE);
        int synthesisFrame = synthesisRecipe != null && synthesisRecipe.shaped() ? 104 : 51;
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI, leftPos + SYNTHESIS_X, topPos + SYNTHESIS_Y,
            synthesisFrame, 192, GRID_FRAME, GRID_FRAME, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI, leftPos + 8, topPos + 164, 161, 192, 20, 20, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (activeKey.isEmpty()) {
            drawHelp(graphics);
            drawList(graphics, mouseX, mouseY);
            return;
        }
        drawActiveName(graphics);
        drawLabel(graphics, "gui.journal.decomposer", 175, 20, 0xFF884400);
        drawChance(graphics);
        drawLabel(graphics, "gui.journal.synthesis", 175, 100, 0xFF884400);
        if (synthesisRecipe != null) {
            graphics.text(font, SynthesisRecipes.energyCost(synthesisRecipe) + " Energy", 175, 110, 0xFF555555, false);
        }
        drawGrid(graphics, decomposerGrid, DECOMPOSER_X, DECOMPOSER_Y);
        drawGrid(graphics, synthesisGrid, SYNTHESIS_X, SYNTHESIS_Y);
        drawList(graphics, mouseX, mouseY);
    }

    private void drawSlider(GuiGraphicsExtractor graphics) {
        int travel = SCROLL_LENGTH - SLIDER_HEIGHT;
        int y = SCROLL_Y + (int) (scrollValue * travel);
        graphics.blit(RenderPipelines.GUI_TEXTURED, SPRITES, SCROLL_X, y,
            SLIDER_U, SLIDER_V, SLIDER_WIDTH, SLIDER_HEIGHT, SPRITE_SHEET, SPRITE_SHEET);
    }

    private void drawLabel(GuiGraphicsExtractor graphics, String key, int x, int y, int color) {
        graphics.text(font, Component.translatable(key), x, y, color, false);
    }

    private void drawActiveName(GuiGraphicsExtractor graphics) {
        String name = JournalEntries.stackFor(activeKey).getHoverName().getString();
        if (name.length() > 18) {
            name = name.substring(0, 18).trim() + "...";
        }
        graphics.text(font, Component.literal(name).withStyle(ChatFormatting.BOLD), 175, 10, 0xFF0000FF, false);
    }

    private void drawChance(GuiGraphicsExtractor graphics) {
        if (decomposerRecipe == null) {
            return;
        }
        float chance = isSelect(decomposerRecipe) ? 100.0F : decomposerRecipe.chance() * 100.0F;
        graphics.text(font, String.format("%.1f%% chance", chance), 175, 30, 0xFF555555, false);
    }

    /** Ghost items of a recipe grid, one per cell; the cells are 18 pixels, the item sits one pixel in. */
    private void drawGrid(GuiGraphicsExtractor graphics, List<ItemStack> grid, int frameX, int frameY) {
        for (int i = 0; i < grid.size(); i++) {
            ItemStack stack = grid.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            // extractLabels runs inside the base class's translation by (leftPos, topPos): the x and y are page-relative.
            int x = frameX + (i % 3) * CELL + 1;
            int y = frameY + (i / 3) * CELL + 1;
            graphics.item(stack, x, y);
        }
    }

    private void drawHelp(GuiGraphicsExtractor graphics) {
        graphics.text(font, Component.translatable("item.minechem.chemist_journal"), 180, 18, 0xFF000000, false);
        // The help text is drawn at half size and wraps at 200 scaled units (100 pixels on screen).
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5F, 0.5F);
        int y = 70;
        for (var line : font.split(Component.translatable("help.journal"), 200)) {
            graphics.text(font, line, 345, y, 0xAA000000, false);
            y += font.lineHeight;
        }
        graphics.pose().popMatrix();
    }

    private int scrollOffset() {
        int rows = (visible.size() + COLUMNS - 1) / COLUMNS;
        int listHeight = rows * CELL;
        return -(int) ((listHeight - VISIBLE_HEIGHT) * scrollValue);
    }

    /** The list's icons, clipped to its box. */
    private void drawList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int offset = scrollOffset();
        graphics.enableScissor(LIST_LEFT, LIST_TOP, LIST_LEFT + LIST_WIDTH, LIST_TOP + LIST_HEIGHT);
        for (int i = 0; i < visible.size(); i++) {
            int x = ORIGIN_X + (i % COLUMNS) * CELL;
            int y = ORIGIN_Y + (i / COLUMNS) * CELL + offset;
            ItemStack stack = visible.get(i).stack();
            graphics.item(stack, x, y);
            boolean hovered = mouseX >= leftPos + x && mouseX < leftPos + x + 16 && mouseY >= topPos + y && mouseY < topPos + y + 16
                && mouseX >= leftPos + LIST_LEFT && mouseX < leftPos + LIST_LEFT + LIST_WIDTH
                && mouseY >= topPos + LIST_TOP && mouseY < topPos + LIST_TOP + LIST_HEIGHT;
            if (hovered) {
                graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
        }
        graphics.disableScissor();
        drawSlider(graphics);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // The list and the slider first: the base class reports a click as handled before the list gets it.
        double x = event.x() - leftPos;
        double y = event.y() - topPos;
        int sliderY = SCROLL_Y + (int) (scrollValue * (SCROLL_LENGTH - SLIDER_HEIGHT));
        if (x >= SCROLL_X && x < SCROLL_X + SLIDER_WIDTH && y >= sliderY && y < sliderY + SLIDER_HEIGHT) {
            draggingSlider = true;
            return true;
        }
        if (x < LIST_LEFT || x >= LIST_LEFT + LIST_WIDTH || y < LIST_TOP || y >= LIST_TOP + LIST_HEIGHT) {
            return super.mouseClicked(event, doubleClick);
        }
        int offset = scrollOffset();
        for (int i = 0; i < visible.size(); i++) {
            int cellX = ORIGIN_X + (i % COLUMNS) * CELL;
            int cellY = ORIGIN_Y + (i / COLUMNS) * CELL + offset;
            if (x >= cellX && x < cellX + 16 && y >= cellY && y < cellY + 16) {
                select(visible.get(i));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Dragging the slider sets the scroll value from the pointer's height. */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!draggingSlider) {
            return super.mouseDragged(event, dragX, dragY);
        }
        float travel = SCROLL_LENGTH - SLIDER_HEIGHT;
        float value = (float) (event.y() - topPos - SCROLL_Y - SLIDER_HEIGHT / 2.0) / travel;
        scrollValue = Math.max(0.0F, Math.min(1.0F, value));
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingSlider = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollValue = Math.max(0.0F, Math.min(1.0F, scrollValue - (float) scrollY * 0.1F));
        return true;
    }
}
