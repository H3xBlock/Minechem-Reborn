package minechem.polytool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The upgrades of one polytool: the power of each element it has taken. Stored as a data component. An element's power is added
 * to its entry, and a new element is appended.
 */
public record PolytoolUpgrades(List<Upgrade> upgrades) {
    public static final PolytoolUpgrades EMPTY = new PolytoolUpgrades(List.of());

    /** One element of the tool: its atomic number and its power. */
    public record Upgrade(int atomicNumber, float power) {
        public static final Codec<Upgrade> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("element").forGetter(Upgrade::atomicNumber),
            Codec.FLOAT.fieldOf("power").forGetter(Upgrade::power)).apply(instance, Upgrade::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Upgrade> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Upgrade::atomicNumber,
            ByteBufCodecs.FLOAT, Upgrade::power,
            Upgrade::new);
    }

    public static final Codec<PolytoolUpgrades> CODEC = Upgrade.CODEC.listOf().xmap(PolytoolUpgrades::new, PolytoolUpgrades::upgrades);
    public static final StreamCodec<RegistryFriendlyByteBuf, PolytoolUpgrades> STREAM_CODEC = Upgrade.STREAM_CODEC.apply(ByteBufCodecs.list())
        .map(PolytoolUpgrades::new, PolytoolUpgrades::upgrades);

    /** The upgrades with {@code power} added for an element. */
    public PolytoolUpgrades withPower(int atomicNumber, float power) {
        List<Upgrade> result = new ArrayList<>(upgrades.size() + 1);
        boolean found = false;
        for (Upgrade upgrade : upgrades) {
            if (!found && upgrade.atomicNumber() == atomicNumber) {
                result.add(new Upgrade(atomicNumber, upgrade.power() + power));
                found = true;
            } else {
                result.add(upgrade);
            }
        }
        if (!found) {
            result.add(new Upgrade(atomicNumber, power));
        }
        return new PolytoolUpgrades(List.copyOf(result));
    }

    /** The power of an element in this tool, 0 when it has none. */
    public float powerOf(int atomicNumber) {
        for (Upgrade upgrade : upgrades) {
            if (upgrade.atomicNumber() == atomicNumber) {
                return upgrade.power();
            }
        }
        return 0;
    }
}
