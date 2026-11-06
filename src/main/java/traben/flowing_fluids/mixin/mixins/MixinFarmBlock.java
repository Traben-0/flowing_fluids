package traben.flowing_fluids.mixin.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

//#if MC >= 26.1
//$$ @Mixin(net.minecraft.world.level.block.FarmlandBlock.class)
//#else
@Mixin(net.minecraft.world.level.block.FarmBlock.class)
//#endif
public class MixinFarmBlock {

    //#if MC >= 26.3
    //$$ @Inject(method = "isNearWater", at = @At(value = "RETURN"))
    //$$ private static void findAndDrain(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
    //$$     // On 26.3+ its no longer trivial to get the water source block position, so we just run another check for any
    //$$     // water source in the area and drain one of them.
    //$$     if (cir.getReturnValue() // Only run if the vanilla check found water.
    //$$             && FlowingFluids.config.enableMod
    //$$             && FlowingFluids.config.farmlandDrainWaterChance > 0
    //$$             && FlowingFluids.config.isWaterAllowed()
    //$$             && level instanceof ServerLevel serverLevel
    //$$             && serverLevel.random.nextFloat() <= FlowingFluids.config.farmlandDrainWaterChance
    //$$             && !FlowingFluids.config.dontTickAtLocation(pos, serverLevel)) {
    //$$
    //$$         for (BlockPos blockPos2 : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
    //$$             if (level.getFluidState(blockPos2).is(net.minecraft.tags.FluidTags.WATER)) {
    //$$                 FFFluidUtils.removeAmountFromFluidAtPosWithRemainder(serverLevel, blockPos2, Fluids.WATER, 1);
    //$$                 return; // Only drain one water source per check
    //$$             }
    //$$         }
    //$$     }
    //$$ }
    //#else
    @Inject(method = "isNearWater", at = @At(value = "RETURN", ordinal = 0))
    private static void ff$drainWater(final LevelReader level, final BlockPos pos, final CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 1) final BlockPos blockPos) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.farmlandDrainWaterChance > 0
                && FlowingFluids.config.isWaterAllowed()
                && level instanceof ServerLevel serverLevel // always true
                && !FlowingFluids.config.dontTickAtLocation(blockPos, serverLevel)) {

            if (serverLevel.random.nextFloat() <= FlowingFluids.config.farmlandDrainWaterChance) {
                FFFluidUtils.removeAmountFromFluidAtPosWithRemainder(serverLevel, blockPos, Fluids.WATER,1);
            }
        }
    }
    //#endif
}
