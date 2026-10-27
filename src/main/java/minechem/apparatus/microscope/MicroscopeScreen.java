package minechem.apparatus.microscope;

import java.util.List;
import minechem.Compendium;
import minechem.apparatus.prefab.gui.container.BasicGuiContainer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Screen of the microscope: the 176 x 217 background, the synthesis and
 * decomposer switch at (153, 26), and the recipe overlay (see {@link MicroscopeOverlay}).
 *
 * <p>The switch is a two-sprite toggle without a sliding knob animation. The switch
 * state is only on the client, so the server does not know which view is shown.
 */
public class MicroscopeScreen extends BasicGuiContainer<MicroscopeMenu> {
    private static final Identifier GUI = Identifier.fromNamespaceAndPath(Compendium.Naming.id, "textures/gui/microscope.png");
    private static final int SWITCH_X = 153;
    private static final int SWITCH_Y = 26;
    private static final int SWITCH_SIZE = 13;
    private static final int SWITCH_V = 124;
    private static final int SYNTHESIS_U = 176;
    private static final int DECOMPOSER_U = 189;
    private static final int OUTPUT_X = 98;
    private static final int OUTPUT_Y = 27;
    private static final int OUTPUT_STEP = 18;
    private static final int COST_X = 100;
    private static final int CHANCE_X = 108;
    private static final int TEXT_Y = 85;
    private static final int TEXT_COLOUR = 0xFF000000;

    private boolean decomposerView;
    private long ticks;

    public MicroscopeScreen(MicroscopeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI);
    }

    /** The GUI is 217 tall; the base class keeps 166, so the texture is drawn at 217. */
    @Override
    protected int backgroundHeight() {
        return 217;
    }

    /** The title is centred at y 5, and no "Inventory" label is drawn. */
    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(getTitle())) / 2;
        titleLabelY = 5;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ticks++;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int u = decomposerView ? DECOMPOSER_U : SYNTHESIS_U;
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI, leftPos + SWITCH_X, topPos + SWITCH_Y, u, SWITCH_V, SWITCH_SIZE, SWITCH_SIZE, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, getTitle(), titleLabelX, titleLabelY, TEXT_COLOUR, false);
        ItemStack input = menu.microscope().getItem(MicroscopeRules.INPUT_SLOT);
        MicroscopeOverlay.Overlay overlay = decomposerView
            ? MicroscopeOverlay.decomposer(input, ticks)
            : MicroscopeOverlay.synthesis(input);
        List<ItemStack> slots = overlay.slots();
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = slots.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            int x = OUTPUT_X + (i % 3) * OUTPUT_STEP;
            int y = OUTPUT_Y + (i / 3) * OUTPUT_STEP;
            graphics.item(stack, x, y);
            if (isHovering(x, y, mouseX, mouseY)) {
                graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
        }
        if (overlay.text() != null && !isOverSwitch(mouseX, mouseY)) {
            int x = decomposerView ? CHANCE_X : COST_X;
            graphics.text(font, overlay.text(), x, TEXT_Y, TEXT_COLOUR, false);
        }
        if (isOverSwitch(mouseX, mouseY)) {
            String target = decomposerView ? "gui.title.synthesis" : "gui.title.decomposer";
            graphics.setTooltipForNextFrame(font, Component.translatable(target), mouseX, mouseY);
        }
    }

    /** A click on the switch changes the view, and never reaches the menu's slots. */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isOverSwitch(event.x(), event.y())) {
            decomposerView = !decomposerView;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Whether a mouse position is over the switch (absolute screen coordinates). */
    private boolean isOverSwitch(double mouseX, double mouseY) {
        return isHovering(SWITCH_X, SWITCH_Y, mouseX, mouseY, SWITCH_SIZE, SWITCH_SIZE, leftPos, topPos);
    }

    /** Whether a position relative to the GUI's top left is under the mouse (absolute screen coordinates). */
    private boolean isHovering(int x, int y, double mouseX, double mouseY) {
        return isHovering(x, y, mouseX, mouseY, 16, 16, leftPos, topPos);
    }

    private static boolean isHovering(int x, int y, double mouseX, double mouseY, int width, int height, int left, int top) {
        return mouseX >= left + x && mouseX < left + x + width && mouseY >= top + y && mouseY < top + y + height;
    }
}
