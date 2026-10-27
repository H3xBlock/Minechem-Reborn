package minechem.registry;

import com.mojang.serialization.MapCodec;
import java.util.List;
import minechem.Compendium;
import minechem.worldgen.UraniumOreBiomeModifier;
import minechem.worldgen.UraniumOreFeature;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The uranium ore generation. */
public final class WorldgenRegistry {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Compendium.Naming.id);
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Compendium.Naming.id);

    public static final DeferredHolder<Feature<?>, UraniumOreFeature> URANIUM_ORE = FEATURES.register("uranium_ore", UraniumOreFeature::new);
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<UraniumOreBiomeModifier>> URANIUM_ORE_BIOME_MODIFIER =
        BIOME_MODIFIERS.register("uranium_ore", () -> UraniumOreBiomeModifier.CODEC);

    private static Holder<PlacedFeature> placedUraniumOre;

    private WorldgenRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
        BIOME_MODIFIERS.register(modEventBus);
    }

    /**
     * The placed uranium ore feature: no placement modifier, so it runs once per chunk at the chunk's corner. One instance, built
     * after the features are registered, so every biome holds the same feature.
     */
    public static synchronized Holder<PlacedFeature> placedUraniumOre() {
        if (placedUraniumOre == null) {
            Holder<ConfiguredFeature<?, ?>> configured = Holder.direct(new ConfiguredFeature<>(URANIUM_ORE.get(), NoneFeatureConfiguration.INSTANCE));
            placedUraniumOre = Holder.direct(new PlacedFeature(configured, List.of()));
        }
        return placedUraniumOre;
    }
}
