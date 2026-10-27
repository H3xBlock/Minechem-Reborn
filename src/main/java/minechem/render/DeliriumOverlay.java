package minechem.render;

import minechem.Compendium;
import minechem.registry.MobEffectRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * While the player has the Delirium effect, the whole screen is covered by a dark translucent colour. The strength comes from the
 * effect's level (amplifier): {@code 5 * level + 5} as the "markiplier", and the colour is
 * {@code (int) (220 * markiplier - 150) << 24 | 0x101020}.
 *
 * <p>It is drawn on the GUI's post event.
 */
@EventBusSubscriber(modid = Compendium.Naming.id, value = Dist.CLIENT)
public final class DeliriumOverlay {
    /** Colour bits of the overlay: 0x101020 (1052704 in decimal). */
    static final int BASE_COLOUR = 1052704;

    private DeliriumOverlay() {
    }

    /**
     * Strongest alpha of the overlay: 0xB6, which level I already gives (950 is capped to 0xB6). The cap stops the alpha from
     * wrapping around in the shift by 24: a higher level would otherwise be almost transparent, and clamping to 255 would cover
     * the screen.
     */
    static final int MAX_ALPHA = 0xB6;

    /** The ARGB colour of the overlay for a level of the effect. */
    static int colourFor(int level) {
        int markiplier = 5 * level + 5;
        int alpha = Math.min(MAX_ALPHA, Math.max(0, (int) (220.0F * markiplier - 150)));
        return alpha << 24 | BASE_COLOUR;
    }

    /** From this effect level (amplifier 1, level II) the view spins as with nausea, see GameRendererMixin. */
    static final int SPIN_AMPLIFIER = 1;

    /** The spin intensity of the Delirium effect (0 to 1, the nausea's blend factor), 0 below level II or without a player. */
    public static float spinIntensity(float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0.0F;
        }
        MobEffectInstance effect = player.getEffect(MobEffectRegistry.DELIRIUM);
        if (effect == null || effect.getAmplifier() < SPIN_AMPLIFIER) {
            return 0.0F;
        }
        return player.getEffectBlendFactor(MobEffectRegistry.DELIRIUM, partialTick);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        MobEffectInstance effect = player.getEffect(MobEffectRegistry.DELIRIUM);
        if (effect == null) {
            return;
        }
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        event.getGuiGraphics().fill(0, 0, width, height, colourFor(effect.getAmplifier()));
    }
}
