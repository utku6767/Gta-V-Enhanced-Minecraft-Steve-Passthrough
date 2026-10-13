package dev.rehan.passthrough.mixin;

import dev.rehan.passthrough.MobWar;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderDragon.class)
abstract class EnderDragonMixin {
	@Redirect(
		method = "aiStep",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;getFlyTargetLocation()Lnet/minecraft/world/phys/Vec3;")
	)
	private Vec3 passthrough$customFlyTarget(final DragonPhaseInstance phase) {
		EnderDragon self = (EnderDragon) (Object) this;
		Vec3 custom = MobWar.getDragonFlightTarget(self);
		if (custom != null) {
			return custom;
		}
		return phase.getFlyTargetLocation();
	}
}
