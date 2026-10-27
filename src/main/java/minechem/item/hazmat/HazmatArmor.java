package minechem.item.hazmat;

import java.util.Map;
import minechem.Compendium;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * The radiation suit: four pieces (helmet, chestplate, leggings, boots) of rubber with a lead layer, as a real anti-radiation suit.
 *
 * <p>Rubber is represented by the slime ball (there is no crafting ingredient for the rubber molecule), and the suit is repaired
 * with slime balls. The lead layer is the element Pb. The shield factors are listed in {@link HazmatPieceItem}, and the armour
 * values are vanilla-like.
 */
public final class HazmatArmor {
    /** The equipment asset: the worn textures are minechem:hazmat (entity/equipment/humanoid and humanoid_leggings). */
    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID,
        Identifier.fromNamespaceAndPath(Compendium.Naming.id, "hazmat"));
    /** The items that repair the suit. */
    public static final TagKey<Item> REPAIR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Compendium.Naming.id, "hazmat_repair"));
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(300,
        Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 4, ArmorType.LEGGINGS, 3, ArmorType.BOOTS, 1),
        10, SoundEvents.ARMOR_EQUIP_LEATHER, 0F, 0F, REPAIR, ASSET);

    /** The share of radiation damage each piece removes; the full suit removes 0.80. */
    public static final float HELMET_SHIELD = 0.15F;
    public static final float CHESTPLATE_SHIELD = 0.30F;
    public static final float LEGGINGS_SHIELD = 0.25F;
    public static final float BOOTS_SHIELD = 0.10F;

    private HazmatArmor() {
    }
}
