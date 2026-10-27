package minechem.worldgen;

import minechem.Config;
import minechem.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Generates uranium ore once per populated chunk. The feature is added with no placement modifier (see
 * {@link UraniumOreBiomeModifier}), so it runs once per chunk with the chunk's lowest corner as origin, that is 16 * chunkX and
 * 16 * chunkZ. The settings generateOre, uraniumoredensity and uraniumOreClusterSize are read from the config each time a chunk
 * is decorated.
 *
 * <p>Only the overworld generates: the biome modifier adds the feature to the overworld biomes, and the dimension is checked here
 * too, so a mod dimension that reuses overworld biomes gets no ore.
 */
public class UraniumOreFeature extends Feature<NoneFeatureConfiguration> {
    public UraniumOreFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!Config.generateOre() || level.getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }
        BlockPos origin = context.origin();
        BlockState ore = BlockRegistry.URANIUM_ORE.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int placed = UraniumOreRules.generateChunk(context.random(), origin.getX(), origin.getZ(), true,
            Config.uraniumOreDensity(), Config.uraniumOreClusterSize(), new UraniumOreRules.VeinTarget() {
                @Override
                public boolean canReplace(int x, int y, int z) {
                    // Only plain stone is replaced: no granite, diorite, andesite, deepslate or tuff.
                    return level.getBlockState(pos.set(x, y, z)).is(Blocks.STONE);
                }

                @Override
                public void placeOre(int x, int y, int z) {
                    // Flag 2 sends the change to the clients.
                    level.setBlock(pos.set(x, y, z), ore, Block.UPDATE_CLIENTS);
                }
            });
        return placed > 0;
    }
}
