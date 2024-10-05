package traben.flowing_fluids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

import static net.minecraft.world.level.block.TallSeagrassBlock.HALF;

@Mixin(TallSeagrassBlock.class)
public class MixinTallSeaGrassBlock {

    @Inject(method = "canSurvive", at = @At(value = "RETURN"), cancellable = true)
    private void flowing_fluids$removeHeightCheck(final BlockState state, final LevelReader level, final BlockPos pos, final CallbackInfoReturnable<Boolean> cir) {
        if (FlowingFluids.config.enableMod
                && level instanceof LevelAccessor accessor
                && cir.getReturnValue()
                && state.getValue(HALF) == DoubleBlockHalf.UPPER) {

            //break the plant if its water can flow out of it
            if(FFFluidUtils.canFluidFlowToNeighbourFromPos(accessor, pos, Fluids.WATER, 8)) {
                cir.setReturnValue(false);
            }
        }
    }
}
