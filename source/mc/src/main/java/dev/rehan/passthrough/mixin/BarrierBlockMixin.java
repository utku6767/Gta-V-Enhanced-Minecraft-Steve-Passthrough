package dev.rehan.passthrough.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BarrierBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BarrierBlock.class)
public class BarrierBlockMixin {

    @Inject(method = "canPlaceLiquid", at = @At("HEAD"), cancellable = true)
    private void noLiquid(BlockGetter level, BlockPos pos, BlockState state,
                          Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "placeLiquid", at = @At("HEAD"), cancellable = true)
    private void noPlace(LevelAccessor level, BlockPos pos, BlockState state,
                         FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
