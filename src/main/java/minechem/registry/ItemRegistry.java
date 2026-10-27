package minechem.registry;

import com.mojang.serialization.Codec;
import minechem.Compendium;
import minechem.apparatus.multiblock.BlueprintItem;
import minechem.apparatus.multiblock.ReactorBlueprint;
import net.minecraft.world.item.Item;
import minechem.item.chemical.ChemicalItem;
import minechem.item.chemistjournal.ChemistJournalItem;
import minechem.radiation.RadiationState;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Items are registered through DeferredRegister. The chemical name is stored in a data component.
 */
public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Compendium.Naming.id);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Compendium.Naming.id);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CHEMICAL_NAME = COMPONENTS.registerComponentType(
        "chemical_name",
        builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));



    /** The chemist journal's active item, by chemical name. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> JOURNAL_ACTIVE = COMPONENTS.registerComponentType(
        "journal_active",
        builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** The chemicals the chemist journal has discovered, by chemical name, in discovery order. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> JOURNAL_DISCOVERED = COMPONENTS.registerComponentType(
        "journal_discovered",
        builder -> builder.persistent(Codec.STRING.listOf()).networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())));

    /** The chemist journal's owner name. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> JOURNAL_OWNER = COMPONENTS.registerComponentType(
        "journal_owner",
        builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** The decay state of a radioactive stack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RadiationState>> RADIATION = COMPONENTS.registerComponentType(
        "radiation",
        builder -> builder.persistent(RadiationState.CODEC).networkSynchronized(RadiationState.STREAM_CODEC));

    /** The upgrades of a polytool. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<minechem.polytool.PolytoolUpgrades>> POLYTOOL_UPGRADES = COMPONENTS.registerComponentType(
        "polytool_upgrades",
        builder -> builder.persistent(minechem.polytool.PolytoolUpgrades.CODEC).networkSynchronized(minechem.polytool.PolytoolUpgrades.STREAM_CODEC));

    public static final DeferredItem<ChemicalItem> CHEMICAL =ITEMS.registerItem("chemical", ChemicalItem::new);
    /** The emptied vial: pouring a molecule's liquid gives eight of these back. */
    public static final DeferredItem<Item> EMPTY_TUBE = ITEMS.registerSimpleItem("empty_tube");
    public static final DeferredItem<ChemistJournalItem> CHEMIST_JOURNAL = ITEMS.registerItem(
        "chemist_journal", props -> new ChemistJournalItem(props.stacksTo(1)));
    /** The blueprints of the reactors. */
    public static final DeferredItem<BlueprintItem> BLUEPRINT_FUSION = ITEMS.registerItem(
        "blueprint_fusion", props -> new BlueprintItem(props, ReactorBlueprint.FUSION));
    /** The polytool: an iron pickaxe whose strength comes from the elements infused into it. */
    public static final DeferredItem<minechem.polytool.PolytoolItem> POLYTOOL = ITEMS.registerItem("polytool", minechem.polytool.PolytoolItem::new);

    /** The radiation suit (minechem.item.hazmat): four pieces, each a shield against radiation damage when worn. */
    public static final DeferredItem<minechem.item.hazmat.HazmatPieceItem> HAZMAT_HELMET = ITEMS.registerItem("hazmat_helmet",
        p -> new minechem.item.hazmat.HazmatPieceItem(p, minechem.item.hazmat.HazmatArmor.HELMET_SHIELD),
        p -> p.humanoidArmor(minechem.item.hazmat.HazmatArmor.MATERIAL, net.minecraft.world.item.equipment.ArmorType.HELMET));
    public static final DeferredItem<minechem.item.hazmat.HazmatPieceItem> HAZMAT_CHESTPLATE = ITEMS.registerItem("hazmat_chestplate",
        p -> new minechem.item.hazmat.HazmatPieceItem(p, minechem.item.hazmat.HazmatArmor.CHESTPLATE_SHIELD),
        p -> p.humanoidArmor(minechem.item.hazmat.HazmatArmor.MATERIAL, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE));
    public static final DeferredItem<minechem.item.hazmat.HazmatPieceItem> HAZMAT_LEGGINGS = ITEMS.registerItem("hazmat_leggings",
        p -> new minechem.item.hazmat.HazmatPieceItem(p, minechem.item.hazmat.HazmatArmor.LEGGINGS_SHIELD),
        p -> p.humanoidArmor(minechem.item.hazmat.HazmatArmor.MATERIAL, net.minecraft.world.item.equipment.ArmorType.LEGGINGS));
    public static final DeferredItem<minechem.item.hazmat.HazmatPieceItem> HAZMAT_BOOTS = ITEMS.registerItem("hazmat_boots",
        p -> new minechem.item.hazmat.HazmatPieceItem(p, minechem.item.hazmat.HazmatArmor.BOOTS_SHIELD),
        p -> p.humanoidArmor(minechem.item.hazmat.HazmatArmor.MATERIAL, net.minecraft.world.item.equipment.ArmorType.BOOTS));
    public static final DeferredItem<BlueprintItem> BLUEPRINT_FISSION = ITEMS.registerItem(
        "blueprint_fission", props -> new BlueprintItem(props, ReactorBlueprint.FISSION));
    /** The optical lenses. */
    public static final DeferredItem<Item> CONCAVE_LENS = ITEMS.registerSimpleItem("concave_lens");
    public static final DeferredItem<Item> CONVEX_LENS = ITEMS.registerSimpleItem("convex_lens");
    public static final DeferredItem<Item> MICROSCOPE_LENS = ITEMS.registerSimpleItem("microscope_lens");
    public static final DeferredItem<Item> PROJECTOR_LENS = ITEMS.registerSimpleItem("projector_lens");
    public static final DeferredItem<Item> ATOMIC_MANIPULATOR = ITEMS.registerSimpleItem("atomic_manipulator");
}
