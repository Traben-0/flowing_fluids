package traben.flowing_fluids.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

import java.util.Iterator;

@Mixin(FarmBlock.class)
public class MixinFarmBlock {

    @Inject(
            method = "isNearWater",
            at = @At(value = "RETURN", ordinal = 0)
    )
    private static void ff$drainWater(final LevelReader level, final BlockPos pos, final CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 1) final BlockPos blockPos) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.farmlandDrainsWater
                && level instanceof ServerLevel serverLevel) {//always true
            FFFluidUtils.removeAmountFromFluidAtPosWithRemainder(serverLevel, blockPos, Fluids.WATER,1);
        }
    }
}
