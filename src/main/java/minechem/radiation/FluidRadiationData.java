package minechem.radiation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import minechem.Compendium;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The radioactivity of placed radioactive fluid. NeoForge fluid blocks have no block entity, so it is kept per level, by block
 * position, and saved with the level.
 *
 * <p>An entry is written when a radioactive bucket empties and taken when the fluid is picked up again. It is also dropped when
 * the fluid block is removed or replaced by another block (MoleculeFluidBlock.affectNeighborsAfterRemoval).
 */
public class FluidRadiationData extends SavedData {
    /**
     * No data fixer: NeoForge's SavedDataType takes no fixer for mod data (SavedDataStorage.readTagFromDisk reads the tag as it
     * is), so saved files load unchanged.
     */
    public static final SavedDataType<FluidRadiationData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Compendium.Naming.id, "fluid_radiation"),
        FluidRadiationData::new,
        RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf().fieldOf("entries").forGetter(FluidRadiationData::entries)).apply(instance, FluidRadiationData::new)));

    /** One placed fluid block and its radiation state. */
    public record Entry(BlockPos pos, RadiationState state) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
            RadiationState.CODEC.fieldOf("state").forGetter(Entry::state)).apply(instance, Entry::new));
    }

    private final Map<BlockPos, RadiationState> states = new HashMap<>();

    public FluidRadiationData() {
    }

    private FluidRadiationData(List<Entry> entries) {
        for (Entry entry : entries) {
            states.put(entry.pos(), entry.state());
        }
    }

    private List<Entry> entries() {
        List<Entry> result = new ArrayList<>();
        for (Map.Entry<BlockPos, RadiationState> entry : states.entrySet()) {
            result.add(new Entry(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /** The data of a level, created when first used. */
    public static FluidRadiationData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void put(BlockPos pos, RadiationState state) {
        states.put(pos.immutable(), state);
        setDirty();
    }

    /** The state of the fluid at a position, removed from the data; null when it is not radioactive. */
    public RadiationState take(BlockPos pos) {
        RadiationState state = states.remove(pos);
        if (state != null) {
            setDirty();
        }
        return state;
    }
}
