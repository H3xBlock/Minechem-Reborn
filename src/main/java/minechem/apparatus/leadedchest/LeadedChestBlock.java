package minechem.apparatus.leadedchest;

import com.mojang.serialization.MapCodec;
import minechem.apparatus.prefab.block.BasicBlockContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * Block of the leaded chest (registry name "leadChest", wood, hardness 2, resistance 5). Right-click opens the
 * chest unless the player is sneaking.
 *
 * The chest is currently drawn with a placeholder cube.
 */
public class LeadedChestBlock extends BasicBlockContainer {
    public LeadedChestBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties createProperties() {
        // Not opaque: the block model is empty, so neighbours must keep their faces.
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0F, 5.0F).noOcclusion();
    }

    @Override
    protected MapCodec<? extends BasicBlockContainer> codec() {
        return simpleCodec(LeadedChestBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LeadedChestBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory, BlockEntity blockEntity) {
        return new LeadedChestMenu(id, inventory, (LeadedChestBlockEntity) blockEntity);
    }
}
