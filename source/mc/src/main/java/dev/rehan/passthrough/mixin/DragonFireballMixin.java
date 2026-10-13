package dev.rehan.passthrough.mixin;

import dev.rehan.passthrough.WorldBridge;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dragon fireball impacts report an explosion so the host can trigger a railgun blast and tear gas cloud. */
@Mixin(DragonFireball.class)
abstract class DragonFireballMixin {
	@Inject(method = "onHit", at = @At("HEAD"))
	private void passthrough$onHit(final HitResult hitResult, final CallbackInfo ci) {
		DragonFireball fireball = (DragonFireball) (Object) this;
		if (!fireball.level().isClientSide()) {
			WorldBridge.onExplosion(fireball.position(), 4.0f, "dragon_fireball");
		}
	}
}
