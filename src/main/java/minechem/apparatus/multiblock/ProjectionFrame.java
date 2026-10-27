package minechem.apparatus.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Where a blueprint's cells are in the world when the blueprint projector projects it. The projector's base is moved
 * forwards by the blueprint's depth plus one and to its left by half its width; a cell {@code (x, y, z)} is then at the
 * base plus the rotated offset.
 *
 * <p>The y is not rotated. Its x and z are swapped for north and south.
 */
public record ProjectionFrame(int baseX, int baseY, int baseZ, Direction facing) {

    /** The frame for a projector at {@code projector} facing {@code facing}, projecting {@code blueprint}. */
    public static ProjectionFrame of(BlockPos projector, Direction facing, ReactorBlueprint blueprint) {
        int forward = blueprint.zSize() + 1;
        int left = blueprint.xSize() / 2;
        int x = projector.getX();
        int z = projector.getZ();
        switch (facing) {
            case SOUTH -> {
                z += forward;
                x += left;
            }
            case NORTH -> {
                z -= forward;
                x -= left;
            }
            case EAST -> {
                x += forward;
                z -= left;
            }
            case WEST -> {
                x -= forward;
                z += left;
            }
            default -> throw new IllegalArgumentException("a projector faces a horizontal direction, not " + facing);
        }
        return new ProjectionFrame(x, projector.getY(), z, facing);
    }

    /** The world position of structure cell {@code (x, y, z)}. */
    public BlockPos at(int x, int y, int z) {
        int worldY = baseY + y;
        return switch (facing) {
            case SOUTH -> new BlockPos(baseX - z, worldY, baseZ - x);
            case NORTH -> new BlockPos(baseX + z, worldY, baseZ + x);
            case EAST -> new BlockPos(baseX - x, worldY, baseZ + z);
            case WEST -> new BlockPos(baseX + x, worldY, baseZ - z);
            default -> throw new IllegalArgumentException("a projector faces a horizontal direction, not " + facing);
        };
    }
}
