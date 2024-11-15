package traben.flowing_fluids.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfig;

@Mixin(value = LiquidBlockRenderer.class, priority = 1001)
public abstract class MixinLiquidBlockRenderer {

    @ModifyVariable(
            method = "tesselate",
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;",
                    shift = At.Shift.AFTER)
            , ordinal = 0
    )
    private Vec3 ff$alterFlowDir(final Vec3 value) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.hideFlowingTexture) {
            return Vec3.ZERO;
        }
        return value;
    }

// stopped working in 1.21.3, absolutely no idea why
//    @ModifyVariable(
//            method = "tesselate",
//            at = @At(value = "STORE"),
//            ordinal = 0
//    )
//    private int ff$alterColor(final int value, @Local(argsOnly = true) FluidState fluidState) {
//        if (FlowingFluids.config.enableMod && FlowingFluids.config.debugWaterLevelColours) {
//            return FFConfig.waterLevelColours[fluidState.getAmount()-1];
//        }
//        return value;
//    }

    @ModifyVariable(method = "tesselate", at = @At(value = "STORE"), ordinal = 0)
    private float ff$f(final float value, @Local(argsOnly = true) FluidState fluidState) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.debugWaterLevelColours) {
            return (FFConfig.waterLevelColours[fluidState.getAmount()-1] >> 16 & 255) / 255.0F;
        }
        return value;
    }
    @ModifyVariable(method = "tesselate", at = @At(value = "STORE"), ordinal = 1)
    private float ff$g(final float value, @Local(argsOnly = true) FluidState fluidState) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.debugWaterLevelColours) {
            return (FFConfig.waterLevelColours[fluidState.getAmount()-1] >> 8 & 255) / 255.0F;
        }
        return value;
    }
    @ModifyVariable(method = "tesselate", at = @At(value = "STORE"), ordinal = 2)
    private float ff$h(final float value, @Local(argsOnly = true) FluidState fluidState) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.debugWaterLevelColours) {
            return (FFConfig.waterLevelColours[fluidState.getAmount()-1] & 255) / 255.0F;
        }
        return value;
    }

}
