package minechem.registry;

import minechem.Compendium;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's own mob effects. "Delirium" has the colour 0x00FF6E.
 */
public class MobEffectRegistry {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Compendium.Naming.id);

    public static final DeferredHolder<MobEffect, MobEffect> DELIRIUM = MOB_EFFECTS.register("delirium",
        () -> new MobEffect(MobEffectCategory.NEUTRAL, 0x00FF6E) {
        });
}
