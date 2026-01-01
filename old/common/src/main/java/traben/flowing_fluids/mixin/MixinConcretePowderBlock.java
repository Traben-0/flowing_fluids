package traben.flowing_fluids.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

@Mixin(ConcretePowderBlock.class)
public class MixinConcretePowderBlock {

    @ModifyExpressionValue(method = "touchesLiquid",
            require = 0, //todo breaks on older forge only
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;isFaceSturdy(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"))
    private static boolean ff$drainWater(final boolean original,
                                         @Local(ordinal = 0) final BlockPos.MutableBlockPos blockPos,
                                         @Local(ordinal = 0, argsOnly = true) final BlockGetter getter
    ) {
        if (!original
                && FlowingFluids.config.enableMod
                && FlowingFluids.config.concreteDrainsWaterChance > 0
                && FlowingFluids.config.isWaterAllowed()
                && getter instanceof ServerLevel serverLevel
                && !FlowingFluids.config.dontTickAtLocation(blockPos, serverLevel)) {

            if (serverLevel.random.nextFloat() <= FlowingFluids.config.concreteDrainsWaterChance) {
                FFFluidUtils.removeAmountFromFluidAtPosWithRemainder(serverLevel, blockPos, Fluids.WATER,1);
            }
        }
        return original;
    }
}
