package dev.rehan.passthrough.mixin;

import dev.rehan.passthrough.MobWar;
import dev.rehan.passthrough.Passthrough;
import java.util.Locale;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bedrock Edition feature: snowballs knock back mobs without dealing damage.
 * Also bridges snowball hits on GTA V ped proxies, distinguishing top-of-head hits
 * (to trigger a vertical collapse) from directional hits (to knock down in throw direction).
 */
@Mixin(Snowball.class)
abstract class SnowballMixin {
	@Inject(method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V", at = @At("TAIL"))
	private void passthrough$onHitEntity(final EntityHitResult result, final CallbackInfo ci) {
		Entity target = result.getEntity();
		Snowball self = (Snowball) (Object) this;

		// 1. Bedrock Edition feature: knock back any LivingEntity without dealing damage (Blaze handled by vanilla)
		if (target instanceof LivingEntity living && !(target instanceof net.minecraft.world.entity.monster.Blaze)) {
			Vec3 vel = self.getDeltaMovement();
			double len = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
			if (len > 1.0E-4) {
				double dirX = vel.x / len;
				double dirZ = vel.z / len;
				living.knockback(0.45, -dirX, -dirZ, self.damageSources().thrown(self, self.getOwner()), 0.0f);
			}
		}

		// 2. GTA ped proxy bridge
		if (MobWar.isProxy(target)) {
			Integer handle = MobWar.getHandle(target);
			if (handle != null) {
				Vec3 hitPos = result.getLocation();
				double targetTopY = target.getY() + target.getBbHeight();
				// Check if hit was at the top of their head
				boolean isTop = (hitPos.y >= targetTopY - 0.25) || (hitPos.y >= target.getY() + target.getBbHeight() * 0.85);

				Entity owner = self.getOwner();
				double px = owner != null ? owner.getX() : self.getX();
				double py = owner != null ? owner.getY() : self.getY();
				double pz = owner != null ? owner.getZ() : self.getZ();

				Vec3 vel = self.getDeltaMovement();
				Passthrough.events.accept(String.format(Locale.ROOT,
					"{\"t\":\"snowballhit\",\"h\":%d,\"top\":%s,\"from\":[%.3f,%.3f,%.3f],\"dir\":[%.3f,%.3f,%.3f]}",
					handle, isTop ? "true" : "false", px, py, pz, vel.x, vel.y, vel.z));
			}
		}
	}
}
