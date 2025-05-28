package traben.flowing_fluids.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

@Mixin(ConcretePowderBlock.class)
public class MixinConcretePowderBlock {

    @ModifyConstant(method = "touchesLiquid", constant = @Constant(intValue = 1))
    private static int ff$drainWater(final int constant,
                                     @Local(ordinal = 0) final BlockPos.MutableBlockPos blockPos,
                                     @Local(ordinal = 0, argsOnly = true) final BlockGetter getter
    ) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.concreteDrainsWaterChance > 0
                && FlowingFluids.config.isWaterAllowed()
                && getter instanceof ServerLevel serverLevel
                && !FlowingFluids.config.dontTickAtLocation(blockPos, serverLevel)) {

            if (serverLevel.random.nextFloat() <= FlowingFluids.config.farmlandDrainWaterChance) {
                FFFluidUtils.removeAmountFromFluidAtPosWithRemainder(serverLevel, blockPos, Fluids.WATER,1);
            }
        }
        return constant;
    }
}
