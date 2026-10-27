package minechem.apparatus.multiblock;

import java.util.List;
import minechem.Compendium;
import minechem.apparatus.prefab.gui.container.BasicGuiContainer;
import minechem.apparatus.prefab.gui.tab.SideTab;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the fission chamber. The title is centred at the top, and the energy bar is drawn over the
 * background (160 pixels at the stored fraction). The state tab and the help tab are side tabs.
 */
public class FissionReactorScreen extends BasicGuiContainer<FissionReactorMenu> {
    private static final Identifier GUI = Identifier.fromNamespaceAndPath(Compendium.Naming.id, "textures/gui/fission_reactor.png");

    private static final int HELP = 0xFF4B7BEC;
    private static final int HELP_WIDTH = 120;
    /** The energy bar: 160 by 3 pixels at (8, 38) of the texture's (0, 187) row. */
    private static final int BAR_X = 8;
    private static final int BAR_Y = 38;
    private static final int BAR_U = 0;
    private static final int BAR_V = 187;
    private static final int BAR_WIDTH = 160;
    private static final int BAR_HEIGHT = 3;

    public FissionReactorScreen(FissionReactorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI);
        addSideTab(new SideTab(Component.literal("State"), () -> HELP, this::stateLines));
        addSideTab(new SideTab(Component.translatable("tab.tooltip.help"), () -> HELP,
            () -> font.split(Component.translatable("help.fission"), HELP_WIDTH)));
    }

    @Override
    protected int backgroundHeight() {
        return 168;
    }

    @Override
    protected void init() {
        super.init();
        // The title is centred at the top, and no "Inventory" label is drawn.
        titleLabelX = (imageWidth - font.width(getTitle())) / 2;
        titleLabelY = 5;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, getTitle(), titleLabelX, titleLabelY, 0xFF000000, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int stored = menu.storedEnergy();
        int width = (int) (stored * (BAR_WIDTH / (double) FissionReactorBlockEntity.ENERGY_CAPACITY));
        if (width > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, GUI, leftPos + BAR_X, topPos + BAR_Y, BAR_U, BAR_V, width, BAR_HEIGHT, 256, 256);
        }
    }

    /** The stored energy with its percentage, and the energy the next split needs. */
    private List<FormattedCharSequence> stateLines() {
        FissionReactorBlockEntity fission = menu.getFission();
        int stored = menu.storedEnergy();
        int percent = (int) (stored * (100D / FissionReactorBlockEntity.ENERGY_CAPACITY));
        return List.of(
            line(Component.translatable("tab.title.stored").getString() + ": " + stored + " RF (" + percent + "%)"),
            line(Component.translatable("tab.title.activationEnergy").getString() + ": " + fission.energyNeeded() + " RF"));
    }

    private static FormattedCharSequence line(String text) {
        return FormattedCharSequence.forward(text, Style.EMPTY);
    }
}
