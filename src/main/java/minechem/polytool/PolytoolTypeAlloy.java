package minechem.polytool;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An alloy's strength for each tool kind, in proportion to the element's power. The vanilla tool tags decide which tool is
 * effective on a block.
 *
 * <p>Only the vanilla stone and cobblestone blocks count as stone.
 */
public class PolytoolTypeAlloy extends PolytoolUpgradeType {
    private final ElementAlloy alloy;

    public PolytoolTypeAlloy(ElementAlloy alloy) {
        this.alloy = alloy;
    }

    public PolytoolTypeAlloy(ElementAlloy alloy, float power) {
        this.alloy = alloy;
        this.power = power;
    }

    public float getStrOre() {
        return alloy.pickaxe * power;
    }

    public float getStrStone() {
        return alloy.stone * power;
    }

    public float getStrAxe() {
        return alloy.axe * power;
    }

    public float getStrSword() {
        return alloy.sword * power;
    }

    public float getStrShovel() {
        return alloy.shovel * power;
    }

    @Override
    public float getStrVsBlock(ItemStack stack, BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)) {
                return getStrStone();
            }
            return getStrOre();
        } else if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return getStrShovel();
        } else if (state.is(BlockTags.SWORD_EFFICIENT)) {
            return getStrSword();
        } else if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return getStrAxe();
        }
        return 0;
    }

    @Override
    public float getDamageModifier() {
        return getStrSword();
    }

    @Override
    public int atomicNumber() {
        return alloy.atomicNumber;
    }

    @Override
    public String getDescription() {
        return "Ore: " + getStrOre() + " Stone: " + getStrStone() + " Sword: " + getStrSword() + " Axe: " + getStrAxe() + " Shovel: " + getStrShovel() + " ";
    }
}
