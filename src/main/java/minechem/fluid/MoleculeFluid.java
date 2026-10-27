package minechem.fluid;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * A molecule's liquid. It flows like water: the source keeps 8 units, and the
 * flowing fluid carries its level. The colour comes from the molecule (see MoleculeFluids).
 */
public abstract class MoleculeFluid extends BaseFlowingFluid {
    /** A solid chemical's liquid does not flow, so it stays where it was placed. */
    protected final boolean solid;

    protected MoleculeFluid(BaseFlowingFluid.Properties properties, boolean solid) {
        super(properties);
        this.solid = solid;
    }

    @Override
    public void tick(ServerLevel level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (solid) {
            return;
        }
        if (getFluidType().isLighterThanAir() && riseOneCell(level, pos, fluidState)) {
            return;
        }
        int gasBefore = gasBeside(level, pos);
        super.tick(level, pos, state, fluidState);
        // A gas source is used up when it spreads: once it has put gas on its sides, its own cell is emptied.
        if (getFluidType().isLighterThanAir() && fluidState.isSource() && level.getFluidState(pos).isSource()
            && gasBeside(level, pos) > gasBefore) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** How many of the four sides of a cell hold this gas. */
    private int gasBeside(ServerLevel level, BlockPos pos) {
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            FluidState near = level.getFluidState(pos.relative(direction));
            if (!near.isEmpty() && near.getType().isSame(this)) {
                count++;
            }
        }
        return count;
    }

    /**
     * A gas does not flow down: it rises, and spreads only sideways when the ceiling stops it (lava in reverse). The directions
     * are the engine's, without the downward one.
     */
    @Override
    protected Map<Direction, FluidState> getSpread(ServerLevel level, BlockPos pos, BlockState state) {
        Map<Direction, FluidState> spread = new HashMap<>(super.getSpread(level, pos, state));
        if (getFluidType().isLighterThanAir()) {
            spread.remove(Direction.DOWN);
        }
        return spread;
    }

    /** The engine's downward spread of a gas is dropped: it goes to the sides and up only. Sideways spread still uses this method. */
    @Override
    protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, FluidState fluidState) {
        if (direction == Direction.DOWN && getFluidType().isLighterThanAir()) {
            return;
        }
        super.spreadTo(level, pos, state, direction, fluidState);
    }

    /**
     * The level of a flowing gas comes from the gas beside it only. The engine also takes the level of the fluid above (a liquid that
     * falls), which for a gas made each cell full (8) from the gas above, and the cloud fed itself without end. Liquids keep the engine's rule.
     */
    @Override
    protected FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState state) {
        if (!getFluidType().isLighterThanAir()) {
            return super.getNewLiquid(level, pos, state);
        }
        int highest = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos near = pos.relative(direction);
            BlockState nearState = level.getBlockState(near);
            FluidState nearFluid = nearState.getFluidState();
            if (!nearFluid.getType().isSame(this) || nearState.blocksMotion()) {
                continue;
            }
            highest = Math.max(highest, nearFluid.isSource() ? 8 : nearFluid.getAmount());
        }
        int amount = highest - getDropOff(level);
        return amount > 0 ? getFlowing(amount, false) : Fluids.EMPTY.defaultFluidState();
    }

    /**
     * A gas never turns into a source, as lava does not: water does this where two sources touch, and a gas that copied it
     * would multiply.
     */
    @Override
    protected boolean canConvertToSource(ServerLevel level) {
        return !getFluidType().isLighterThanAir() && super.canConvertToSource(level);
    }

    /**
     * A gas rises (it has density -10). The engine does not lift a fluid by its density, so this code moves the gas into the
     * air above it, keeping its amount, and it spreads as a liquid when the ceiling is closed.
     * Returns false when the cell above is not air.
     */
    private static boolean riseOneCell(ServerLevel level, BlockPos pos, FluidState fluidState) {
        BlockPos above = pos.above();
        // At the top of the world the gas stops, as at a ceiling: it would otherwise leave the world and be lost.
        if (!level.isInsideBuildHeight(above.getY()) || !level.getBlockState(above).isAir()) {
            return false;
        }
        level.setBlock(above, fluidState.createLegacyBlock(), Block.UPDATE_ALL);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        return true;
    }

    public static class Source extends MoleculeFluid {
        public Source(BaseFlowingFluid.Properties properties, boolean solid) {
            super(properties, solid);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends MoleculeFluid {
        public Flowing(BaseFlowingFluid.Properties properties, boolean solid) {
            super(properties, solid);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
