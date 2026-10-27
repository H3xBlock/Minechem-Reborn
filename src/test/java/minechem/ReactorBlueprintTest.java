package minechem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import minechem.apparatus.multiblock.ProjectionFrame;
import minechem.apparatus.multiblock.ReactorBlueprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/** Checks of the reactor structures and the projector's frame against the fusion and fission blueprints. */
class ReactorBlueprintTest {
    @Test
    void theFusionChamberIsThirteenByFiveByThirteen() {
        assertEquals(13, ReactorBlueprint.FUSION.xSize());
        assertEquals(5, ReactorBlueprint.FUSION.ySize());
        assertEquals(13, ReactorBlueprint.FUSION.zSize());
    }

    @Test
    void theFissionReactorIsFiveCubed() {
        assertEquals(5, ReactorBlueprint.FISSION.xSize());
        assertEquals(5, ReactorBlueprint.FISSION.ySize());
        assertEquals(5, ReactorBlueprint.FISSION.zSize());
    }

    @Test
    void theFusionManagerIsAtSixOneSixAndItsCellIsAir() {
        assertEquals(6, ReactorBlueprint.FUSION.managerX());
        assertEquals(1, ReactorBlueprint.FUSION.managerY());
        assertEquals(6, ReactorBlueprint.FUSION.managerZ());
        assertEquals(ReactorBlueprint.AIR, ReactorBlueprint.FUSION.idAt(6, 1, 6));
    }

    @Test
    void theFissionManagerIsAtTwoTwoTwo() {
        assertEquals(2, ReactorBlueprint.FISSION.managerX());
        assertEquals(2, ReactorBlueprint.FISSION.managerY());
        assertEquals(2, ReactorBlueprint.FISSION.managerZ());
        assertEquals(ReactorBlueprint.WILDCARD, ReactorBlueprint.FISSION.idAt(2, 2, 2));
    }

    @Test
    void theFissionShellHasNinetyEightWallsAndAHollowCentre() {
        // A 5 x 5 x 5 shell: 125 cells less the 3 x 3 x 3 inside it.
        int walls = 0;
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                for (int z = 0; z < 5; z++) {
                    if (ReactorBlueprint.FISSION.idAt(x, y, z) == ReactorBlueprint.WALL_2) {
                        walls++;
                    }
                }
            }
        }
        assertEquals(98, walls);
        assertEquals(ReactorBlueprint.WILDCARD, ReactorBlueprint.FISSION.idAt(2, 1, 2));
    }

    @Test
    void theFusionChamberHasFourHundredTwentyTwoWalls() {
        // Counted from the table: 181 cells of the first wall kind and 241 of the second.
        int first = 0;
        int second = 0;
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 13; x++) {
                for (int z = 0; z < 13; z++) {
                    int id = ReactorBlueprint.FUSION.idAt(x, y, z);
                    if (id == ReactorBlueprint.WALL_1) {
                        first++;
                    } else if (id == ReactorBlueprint.WALL_2) {
                        second++;
                    }
                }
            }
        }
        assertEquals(181, first);
        assertEquals(241, second);
    }

    @Test
    void southProjectsTheFissionReactorFourBlocksAhead() {
        // The projector at (0, 64, 0) facing south: the base is (2, 64, 6); the manager's cell (2, 2, 2) is at (0, 66, 4).
        ProjectionFrame frame = ProjectionFrame.of(new BlockPos(0, 64, 0), Direction.SOUTH, ReactorBlueprint.FISSION);
        assertEquals(new BlockPos(0, 66, 4), frame.at(2, 2, 2));
        assertEquals(new BlockPos(2, 64, 6), frame.at(0, 0, 0));
    }

    @Test
    void northEastAndWestProjectTheManagerTheSameWay() {
        assertEquals(new BlockPos(0, 66, -4), ProjectionFrame.of(new BlockPos(0, 64, 0), Direction.NORTH, ReactorBlueprint.FISSION).at(2, 2, 2));
        assertEquals(new BlockPos(4, 66, 0), ProjectionFrame.of(new BlockPos(0, 64, 0), Direction.EAST, ReactorBlueprint.FISSION).at(2, 2, 2));
        assertEquals(new BlockPos(-4, 66, 0), ProjectionFrame.of(new BlockPos(0, 64, 0), Direction.WEST, ReactorBlueprint.FISSION).at(2, 2, 2));
    }

    @Test
    void theFusionFrameKeepsEveryCellOutOfTheProjector() {
        // The chamber is in front of the projector, so no cell lands on the projector's own block.
        for (Direction facing : new Direction[]{Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST}) {
            ProjectionFrame frame = ProjectionFrame.of(new BlockPos(0, 64, 0), facing, ReactorBlueprint.FUSION);
            for (int x = 0; x < 13; x++) {
                for (int z = 0; z < 13; z++) {
                    assertNotEquals(new BlockPos(0, 64, 0), frame.at(x, 0, z), facing + " hits the projector");
                }
            }
        }
    }
}
