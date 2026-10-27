package minechem.apparatus.synthesis;

import java.util.List;
import minechem.Compendium;
import minechem.apparatus.prefab.gui.container.BasicGuiContainer;
import minechem.apparatus.prefab.gui.tab.SideTab;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

/**
 * The synthesis machine's screen: the 176 x 205 background, the state line at the top, and two side tabs: the state control
 * tab (stored and needed energy) and the help tab (with "help.synthesis").
 *
 * <p>The tabs have no icons (see {@link SideTab}). The journal's recipe slideshow is not drawn.
 */
public class SynthesisScreen extends BasicGuiContainer<SynthesisMenu> {
    private static final Identifier GUI = Identifier.fromNamespaceAndPath(Compendium.Naming.id, "textures/gui/synthesis.png");
    private static final int POWERED = 0xFF00CC00;
    private static final int UNPOWERED = 0xFFAA0000;
    private static final int HELP = 0xFF4B7BEC;
    private static final int HELP_WIDTH = 120;

    public SynthesisScreen(SynthesisMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI);
        addSideTab(new SideTab(Component.literal("State"), this::stateColour, this::stateLines));
        addSideTab(new SideTab(Component.translatable("tab.tooltip.help"), () -> HELP,
            () -> font.split(Component.translatable("help.synthesis"), HELP_WIDTH)));
    }

    /** The background is 205 tall; the base class keeps 166, so the texture is drawn at 205. */
    @Override
    protected int backgroundHeight() {
        return 205;
    }

    @Override
    protected void init() {
        super.init();
        // The title is centred at y 5 (gui.title.synthesis).
        titleLabelX = (imageWidth - font.width(getTitle())) / 2;
        titleLabelY = 5;
    }

    /**
     * The foreground draws the title only. There is no "Inventory" label, and the state is shown on the state tab, not on
     * the GUI.
     */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, getTitle(), titleLabelX, titleLabelY, 0xFF000000, false);
    }

    private int stateColour() {
        return menu.machine().hasEnoughPowerForCurrentRecipe() ? POWERED : UNPOWERED;
    }

    private List<FormattedCharSequence> stateLines() {
        var machine = menu.machine();
        return List.of(
            line(machine.getState()),
            line(Component.translatable("tab.title.stored").getString() + ": " + machine.energyStored()),
            line(Component.translatable("tab.title.activationEnergy").getString() + ": " + machine.energyNeeded()));
    }

    private static FormattedCharSequence line(String text) {
        return FormattedCharSequence.forward(text, Style.EMPTY);
    }
}
