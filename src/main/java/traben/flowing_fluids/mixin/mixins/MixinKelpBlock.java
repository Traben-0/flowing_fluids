package traben.flowing_fluids.mixin.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import traben.flowing_fluids.FlowingFluids;

@Mixin(KelpBlock.class)
public abstract class MixinKelpBlock extends GrowingPlantHeadBlock {

    protected MixinKelpBlock() {
        //noinspection DataFlowIssue
        super(null, null, null, false, 0);
    }

    @ModifyReturnValue(method = "canGrowInto", at = @At("RETURN"))
    boolean ff$canGrowInto(boolean original, @Local(argsOnly = true) BlockState state) {
        if (original && FlowingFluids.config.enableMod && FlowingFluids.config.isWaterAllowed()) {
            return state.getFluidState().getAmount() >= 8;
        }
        return original;
    }

}
