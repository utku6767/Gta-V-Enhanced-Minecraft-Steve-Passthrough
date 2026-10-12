package dev.rehan.passthrough.client.mixin;

import dev.rehan.passthrough.client.ClientInput;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws a high-visibility virtual cursor on top of all Minecraft screens. */
@Mixin(Screen.class)
abstract class ScreenMixin {
	@Shadow public int width;
	@Shadow public int height;

	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
	private void passthrough$renderVirtualCursor(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick, final CallbackInfo ci) {
		if (!ClientInput.virtualCursorVisible) {
			return;
		}

		graphics.nextStratum();
		int cx = (int) Math.round(ClientInput.virtualCursorX * this.width);
		int cy = (int) Math.round(ClientInput.virtualCursorY * this.height);
		int crossColor = ClientInput.virtualCursorFlash > 0 ? 0xFF55FF55 : 0xFFFFFFFF;
		if (ClientInput.virtualCursorFlash > 0) {
			ClientInput.virtualCursorFlash--;
		}

		// Black shadow / border
		graphics.fill(cx - 5, cy - 2, cx + 6, cy + 3, 0xDD000000);
		graphics.fill(cx - 2, cy - 5, cx + 3, cy + 6, 0xDD000000);

		// Crosshair lines
		graphics.fill(cx - 4, cy - 1, cx + 5, cy + 2, crossColor);
		graphics.fill(cx - 1, cy - 4, cx + 2, cy + 5, crossColor);

		// Center red dot
		graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFF3333);
	}
}
