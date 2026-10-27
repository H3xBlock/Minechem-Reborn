package minechem.apparatus.prefab.gui.container;

import java.util.ArrayList;
import java.util.List;
import minechem.apparatus.prefab.gui.tab.SideTab;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Draws a background texture behind the menu and the left side tabs.
 */
public abstract class BasicGuiContainer<T extends BasicContainer> extends AbstractContainerScreen<T> {
    protected final Identifier backgroundTexture;
    private final List<SideTab> sideTabs = new ArrayList<>();

    public BasicGuiContainer(T menu, Inventory playerInventory, Component title, Identifier backgroundTexture) {
        super(menu, playerInventory, title);
        this.backgroundTexture = backgroundTexture;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, backgroundTexture, leftPos, topPos, 0, 0, imageWidth, backgroundHeight(), 256, 256);
        for (int i = 0; i < sideTabs.size(); i++) {
            sideTabs.get(i).extract(graphics, font, leftPos, topPos + 4 + i * 20);
        }
    }

    /** Adds a tab on the left edge. Tabs stack from the top; only one of them is open at a time. */
    protected void addSideTab(SideTab tab) {
        sideTabs.add(tab);
    }

    /** Height of the background texture drawn. Defaults to the screen's image height; a GUI taller than that overrides it. */
    protected int backgroundHeight() {
        return imageHeight;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        sideTabs.forEach(SideTab::tick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int i = 0; i < sideTabs.size(); i++) {
            SideTab tab = sideTabs.get(i);
            boolean wasOpen = tab.isOpen();
            if (tab.mouseClicked(event.x(), event.y(), leftPos, topPos + 4 + i * 20)) {
                sideTabs.forEach(other -> other.setOpen(false));
                tab.setOpen(!wasOpen);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
