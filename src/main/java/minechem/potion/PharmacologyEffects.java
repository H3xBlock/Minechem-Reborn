package minechem.potion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import minechem.registry.MobEffectRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * What a molecule does when it is drunk. The table is keyed by the molecule's full name, and each molecule keeps its effects in
 * the order they are listed in the table. The effects in the recreational block follow the config recreationalChemicalEffects
 * (read once, when the table is built).
 *
 * <p>The potion names are mapped to the 26.1 mob effects (moveSpeed to SPEED, confusion to NAUSEA, and so on).
 */
public final class PharmacologyEffects {
    private static final Map<String, List<PharmacologyEffect>> EFFECTS = new HashMap<>();
    private static boolean loaded;

    private PharmacologyEffects() {
    }

    /** Applies every effect of a molecule to the entity that drank it. */
    public static void apply(String molecule, LivingEntity entity) {
        load();
        for (PharmacologyEffect effect : EFFECTS.getOrDefault(molecule, List.of())) {
            effect.apply(entity);
        }
    }

    /** Whether a molecule has any effect; the others are drunk with none. */
    public static boolean hasEffect(String molecule) {
        load();
        return EFFECTS.containsKey(molecule);
    }

    /** The table is built on first use, after the vanilla and mod mob effects are registered. */
    private static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        add("Water", food(1, .1F));
        add("Starch", food(6, .2F));
        add("Xylitol", food(6, .2F));
        add("Sucrose", potion(MobEffects.SPEED, 1, 20));
        add("Sucrose", food(1, .1F));
        add("T-2 Mycotoxin", potion(MobEffects.WITHER, 7, 8));
        add("T-2 Mycotoxin", potion(MobEffects.WEAKNESS, 1, 12));
        add("Ethyl Alcohol", potion(MobEffects.NAUSEA, 5, 30));
        add("Ethyl Alcohol", food(1, .1F));
        add("Potassium Cyanide", potion(MobEffects.WITHER, 3, 40));
        add("Penicillin", cure());
        add("Penicillin", potion(MobEffects.REGENERATION, 3, 40));
        add("Testosterone", potion(MobEffects.STRENGTH, 0, 20));
        add("Testosterone", potion(MobEffects.SPEED, 1, 20));
        add("Alprazolam", potion(MobEffects.NAUSEA, 3, 30));
        add("Alprazolam", potion(MobEffects.SPEED, 1, 20));
        add("Pantherine", potion(MobEffects.NAUSEA, 3, 30));
        add("Pantherine", potion(MobEffects.SLOWNESS, 2, 40));
        add("Mescaline", damage(2F));
        add("Mescaline", potion(MobEffects.NAUSEA, 4, 60));
        add("Mescaline", potion(MobEffects.BLINDNESS, 4, 30));
        add("Aspirin", cure());
        add("Aspirin", potion(MobEffects.REGENERATION, 1, 2));
        add("Phosgene", burn(2));
        add("Allyl Alcohol", burn(2));
        add("Sulfuric Acid", burn(2));
        add("Tert-Butyllithium", burn(2));
        add("Tetrodotoxin", potion(MobEffects.SLOWNESS, 8, 5));
        add("Tetrodotoxin", potion(MobEffects.WEAKNESS, 1, 2));
        add("Fingolimod", potion(MobEffects.STRENGTH, 1, 60));
        add("Fingolimod", potion(MobEffects.SPEED, 1, 60));
        add("Fingolimod", potion(MobEffects.REGENERATION, 1, 60));
        add("Fingolimod", potion(MobEffects.HUNGER, 1, 300));
        add("Nodularin", potion(MobEffects.POISON, 3, 30));
        add("Nodularin", potion(MobEffects.HUNGER, 3, 60));
        add("Diphenhydramine", cure());
        add("Diphenhydramine", potion(MobEffects.NAUSEA, 5, 20));
        add("Batrachotoxin", potion(MobEffects.SLOWNESS, 7, 5));
        add("Batrachotoxin", potion(MobEffects.WITHER, 0, 5));
        add("Theobromine", potion(MobEffects.HASTE, 10, 30));
        add("Theobromine", potion(MobEffects.SPEED, 10, 30));
        add("Retinol", potion(MobEffects.NIGHT_VISION, 5, 30));
        add("Glycine", food(2, .1F));
        add("Alanine", food(2, .1F));
        add("Arginine", food(2, .1F));
        add("Proline", food(2, .1F));
        add("Leucine", food(2, .1F));
        add("Isoleucine", food(2, .1F));
        add("Cysteine", food(2, .1F));
        add("Valine", food(2, .1F));
        add("Threonine", food(2, .1F));
        add("Histidine", food(2, .1F));
        add("Methionine", food(2, .1F));
        add("Tyrosine", food(2, .1F));
        add("Asparagine", food(2, .1F));
        add("Aspartic Acid", food(2, .1F));
        add("Phenylalanine", food(2, .1F));
        add("Serine", food(2, .1F));
        add("Glycine", potion(MobEffects.HASTE, 1, 20));
        add("Alanine", potion(MobEffects.HASTE, 1, 20));
        add("Arginine", potion(MobEffects.HASTE, 1, 20));
        add("Proline", potion(MobEffects.HASTE, 1, 20));
        add("Leucine", potion(MobEffects.HASTE, 1, 20));
        add("Isoleucine", potion(MobEffects.HASTE, 1, 20));
        add("Cysteine", potion(MobEffects.HASTE, 1, 20));
        add("Valine", potion(MobEffects.HASTE, 1, 20));
        add("Threonine", potion(MobEffects.HASTE, 1, 20));
        add("Histidine", potion(MobEffects.HASTE, 1, 20));
        add("Methionine", potion(MobEffects.HASTE, 1, 20));
        add("Tyrosine", potion(MobEffects.HASTE, 1, 20));
        add("Asparagine", potion(MobEffects.HASTE, 1, 20));
        add("Aspartic Acid", potion(MobEffects.HASTE, 1, 20));
        add("Phenylalanine", potion(MobEffects.HASTE, 1, 20));
        add("Serine", potion(MobEffects.HASTE, 1, 20));
        add("Glycine", potion(MobEffects.JUMP_BOOST, 1, 20));
        add("Alanine", potion(MobEffects.JUMP_BOOST, 1, 20));
        add("Arginine", potion(MobEffects.JUMP_BOOST, 1, 20));
        add("Proline", potion(MobEffects.JUMP_BOOST, 1, 20));
        add("Leucine", potion(MobEffects.JUMP_BOOST, 1, 20));
        add("Isoleucine", potion(MobEffects.JUMP_BOOST, 1, 20));
        add("Cysteine", potion(MobEffects.JUMP_BOOST, 1, 20));
        add("Valine", potion(MobEffects.JUMP_BOOST, 1, 20));
        add("Threonine", potion(MobEffects.JUMP_BOOST, 1, 20));
        add("Histidine", potion(MobEffects.JUMP_BOOST, 1, 20));
        add("Methionine", potion(MobEffects.JUMP_BOOST, 1, 20));
        add("Tyrosine", potion(MobEffects.JUMP_BOOST, 1, 20));
        add("Asparagine", potion(MobEffects.JUMP_BOOST, 1, 20));
        add("Aspartic Acid", potion(MobEffects.JUMP_BOOST, 1, 20));
        add("Phenylalanine", potion(MobEffects.JUMP_BOOST, 1, 20));
        add("Serine", potion(MobEffects.JUMP_BOOST, 1, 20));
        // The recreational block follows the config recreationalChemicalEffects (on by default).
        if (minechem.Config.recreationalChemicalEffects()) {
            add("Amphetamine", damage(4F));
            add("Amphetamine", potion(MobEffects.NAUSEA, 5, 20));
            add("Amphetamine", potion(MobEffects.SPEED, 15, 30));
            add("Cocaine", damage(4F));
            add("Cocaine", potion(MobEffects.NAUSEA, 5, 120));
            add("Cocaine", potion(MobEffects.SPEED, 12, 120));
            add("Cocaine", potion(MobEffects.NIGHT_VISION, 5, 120));
            add("Cocaine Hydrochloride", damage(4F));
            add("Cocaine Hydrochloride", potion(MobEffects.NAUSEA, 5, 60));
            add("Cocaine Hydrochloride", potion(MobEffects.SPEED, 10, 60));
            add("Cocaine Hydrochloride", potion(MobEffects.NIGHT_VISION, 5, 60));
            add("Dimethyltryptamine", damage(2F));
            add("Dimethyltryptamine", potion(MobEffects.NAUSEA, 5, 30));
            add("Dimethyltryptamine", potion(MobEffects.NIGHT_VISION, 5, 60));
            add("Methamphetamine", damage(4F));
            add("Methamphetamine", potion(MobEffects.NAUSEA, 5, 40));
            add("Methamphetamine", potion(MobEffects.SPEED, 7, 60));
            add("Psilocybin", damage(2F));
            add("Psilocybin", potion(MobEffects.NAUSEA, 5, 10));
            add("Psilocybin", potion(MobEffects.NIGHT_VISION, 5, 20));
            add("THC", potion(MobEffects.NAUSEA, 5, 60));
            add("THC", potion(MobEffects.HUNGER, 20, 120));
            add("THC", potion(MobEffects.SLOWNESS, 3, 60));
            add("THC", potion(MobEffects.REGENERATION, 1, 60));
        }
        add("Methylene Blue", cure());
        add("Methylene Blue", potion(MobEffects.REGENERATION, 4, 30));
        add("Methylene Blue", potion(MobEffects.WEAKNESS, 4, 30));
        add("Methyl Alcohol", potion(MobEffects.BLINDNESS, 6, 20));
        add("Methyl Alcohol", potion(MobEffects.WITHER, 2, 10));
        add("Radium Chloride", potion(MobEffects.WEAKNESS, 6, 120));
        add("Radium Chloride", potion(MobEffects.POISON, 6, 20));
        add("Radium Chloride", potion(MobEffects.WITHER, 1, 30));
        add("Caulerpenyne", potion(MobEffects.WEAKNESS, 6, 2));
        add("Caulerpenyne", potion(MobEffects.NAUSEA, 4, 2));
        add("Hyoscyamine", damage(4F));
        add("Hyoscyamine", potion(MobEffects.NAUSEA, 2, 12));
        add("Hyoscyamine", potion(MobEffectRegistry.DELIRIUM, 1, 18));
        add("Hyoscyamine", potion(MobEffects.SLOWNESS, 2, 45));
    }

    private static void add(String molecule, PharmacologyEffect effect) {
        EFFECTS.computeIfAbsent(molecule, key -> new ArrayList<>()).add(effect);
    }

    private static PharmacologyEffect food(int level, float saturation) {
        return new PharmacologyEffect.Food(level, saturation);
    }

    private static PharmacologyEffect burn(int seconds) {
        return new PharmacologyEffect.Burn(seconds);
    }

    private static PharmacologyEffect cure() {
        return new PharmacologyEffect.Cure();
    }

    private static PharmacologyEffect damage(float amount) {
        return new PharmacologyEffect.Damage(amount);
    }

    private static PharmacologyEffect potion(Holder<MobEffect> effect, int amplifier, int seconds) {
        return new PharmacologyEffect.Potion(effect, amplifier, seconds);
    }
}
