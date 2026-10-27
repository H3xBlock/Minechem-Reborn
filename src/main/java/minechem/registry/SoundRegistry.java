package minechem.registry;

import minechem.Compendium;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The sounds of the mod. The sound files are listed in assets/minechem/sounds.json.
 */
public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Compendium.Naming.id);

    /** The blueprint projector's hum, played every 20 ticks while it projects. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PROJECTOR = SOUNDS.register("projector",
        id -> SoundEvent.createVariableRangeEvent(id));
}
