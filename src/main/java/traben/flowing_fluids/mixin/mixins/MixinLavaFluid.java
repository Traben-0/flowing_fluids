package traben.flowing_fluids.mixin.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

@Mixin(LavaFluid.class)
public abstract class MixinLavaFluid extends FlowingFluid {

    @Shadow public abstract boolean isSame(final Fluid fluid);

    @Inject(method = "canBeReplacedWith", at = @At(value = "RETURN"), cancellable = true)
    private void flowing_fluids$removeHeightCheck(final FluidState fluidState, final BlockGetter blockGetter, final BlockPos blockPos, final Fluid fluid, final Direction direction, final CallbackInfoReturnable<Boolean> cir) {
        if (FlowingFluids.config.enableMod
                && !cir.getReturnValue()
                && FlowingFluids.config.isFluidAllowed(this)
        ) {
            cir.setReturnValue(fluid.isSame(Fluids.WATER));
        }
    }

    @Inject(method = "beforeDestroyingBlock", at = @At(value = "HEAD"), cancellable = true)
    private void ff$fizzOnlyForNonLava(final LevelAccessor level, final BlockPos pos, final BlockState state, final CallbackInfo ci) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.isFluidAllowed(this)
                && isSame(state.getFluidState().getType())) {
            ci.cancel();
        }
    }

    @Inject(method = "getSlopeFindDistance", at = @At(value = "RETURN"), cancellable = true)
    private void ff$modifySlopeDistance(final LevelReader level, final CallbackInfoReturnable<Integer> cir) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.isFluidAllowed(this)) {
            cir.setReturnValue(Mth.clamp(FFFluidUtils.dimensionEvaporatesWaterVanilla(level)
                    ? FlowingFluids.config.lavaNetherFlowDistance
                    : FlowingFluids.config.lavaFlowDistance,
                    1,8));
        }
    }

    @Inject(method = "getTickDelay", at = @At(value = "RETURN"), cancellable = true)
    private void ff$modifyTickDelay(final LevelReader level, final CallbackInfoReturnable<Integer> cir) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.isFluidAllowed(this)) {
            cir.setReturnValue(Mth.clamp(FFFluidUtils.dimensionEvaporatesWaterVanilla(level)
                    ? FlowingFluids.config.lavaNetherTickDelay
                    : FlowingFluids.config.lavaTickDelay,
                    1,255));
        }
    }

    @Inject(method = "randomTick", at = @At(value = "HEAD"))
    private void ff$callSuper(
            //#if MC > 12100
            ServerLevel level,
            //#else
            //$$ Level level,
            //#endif
            final BlockPos pos, final FluidState state, final RandomSource random, final CallbackInfo ci) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.isFluidAllowed(this)) {
            super.randomTick(level, pos, state, random);
        }
    }

    @Inject(method = "spreadTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/LavaFluid;fizz(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)V"))
    private void ff$consumeLevelStoneCreation(final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState blockState, final Direction direction, final FluidState fluidState, final CallbackInfo ci) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this)) {
            var above = blockPos.above();
            FFFluidUtils.setFluidStateAtPosToNewAmount(levelAccessor, above, Fluids.LAVA,
                    levelAccessor.getFluidState(above).getAmount() - 1);
        }
    }
}
