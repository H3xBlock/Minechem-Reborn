package minechem.polytool;

import minechem.Compendium;
import minechem.registry.ItemRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The polytool screen: the infusion slot and the strengths of the held polytool. */
public class PolytoolScreen extends AbstractContainerScreen<PolytoolMenu> {
    private static final Identifier GUI = Identifier.fromNamespaceAndPath(Compendium.Naming.id, "textures/gui/polytool.png");
    private static final int TEXT_COLOUR = 0xFF404040;
    /** Height of the background texture; the image width (176) is the default. */
    private static final int BACKGROUND_HEIGHT = 218;

    public PolytoolScreen(PolytoolMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, Component.translatable("gui.minechem.polytool"));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI, leftPos, topPos, 0, 0, imageWidth, BACKGROUND_HEIGHT, 256, 256);
    }

    /**
     * Draws the polytool icon at (80, 42), then the five strengths at y 80, 90, 100, 110 and 120. Nothing is drawn when no
     * polytool is held in the main hand.
     */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.translatable("gui.minechem.polytool"), titleLabelX, titleLabelY, TEXT_COLOUR, false);
        ItemStack held = minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
        if (!(held.getItem() instanceof PolytoolItem)) {
            return;
        }
        graphics.item(ItemRegistry.POLYTOOL.get().getDefaultInstance(), 80, 42);
        graphics.text(font, label("sword") + ": " + PolytoolItem.getSwordStr(held), 10, 80, TEXT_COLOUR, false);
        graphics.text(font, label("ores") + ": " + PolytoolItem.getPickaxeStr(held), 10, 90, TEXT_COLOUR, false);
        graphics.text(font, label("stone") + ": " + PolytoolItem.getStoneStr(held), 10, 100, TEXT_COLOUR, false);
        graphics.text(font, label("axe") + ": " + PolytoolItem.getAxeStr(held), 10, 110, TEXT_COLOUR, false);
        graphics.text(font, label("shovel") + ": " + PolytoolItem.getShovelStr(held), 10, 120, TEXT_COLOUR, false);
    }

    /** The translated name of a strength (gui.polytool.* keys). */
    private static String label(String name) {
        return Component.translatable("gui.polytool." + name).getString();
    }
}
