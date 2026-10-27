package minechem.radiation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The decay state of a radioactive stack: the world time the decay started, the last time it was updated, and the dimension it
 * was last in.
 */
public record RadiationState(long decayStarted, long lastDecayUpdate, String dimension) {
    public static final Codec<RadiationState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("decay_started").forGetter(RadiationState::decayStarted),
        Codec.LONG.fieldOf("last_update").forGetter(RadiationState::lastDecayUpdate),
        Codec.STRING.fieldOf("dimension").forGetter(RadiationState::dimension)
    ).apply(instance, RadiationState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RadiationState> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG, RadiationState::decayStarted,
        ByteBufCodecs.VAR_LONG, RadiationState::lastDecayUpdate,
        ByteBufCodecs.STRING_UTF8, RadiationState::dimension,
        RadiationState::new);
}
