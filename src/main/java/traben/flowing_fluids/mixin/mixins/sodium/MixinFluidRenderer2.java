package traben.flowing_fluids.mixin.mixins.sodium;

//#if MC == 12001
//$$ import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
//$$ import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
//$$ import net.minecraft.world.level.material.FluidState;
//$$ import net.minecraft.world.phys.Vec3;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Pseudo;
//$$ import org.spongepowered.asm.mixin.Unique;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.ModifyVariable;
//$$ import traben.flowing_fluids.FlowingFluids;
//$$ import traben.flowing_fluids.config.FFConfig;
//$$
//$$ import java.util.Arrays;
//$$
//$$ @Pseudo
//$$ @Mixin(FluidRenderer.class)
//$$ public abstract class MixinFluidRenderer2 {
//$$
//$$     @ModifyVariable(
//$$             method = "render",
//$$             at = @At(value = "INVOKE_ASSIGN",
//$$                     target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;",
//$$                     shift = At.Shift.AFTER)
//$$             , ordinal = 0
//$$             , require = 0
//$$     )
//$$     private Vec3 ff$alterFlowDir(final Vec3 value) {
//$$         if (FlowingFluids.config.enableMod
//$$                 && FlowingFluids.config.hideFlowingTexture) {
//$$             return Vec3.ZERO;
//$$         }
//$$         return value;
//$$     }
//$$
//$$     @Unique
//$$     private static final ColorProvider<FluidState> ff$waterLevelColours = (worldSlice, blockPos, fluidState, modelQuadView, ints)
//$$             -> Arrays.fill(ints, FFConfig.waterLevelColours[fluidState.getAmount() - 1]);
//$$
//$$
//$$     @ModifyVariable(method = "render", at = @At("HEAD"),
//$$             ordinal = 2,
//$$             require = 0, remap = false)
//$$     private ColorProvider<FluidState> ff$alterColor(final ColorProvider<FluidState> value) {
//$$         if (FlowingFluids.config.enableMod && FlowingFluids.config.debugWaterLevelColours) {
//$$             return ff$waterLevelColours;
//$$         }
//$$         return value;
//$$     }
//$$ }
//#else
//#endif

//#if FABRIC && MC >= 12100 || !FORGE && MC >= 12103 && MC < 12109
/// difference is still mostly identical but seems 1.20.1 sodium won't get the 0.6 update so just making separate mixin down here

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;

import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfig;

import java.util.Arrays;

@Pseudo
@Mixin(DefaultFluidRenderer.class)
public abstract class MixinFluidRenderer2 {

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;")
            , require = 0)
    private Vec3 ff$alterFlowDir(Vec3 original) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.hideFlowingTexture) {
            return Vec3.ZERO;
        }
        return original;
    }

    @Unique
    private static final ColorProvider<FluidState> ff$waterLevelColours = (worldSlice, blockPos, mut, fluidState, modelQuadView, ints
                                                                            //#if MC >= 12106
                                                                            , a
                                                                            //#endif
    ) -> Arrays.fill(ints, FFConfig.waterLevelColours[fluidState.getAmount() - 1]);


    @ModifyVariable(method = "render", at = @At("HEAD"), ordinal = 0, argsOnly = true
            , require = 0)
    private ColorProvider<FluidState> ff$alterColor(final ColorProvider<FluidState> value) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.debugWaterLevelColours) {
            return ff$waterLevelColours;
        }
        return value;
    }
}

//#elseif MC != 12001
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import traben.flowing_fluids.mixin.CancelTarget;
//$$
//$$ @Mixin(CancelTarget.class)
//$$ public abstract class MixinFluidRenderer2 { }
//#endif