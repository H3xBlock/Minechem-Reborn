package minechem.apparatus.multiblock;

import java.util.Map;
import minechem.Compendium;
import minechem.apparatus.prefab.gui.container.BasicGuiContainer;
import minechem.apparatus.prefab.gui.tab.SideTab;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the blueprint projector, with a help tab. The height follows the slot
 * layout (the hotbar at y 142, and 26 below it).
 */
public class BlueprintProjectorScreen extends BasicGuiContainer<BlueprintProjectorMenu> {
    private static final Identifier GUI = Identifier.fromNamespaceAndPath(Compendium.Naming.id, "textures/gui/blueprint_projector.png");

    private static final int HELP = 0xFF4B7BEC;
    private static final int HELP_WIDTH = 120;

    public BlueprintProjectorScreen(BlueprintProjectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI);
        addSideTab(new SideTab(Component.translatable("tab.tooltip.help"), () -> HELP,
            () -> font.split(Component.translatable("help.projector"), HELP_WIDTH)));
    }

    @Override
    protected int backgroundHeight() {
        return 168;
    }

    /**
     * No title and no inventory label; the blueprint's name at (64, 12) with a shadow, then one
     * line "NxBlock" per wall in the blueprint, from y 23 in steps of 10. The air and the wildcard have no line.
     */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ReactorBlueprint blueprint = menu.blueprint();
        if (blueprint == null) {
            return;
        }
        String name = menu.blueprintStack().getHoverName().getString().replace("Blueprint", "");
        graphics.text(font, Component.literal(name), 64, 12, 0xFFFFFFFF, true);
        int y = 23;
        for (Map.Entry<Integer, Integer> entry : blueprint.cellCounts().entrySet()) {
            int id = entry.getKey();
            if (id != ReactorBlueprint.WALL_1 && id != ReactorBlueprint.WALL_2) {
                continue;
            }
            String line = entry.getValue() + "x" + ReactorBlockEntity.wallBlock(id).getName().getString();
            graphics.text(font, Component.literal(line), 64, y, 0xFFDDDDDD, false);
            y += 10;
        }
    }
}
