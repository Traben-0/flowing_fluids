package traben.flowing_fluids.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

#if MC > MC_20_1

import traben.flowing_fluids.FFFluidUtils;
@Pseudo
@Mixin(FFFluidUtils.class)
public abstract class MixinFluidRenderer {

}
#else

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfig;

import java.util.Arrays;

@Pseudo
@Mixin(FluidRenderer.class)
public abstract class MixinFluidRenderer {

    @ModifyVariable(
            method = "render",
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;",
                    shift = At.Shift.AFTER)
            , ordinal = 0, require = 0
    )
    private Vec3 ff$alterFlowDir(final Vec3 value) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.hideFlowingTexture) {
            return Vec3.ZERO;
        }
        return value;
    }

    @Unique
    private static final ColorProvider<FluidState> ff$waterLevelColours = (worldSlice, blockPos, fluidState, modelQuadView, ints)
            -> Arrays.fill(ints, FFConfig.waterLevelColours[fluidState.getAmount() - 1]);

    @ModifyVariable(
            method = "render",
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/FluidRenderer;getColorProvider(Lnet/minecraft/world/level/material/Fluid;Lnet/fabricmc/fabric/api/client/render/fluid/v1/FluidRenderHandler;)Lme/jellysquid/mods/sodium/client/model/color/ColorProvider;",
                    shift = At.Shift.AFTER)
            , ordinal = 0, require = 0
    )
    private ColorProvider<FluidState> ff$alterColor(final ColorProvider<FluidState> value) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.debugWaterLevelColours) {
            return ff$waterLevelColours;
        }
        return value;
    }
}
#endif

