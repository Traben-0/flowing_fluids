package traben.flowing_fluids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FlowingFluids;

@Mixin(LavaFluid.class)
public class MixinLavaFluid {

    @Inject(method = "canBeReplacedWith", at = @At(value = "RETURN"), cancellable = true)
    private void flowing_fluids$removeHeightCheck(final FluidState fluidState, final BlockGetter blockGetter, final BlockPos blockPos, final Fluid fluid, final Direction direction, final CallbackInfoReturnable<Boolean> cir) {
        if (FlowingFluids.config.enableMod && !cir.getReturnValue()) {
            cir.setReturnValue(fluid.is(FluidTags.WATER));
        }
    }
}
