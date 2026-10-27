package minechem.client;

import minechem.Compendium;
import net.minecraft.resources.Identifier;

/**
 * Atlas of the device model textures. The vanilla blocks atlas reads only minecraft's own definition file, so the mod
 * registers its own atlas (see ClientSetup) and the device renderers sample sprites from it, as vanilla chests do.
 */
public final class DeviceAtlas {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Compendium.Naming.id, "textures/atlas/devices.png");
    public static final Identifier DEFINITION = Identifier.fromNamespaceAndPath(Compendium.Naming.id, "devices");

    private DeviceAtlas() {
    }
}
