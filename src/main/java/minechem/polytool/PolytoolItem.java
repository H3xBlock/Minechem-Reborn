package minechem.polytool;

import java.util.ArrayList;
import java.util.List;
import minechem.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.SimpleMenuProvider;

/**
 * An iron pickaxe whose strength comes from the elements it has taken. Each element adds its power to the tool's strength for
 * the axe, shovel, pickaxe (ore), stone and sword, through its upgrade type. The attribute modifier (attack damage = getSwordStr)
 * is not dynamic in 26.1: the mining speed is computed by {@link PolytoolEvents} (break speed event), and the hit damage by
 * {@link #hurtEnemy} (the sword strength). The item's own attack attribute stays the iron pickaxe's.
 *
 * <p>Right-click opens the polytool's infusion menu.
 */
public class PolytoolItem extends Item {
    /** The base strength of every tool kind, before the upgrades. */
    static final float BASE_STRENGTH = 8F;

    /**
     * An iron pickaxe that never loses durability: neither a hit nor a broken block damages it. In 26.1 the hit damage comes from
     * the WEAPON component (2 per hit for a pickaxe), so it is replaced by a weapon that costs nothing, and {@link #mineBlock} does
     * not call super (which would use the TOOL component's damage per block).
     */
    public PolytoolItem(Item.Properties properties) {
        super(properties.pickaxe(ToolMaterial.IRON, 1F, -2.8F).component(DataComponents.WEAPON, new Weapon(0)).stacksTo(1));
    }

    /** The upgrades of a stack, as types with their power. */
    public static List<PolytoolUpgradeType> getUpgrades(ItemStack stack) {
        List<PolytoolUpgradeType> result = new ArrayList<>();
        for (PolytoolUpgrades.Upgrade upgrade : upgradesOf(stack).upgrades()) {
            PolytoolUpgradeType type = PolytoolHelper.getTypeFromElement(upgrade.atomicNumber(), upgrade.power());
            if (type != null) {
                result.add(type);
            }
        }
        return result;
    }

    /** The alloy upgrades of a stack. */
    public static List<PolytoolTypeAlloy> getAlloyUpgrades(ItemStack stack) {
        List<PolytoolTypeAlloy> result = new ArrayList<>();
        for (PolytoolUpgradeType type : getUpgrades(stack)) {
            if (type instanceof PolytoolTypeAlloy alloy) {
                result.add(alloy);
            }
        }
        return result;
    }

    public static PolytoolUpgrades upgradesOf(ItemStack stack) {
        PolytoolUpgrades upgrades = stack.get(ItemRegistry.POLYTOOL_UPGRADES.get());
        return upgrades == null ? PolytoolUpgrades.EMPTY : upgrades;
    }

    /** The power of an element in a stack. */
    public static float getPowerOfType(ItemStack stack, int atomicNumber) {
        return upgradesOf(stack).powerOf(atomicNumber);
    }

    /** Adds the power of an element to a stack. */
    public static void addPower(ItemStack stack, int atomicNumber, float power) {
        stack.set(ItemRegistry.POLYTOOL_UPGRADES.get(), upgradesOf(stack).withPower(atomicNumber, power));
    }

    public static float getSwordStr(ItemStack stack) {
        return strength(stack, PolytoolTypeAlloy::getStrSword);
    }

    public static float getPickaxeStr(ItemStack stack) {
        return strength(stack, PolytoolTypeAlloy::getStrOre);
    }

    public static float getStoneStr(ItemStack stack) {
        return strength(stack, PolytoolTypeAlloy::getStrStone);
    }

    public static float getAxeStr(ItemStack stack) {
        return strength(stack, PolytoolTypeAlloy::getStrAxe);
    }

    public static float getShovelStr(ItemStack stack) {
        return strength(stack, PolytoolTypeAlloy::getStrShovel);
    }

    private static float strength(ItemStack stack, java.util.function.ToDoubleFunction<PolytoolTypeAlloy> part) {
        float result = BASE_STRENGTH;
        for (PolytoolTypeAlloy alloy : getAlloyUpgrades(stack)) {
            result += (float) part.applyAsDouble(alloy);
        }
        return result;
    }

    /**
     * Whether the element can be infused into the stack: with the alloy added, the tool must stay above zero in all five strengths.
     * An element that is not an alloy can always be infused.
     */
    public static boolean validAlloyInfusion(ItemStack polytool, int atomicNumber) {
        PolytoolUpgradeType upgrade = PolytoolHelper.getTypeFromElement(atomicNumber, 1);
        if (!(upgrade instanceof PolytoolTypeAlloy)) {
            return true;
        }
        ItemStack copy = polytool.copy();
        addPower(copy, atomicNumber, 1);
        return getSwordStr(copy) > 0 && getPickaxeStr(copy) > 0 && getStoneStr(copy) > 0 && getAxeStr(copy) > 0 && getShovelStr(copy) > 0;
    }

    /** The mining speed of the stack against a block. */
    public static float getDigSpeed(ItemStack stack, BlockState state) {
        float result = BASE_STRENGTH;
        for (PolytoolUpgradeType type : getUpgrades(stack)) {
            result += type.getStrVsBlock(stack, state);
        }
        return result;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, menuPlayer) -> new PolytoolMenu(id, inventory),
                Component.translatable("gui.minechem.polytool")));
        }
        return InteractionResult.SUCCESS;
    }

    /** A hit deals the sword strength as damage, then each upgrade's hook runs. */
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player) {
            target.hurt(target.damageSources().playerAttack(player), getSwordStr(stack));
        }
        for (PolytoolUpgradeType type : getUpgrades(stack)) {
            type.hitEntity(stack, target, attacker);
        }
    }

    /**
     * Called by the game once the block is broken. It does not call super, so the tool takes no damage, and it returns true.
     */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        for (PolytoolUpgradeType type : getUpgrades(stack)) {
            type.onBlockDestroyed(stack, level, state, pos, miner);
        }
        return true;
    }

    /** Each upgrade ticks while the stack is in an inventory. */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        boolean selected = entity instanceof Player player && player.getMainHandItem() == stack;
        for (PolytoolUpgradeType type : getUpgrades(stack)) {
            type.onTickFull(stack, level, entity, 0, selected);
        }
        super.inventoryTick(stack, level, entity, slot);
    }
}
