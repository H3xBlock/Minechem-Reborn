package minechem.apparatus.leadedchest;

import minechem.Compendium;
import minechem.apparatus.prefab.gui.container.BasicGuiContainer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the leaded chest. The background is the 176 x 217 leaded chest texture.
 */
public class LeadedChestScreen extends BasicGuiContainer<LeadedChestMenu> {
    private static final Identifier GUI = Identifier.fromNamespaceAndPath(Compendium.Naming.id, "textures/gui/leaded_chest.png");

    public LeadedChestScreen(LeadedChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI);
    }

    @Override
    protected void init() {
        super.init();
        // The title is centred at y 5.
        titleLabelX = (imageWidth - font.width(getTitle())) / 2;
        titleLabelY = 5;
    }

    /**
     * The foreground draws the title only, so there is no "Inventory" label.
     */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, getTitle(), titleLabelX, titleLabelY, 0xFF000000, false);
    }

    /** The GUI is 217 tall; the base class's image height stays at 166, so the texture is drawn at 217. */
    @Override
    protected int backgroundHeight() {
        return 217;
    }
}
