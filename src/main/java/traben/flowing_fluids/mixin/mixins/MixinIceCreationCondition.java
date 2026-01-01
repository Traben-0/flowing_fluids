package traben.flowing_fluids.mixin.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import traben.flowing_fluids.FlowingFluids;

@Mixin(Biome.class)
public class MixinIceCreationCondition {

    @WrapOperation(
            method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getType()Lnet/minecraft/world/level/material/Fluid;")
    )
    private Fluid ff$freeze(final FluidState instance, final Operation<Fluid> original) {
        if (instance != null
                && FlowingFluids.config.enableMod
                && FlowingFluids.config.isWaterAllowed()
                && instance.getType() == Fluids.FLOWING_WATER
                && instance.getAmount() >= FlowingFluids.config.minWaterLevelForIce) {
            return Fluids.WATER; // force it to see water so we can freeze
        }
        return original.call(instance);
    }
}
