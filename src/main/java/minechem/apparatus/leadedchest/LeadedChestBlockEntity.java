package minechem.apparatus.leadedchest;

import java.util.List;
import minechem.apparatus.prefab.tileEntity.BaseTileEntity;
import minechem.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The nine slots, saved with their items. Stacks are kept at the chest's limit
 * (see {@link LeadedChestSlots}).
 */
public class LeadedChestBlockEntity extends BaseTileEntity implements Container, minechem.radiation.INoDecay {
    private final NonNullList<ItemStack> items = NonNullList.withSize(LeadedChestSlots.SLOTS, ItemStack.EMPTY);

    public LeadedChestBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.LEADED_CHEST.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return LeadedChestSlots.SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty()) {
            stack.setCount(LeadedChestSlots.clamp(stack.getCount()));
        }
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return LeadedChestSlots.STACK_LIMIT;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        List<ItemStack> saved = input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < Math.min(saved.size(), items.size()); i++) {
            items.set(i, saved.get(i));
        }
    }
}
