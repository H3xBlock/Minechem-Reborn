package minechem.fluid;

import minechem.Config;
import minechem.chemical.ChemicalBase;
import minechem.fluid.reaction.ChemicalFluidReactionHandler;
import minechem.helper.Jenkins;
import minechem.potion.PharmacologyEffects;
import minechem.radiation.FluidRadiationData;
import minechem.radiation.RadiationEnum;
import minechem.radiation.RadiationState;
import minechem.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.redstone.Orientation;

/**
 * The placed block of a molecule's liquid (or an element's). Like water, it gives way to the next block placed over it.
 * A living entity in it gets the molecule's pharmacology and, for a radioactive chemical, poison.
 */
public class MoleculeFluidBlock extends LiquidBlock {
    /** The molecule's name, or null for an element's liquid (only molecules have pharmacology). */
    private final String molecule;
    /** The element's symbol, or null for a molecule's liquid. */
    private final String symbol;
    private final RadiationEnum radioactivity;
    /** The slowness amplifier of the liquid (see {@link #slownessOf}), or -1 for none. */
    private final int slowness;
    /** The chemical of this liquid, found on first use (the chemicals are loaded after the blocks are registered). */
    private ChemicalBase chemical;

    public MoleculeFluidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties, String molecule, String symbol,
                              RadiationEnum radioactivity, int slowness) {
        super(fluid, properties);
        this.molecule = molecule;
        this.symbol = symbol;
        this.radioactivity = radioactivity;
        this.slowness = slowness;
    }

    /** The chemical of this liquid: the molecule, or the element with the symbol. */
    public ChemicalBase chemical() {
        if (chemical == null) {
            if (molecule != null) {
                chemical = Jenkins.get(molecule);
            } else {
                chemical = ChemicalBuckets.elementBySymbol(symbol);
            }
        }
        return chemical;
    }

    /**
     * When a neighbour changes, the liquid may react with it or explode. Only the server decides.
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level instanceof ServerLevel server) {
            ChemicalFluidReactionHandler.checkStatus(server, pos);
        }
    }

    /**
     * How much a living entity is slowed in a liquid, by its form: liquid amplifier 2 (-45 %), solid 3 (-60 %), gas 0
     * (-15 %). The slowness effect is 15 % a level. -1 is no slowing.
     */
    public static int slownessOf(String form) {
        return switch (form) {
            case "liquid" -> 2;
            case "solid" -> 3;
            case "gas" -> 0;
            default -> -1;
        };
    }

    /** The bucket of a picked-up radioactive fluid keeps its state. */
    @Override
    public ItemStack pickupBlock(LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state) {
        // The entry is taken before the pickup: the pickup removes the block, which drops the entry (affectNeighborsAfterRemoval).
        RadiationState radiation = level instanceof ServerLevel server ? FluidRadiationData.get(server).take(pos) : null;
        ItemStack bucket = super.pickupBlock(user, level, pos, state);
        if (radiation != null) {
            if (!bucket.isEmpty()) {
                bucket.set(ItemRegistry.RADIATION.get(), radiation);
            } else if (level.getBlockState(pos).is(this)) {
                // Nothing was picked up (a flowing block): the fluid keeps its state.
                FluidRadiationData.get((ServerLevel) level).put(pos, radiation);
            }
        }
        return bucket;
    }

    /**
     * The fluid block was removed or replaced by another block: its radioactivity goes with it. Without this, the entries of
     * fluids that flowed away or were built over stayed in the level's data for good.
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        FluidRadiationData.get(level).take(pos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean precise) {
        super.entityInside(state, level, pos, entity, applier, precise);
        if (level.isClientSide() || !(entity instanceof LivingEntity living) || !Config.fluidEffects()) {
            return;
        }
        if (molecule != null) {
            PharmacologyEffects.apply(molecule, living);
        }
        if (slowness >= 0) {
            // Short and hidden: entering the liquid (a move through it) renews it, leaving lets it run out.
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, slowness, false, false, false));
        }
        // Poison for the radioactivity's level minus one, for 10 ticks (the stable level, power 0, gives none).
        int power = radioactivity.ordinal();
        if (power > 0) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 10, power - 1));
        }
    }
}
