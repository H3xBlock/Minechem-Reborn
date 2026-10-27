package minechem.potion;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * One effect of a drunk molecule: food, burning, cure, damage or a potion effect.
 * Seconds are converted to ticks (20 per second).
 */
public sealed interface PharmacologyEffect permits PharmacologyEffect.Food, PharmacologyEffect.Burn, PharmacologyEffect.Cure,
    PharmacologyEffect.Damage, PharmacologyEffect.Potion {

    void apply(LivingEntity entity);

    /** Adds food level and saturation, for players only. */
    record Food(int level, float saturation) implements PharmacologyEffect {
        @Override
        public void apply(LivingEntity entity) {
            if (entity instanceof Player player) {
                player.getFoodData().eat(level, saturation);
            }
        }
    }

    /** Sets the entity on fire for the given seconds. */
    record Burn(int seconds) implements PharmacologyEffect {
        @Override
        public void apply(LivingEntity entity) {
            entity.igniteForSeconds(seconds);
        }
    }

    /** Removes every effect from the entity. */
    record Cure() implements PharmacologyEffect {
        @Override
        public void apply(LivingEntity entity) {
            entity.removeAllEffects();
        }
    }

    /** A generic damage of the given amount (in half hearts). */
    record Damage(float amount) implements PharmacologyEffect {
        @Override
        public void apply(LivingEntity entity) {
            if (entity.level() instanceof ServerLevel level) {
                entity.hurtServer(level, entity.damageSources().generic(), amount);
            }
        }
    }

    /** The effect at the given amplifier (0 is level I) for the given seconds. */
    record Potion(Holder<MobEffect> effect, int amplifier, int seconds) implements PharmacologyEffect {
        @Override
        public void apply(LivingEntity entity) {
            entity.addEffect(new MobEffectInstance(effect, seconds * 20, amplifier));
        }
    }
}
