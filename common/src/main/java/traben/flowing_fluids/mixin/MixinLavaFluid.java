package traben.flowing_fluids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FlowingFluids;

@Mixin(LavaFluid.class)
public abstract class MixinLavaFluid {

    @Shadow public abstract boolean isSame(final Fluid fluid);

    @Inject(method = "canBeReplacedWith", at = @At(value = "RETURN"), cancellable = true)
    private void flowing_fluids$removeHeightCheck(final FluidState fluidState, final BlockGetter blockGetter, final BlockPos blockPos, final Fluid fluid, final Direction direction, final CallbackInfoReturnable<Boolean> cir) {
        if (FlowingFluids.config.enableMod && !cir.getReturnValue()) {
            cir.setReturnValue(fluid.isSame(Fluids.WATER));
        }
    }

    @Inject(method = "beforeDestroyingBlock", at = @At(value = "HEAD"), cancellable = true)
    private void ff$fizzOnlyForNonLava(final LevelAccessor level, final BlockPos pos, final BlockState state, final CallbackInfo ci) {
        if (FlowingFluids.config.enableMod && isSame(state.getFluidState().getType())) {
            ci.cancel();
        }
    }

    @Inject(method = "getSlopeFindDistance", at = @At(value = "RETURN"), cancellable = true)
    private void ff$modifySlopeDistance(final LevelReader level, final CallbackInfoReturnable<Integer> cir) {
        if (FlowingFluids.config.enableMod) {
            cir.setReturnValue(Mth.clamp(level.dimensionType().ultraWarm()
                    ? FlowingFluids.config.lavaNetherFlowDistance
                    : FlowingFluids.config.lavaFlowDistance,
                    1,8));
        }
    }

    @Inject(method = "getTickDelay", at = @At(value = "RETURN"), cancellable = true)
    private void ff$modifyTickDelay(final LevelReader level, final CallbackInfoReturnable<Integer> cir) {
        if (FlowingFluids.config.enableMod) {
            cir.setReturnValue(Mth.clamp(level.dimensionType().ultraWarm()
                    ? FlowingFluids.config.lavaNetherTickDelay
                    : FlowingFluids.config.lavaTickDelay,
                    1,255));
        }
    }
}
