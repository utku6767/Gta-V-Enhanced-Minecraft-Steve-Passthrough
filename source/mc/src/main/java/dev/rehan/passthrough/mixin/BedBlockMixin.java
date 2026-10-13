package dev.rehan.passthrough.mixin;

import dev.rehan.passthrough.Nether;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Beds explode in Nether mode instead of allowing sleep, mirroring vanilla Nether behaviour. */
@Mixin(AbstractBedBlock.class)
abstract class BedBlockMixin {
	@Inject(method = "getBedRule", at = @At("HEAD"), cancellable = true)
	private void passthrough$netherBedExplosion(final Level level, final BlockPos pos, final CallbackInfoReturnable<BedRule> cir) {
		if (Nether.active()) {
			cir.setReturnValue(BedRule.DESTROY_ON_USE);
		}
	}
}
