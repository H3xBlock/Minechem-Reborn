package minechem.apparatus.multiblock;

import com.mojang.serialization.Codec;
import minechem.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Block entity of the ghost: which structure id the ghost stands for ({@link ReactorBlueprint#WALL_1} or
 * {@link ReactorBlueprint#WALL_2}). The ghost only stores the wall, not the blueprint's id.
 */
public class GhostBlockEntity extends BlockEntity {
    private int wall = ReactorBlueprint.WALL_1;

    public GhostBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GHOST_BLOCK.get(), pos, state);
    }

    /** The structure id of the wall this ghost stands for. */
    public int wall() {
        return wall;
    }

    public void setWall(int wall) {
        this.wall = wall;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("wall", Codec.INT, wall);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        wall = input.read("wall", Codec.INT).orElse(ReactorBlueprint.WALL_1);
    }
}
