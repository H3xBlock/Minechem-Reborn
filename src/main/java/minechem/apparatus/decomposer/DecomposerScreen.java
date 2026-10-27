package minechem.apparatus.decomposer;

import java.util.List;
import minechem.Compendium;
import minechem.apparatus.prefab.gui.container.BasicGuiContainer;
import minechem.apparatus.prefab.gui.tab.SideTab;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the decomposer: the 176 x 186 background, the "x" button that dumps the fluid tank (server side,
 * DecomposerMenu.clickMenuButton), the fluid gauge, and the state tab.
 *
 * <p>The liquid is a flat colour, not the fluid's own sprite.
 */
public class DecomposerScreen extends BasicGuiContainer<DecomposerMenu> {
    private static final Identifier GUI = Identifier.fromNamespaceAndPath(Compendium.Naming.id, "textures/gui/decomposer.png");
    private static final Identifier TANK = Identifier.fromNamespaceAndPath(Compendium.Naming.id, "textures/gui/fluidtank.png");

    private static final int POWERED = 0xFF00CC00;
    private static final int UNPOWERED = 0xFFAA0000;
    private static final int HELP = 0xFF4B7BEC;
    private static final int HELP_WIDTH = 120;

    /** The gauge frame is 18 by 39 pixels at (18, 16) of the GUI. */
    private static final int TANK_X = 18;
    private static final int TANK_Y = 16;
    private static final int FRAME_WIDTH = 18;
    private static final int FRAME_HEIGHT = 39;
    /** The glass overlay (the texture's (19, 1) part, 16 by 37) at the frame's inner corner (posX + 1, posY + 1). */
    private static final int GLASS_U = 19;
    private static final int GLASS_V = 1;
    private static final int GLASS_WIDTH = 16;
    private static final int GLASS_HEIGHT = 37;
    /** The liquid is 15 pixels wide at (posX + 2, posY + 1) and is masked from the top by 37 - (38 * amount / capacity) rows. */
    private static final int LIQUID_X = 2;
    private static final int LIQUID_WIDTH = 15;
    private static final int LIQUID_ROWS = 38;
    private static final int LIQUID_TOP = 1;
    private static final int LIQUID_HEIGHT = 37;

    public DecomposerScreen(DecomposerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI);
        addSideTab(new SideTab(Component.literal("State"), this::stateColour, this::stateLines));
        addSideTab(new SideTab(Component.translatable("tab.tooltip.help"), () -> HELP,
            () -> font.split(Component.translatable("help.decomposer"), HELP_WIDTH)));
    }

    private int stateColour() {
        return "Active".equals(menu.getDecomposer().getStateString()) ? POWERED : UNPOWERED;
    }

    private List<FormattedCharSequence> stateLines() {
        var decomposer = menu.getDecomposer();
        return List.of(
            line(decomposer.getStateString()),
            line(Component.translatable("tab.title.stored").getString() + ": " + decomposer.energyStored()),
            line(Component.translatable("tab.title.activationEnergy").getString() + ": " + DecomposerBlockEntity.energyNeeded()));
    }

    private static FormattedCharSequence line(String text) {
        return FormattedCharSequence.forward(text, Style.EMPTY);
    }

    /** The GUI is 186 tall; the base class's image height stays at 166, so the texture is drawn at 186. */
    @Override
    protected int backgroundHeight() {
        return 186;
    }

    @Override
    protected void init() {
        super.init();
        // The title is centred at y 5. No "Inventory" label is drawn.
        titleLabelX = (imageWidth - font.width(getTitle())) / 2;
        titleLabelY = 5;
        Button dump = Button.builder(Component.literal("x"), button -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DecomposerMenu.DUMP_BUTTON);
                }
            })
            .bounds(leftPos + 20, topPos + 5, 12, 12)
            .tooltip(Tooltip.create(Component.translatable("gui.title.decomposer.dump")))
            .build();
        addRenderableWidget(dump);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, getTitle(), titleLabelX, titleLabelY, 0xFF000000, false);
    }

    /**
     * The gauge is drawn with the background, in screen coordinates (the labels are drawn relative to the GUI). The frame, the
     * liquid and the glass are the gauge's three draws. A non-empty tank always shows at least one row of liquid,
     * even below 1/38 of the capacity.
     */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int left = leftPos + TANK_X;
        int top = topPos + TANK_Y;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TANK, left, top, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, 256, 256);

        var tank = menu.getDecomposer().getFluidTank();
        FluidResource resource = tank.getResource(0);
        int amount = tank.getAmountAsInt(0);
        int capacity = tank.getCapacityAsInt(0, resource);
        if (!resource.isEmpty() && amount > 0 && capacity > 0) {
            int rows = Math.max(1, LIQUID_ROWS * amount / capacity);
            int bottom = top + LIQUID_TOP + LIQUID_HEIGHT;
            graphics.fill(left + LIQUID_X, bottom - rows, left + LIQUID_X + LIQUID_WIDTH, bottom, fluidColour(resource));
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, TANK, left + 1, top + 1, GLASS_U, GLASS_V, GLASS_WIDTH, GLASS_HEIGHT, 256, 256);

        boolean hovered = mouseX >= left && mouseX < left + 16 && mouseY >= top && mouseY < top + 37;
        if (hovered) {
            String text = resource.isEmpty() || amount <= 0 ? "Empty" : fluidName(resource) + " " + amount + "mB";
            graphics.setTooltipForNextFrame(font, Component.literal(text), mouseX, mouseY);
        }
    }

    /** The fluid's own name, localised: the name of the fluid's block. */
    private static String fluidName(FluidResource resource) {
        return resource.getFluid().defaultFluidState().createLegacyBlock().getBlock().getName().getString();
    }

    /** A colour per fluid: a flat colour, water and lava as in-game. */
    private static int fluidColour(FluidResource resource) {
        String name = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(resource.getFluid()).getPath();
        if (name.contains("lava")) {
            return 0xFFFF6A00;
        }
        if (name.contains("water")) {
            return 0xFF3F76E4;
        }
        return 0xFF888888;
    }
}
