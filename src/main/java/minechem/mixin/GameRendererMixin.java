package minechem.mixin;

import minechem.render.DeliriumOverlay;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The screen spin of the nausea effect, also for the Delirium effect at level II (that level only has the dark overlay, see
 * DeliriumOverlay). Vanilla spins the projection by its nausea and portal intensities; here the Delirium intensity joins the
 * maximum, and the spin speed is the nausea's when nothing else sets one.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /** The nausea's spin speed (vanilla tick: 7 degrees per tick at full nausea). */
    private static final float DELIRIUM_SPIN_SPEED = 7.0F;

    @Shadow
    private float spinningEffectTime;

    @Shadow
    private float spinningEffectSpeed;

    /** After vanilla's tick: when nothing else spins the view, the Delirium spin advances the angle. */
    @Inject(method = "tick", at = @At("RETURN"))
    private void minechem$deliriumSpinTick(CallbackInfo info) {
        if (this.spinningEffectSpeed == 0.0F && DeliriumOverlay.spinIntensity(1.0F) > 0.0F) {
            this.spinningEffectSpeed = DELIRIUM_SPIN_SPEED;
            this.spinningEffectTime = this.spinningEffectTime + this.spinningEffectSpeed;
        }
    }

    /** Vanilla's spin intensity in renderLevel is max(portal, nausea); the Delirium intensity joins the maximum. */
    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float minechem$spinIntensity(float portalIntensity, float nauseaIntensity) {
        return Math.max(Math.max(portalIntensity, nauseaIntensity), DeliriumOverlay.spinIntensity(1.0F));
    }
}
