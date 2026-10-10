package dev.rehan.passthrough.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BarrierBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BarrierBlock.class)
public class BarrierBlockMixin {

    @Inject(method = "canPlaceLiquid", at = @At("HEAD"), cancellable = true)
    private void noLiquid(LivingEntity user, BlockGetter level, BlockPos pos, BlockState state, Fluid type, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
