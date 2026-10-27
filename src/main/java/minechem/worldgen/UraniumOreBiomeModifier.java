package minechem.worldgen;

import com.mojang.serialization.MapCodec;
import minechem.registry.WorldgenRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Adds the uranium ore feature to the given biomes (the data file biome_modifier/uranium_ore.json gives the overworld biomes).
 * The placed feature is built in code, with no placement modifier: {@link UraniumOreFeature} does the per-chunk loop itself,
 * with the values of the config.
 */
public record UraniumOreBiomeModifier(HolderSet<Biome> biomes) implements BiomeModifier {
    public static final MapCodec<UraniumOreBiomeModifier> CODEC = Biome.LIST_CODEC.fieldOf("biomes")
        .xmap(UraniumOreBiomeModifier::new, UraniumOreBiomeModifier::biomes);

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && this.biomes.contains(biome)) {
            builder.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, WorldgenRegistry.placedUraniumOre());
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return WorldgenRegistry.URANIUM_ORE_BIOME_MODIFIER.get();
    }
}
