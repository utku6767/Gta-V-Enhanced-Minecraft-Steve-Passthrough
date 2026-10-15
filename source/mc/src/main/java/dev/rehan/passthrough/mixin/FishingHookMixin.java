package dev.rehan.passthrough.mixin;

import dev.rehan.passthrough.MobWar;
import dev.rehan.passthrough.Passthrough;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bridges Minecraft fishing hooks to GTA V pedestrians.
 * Hitting a proxy sends a "hook" event; retrieving/reeling the line sends a "reel" event
 * to trigger GTA ped ragdoll and upward/toward-player physical impulse.
 */
@Mixin(FishingHook.class)
abstract class FishingHookMixin {
	@Shadow
	private Entity hookedIn;

	@Shadow
	public abstract Player getPlayerOwner();

	@Unique
	private Integer passthrough$hookedPedHandle;

	@Inject(method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V", at = @At("HEAD"))
	private void passthrough$onHitEntity(final EntityHitResult result, final CallbackInfo ci) {
		Entity target = result.getEntity();
		if (MobWar.isProxy(target)) {
			Integer handle = MobWar.getHandle(target);
			if (handle != null) {
				this.passthrough$hookedPedHandle = handle;
				Passthrough.events.accept(String.format(Locale.ROOT, "{\"t\":\"hook\",\"h\":%d}", handle));
			}
		}
	}

	@Inject(method = "retrieve(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"))
	private void passthrough$retrieve(final ItemStack stack, final CallbackInfoReturnable<Integer> cir) {
		FishingHook self = (FishingHook) (Object) this;
		if (self.level() instanceof ServerLevel) {
			Integer handle = this.passthrough$hookedPedHandle;
			if (handle == null && this.hookedIn != null && MobWar.isProxy(this.hookedIn)) {
				handle = MobWar.getHandle(this.hookedIn);
			}
			if (handle != null) {
				Player player = this.getPlayerOwner();
				double px = player != null ? player.getX() : self.getX();
				double py = player != null ? player.getY() : self.getY();
				double pz = player != null ? player.getZ() : self.getZ();

				Passthrough.events.accept(String.format(Locale.ROOT,
					"{\"t\":\"reel\",\"h\":%d,\"p\":[%.3f,%.3f,%.3f]}",
					handle, px, py, pz));
				this.passthrough$hookedPedHandle = null;
			}
		}
	}
}
