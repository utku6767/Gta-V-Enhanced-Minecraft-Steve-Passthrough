package dev.rehan.passthrough.client.mixin;

import dev.rehan.passthrough.client.HostState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide vanilla Minecraft crosshair when GTA gun mode is active with Steve on person. */
@Mixin(Hud.class)
abstract class HudMixin {
	@Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
	private void passthrough$hideCrosshairOnGun(final GuiGraphicsExtractor extractor, final DeltaTracker deltaTracker, final CallbackInfo ci) {
		final HostState.Pose p = HostState.frame() != null ? HostState.frame() : HostState.live();
		if (p != null && p.gun()) {
			ci.cancel();
		}
	}
}
