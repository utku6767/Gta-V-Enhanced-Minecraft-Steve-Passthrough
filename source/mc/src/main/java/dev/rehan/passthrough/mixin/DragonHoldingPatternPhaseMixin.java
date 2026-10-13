package dev.rehan.passthrough.mixin;

import dev.rehan.passthrough.MobWar;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonHoldingPatternPhase;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DragonHoldingPatternPhase.class)
abstract class DragonHoldingPatternPhaseMixin {
	@Shadow
	protected EnderDragon dragon;

	@Inject(method = "getFlyTargetLocation", at = @At("HEAD"), cancellable = true)
	private void passthrough$customFlyTarget(final CallbackInfoReturnable<Vec3> cir) {
		Vec3 customTarget = MobWar.getDragonFlightTarget(this.dragon);
		if (customTarget != null) {
			cir.setReturnValue(customTarget);
		}
	}

	@Inject(method = "doServerTick", at = @At("HEAD"), cancellable = true)
	private void passthrough$customServerTick(final ServerLevel level, final CallbackInfo ci) {
		if (MobWar.hasActiveDragonTarget(this.dragon)) {
			ci.cancel();
		}
	}
}
