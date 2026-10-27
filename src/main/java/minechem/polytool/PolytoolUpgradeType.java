package minechem.polytool;

import java.util.Random;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/**
 * One element's effect on a polytool. The hooks do nothing by default, and the upgrade gives no strength against any block.
 * The power is set on the registered instance before use (setPower returns this).
 */
public abstract class PolytoolUpgradeType {
    protected static final Random RAND = new Random(System.currentTimeMillis());
    public float power = 0;

    /** The atomic number of the element this upgrade comes from. */
    public abstract int atomicNumber();

    /** The strength this upgrade adds when mining the block. */
    public float getStrVsBlock(ItemStack stack, BlockState state) {
        return 0F;
    }

    /** Called when the tool hits a living entity. */
    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    }

    /** Called when the tool breaks a block. */
    public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
    }

    /** Called every tick the tool is in an entity's inventory. */
    public void onTickFull(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        this.onTick();
    }

    public void onTick() {
    }

    public float getDamageModifier() {
        return 0F;
    }

    public abstract String getDescription();

    public PolytoolUpgradeType setPower(float power) {
        this.power = power;
        return this;
    }
}
