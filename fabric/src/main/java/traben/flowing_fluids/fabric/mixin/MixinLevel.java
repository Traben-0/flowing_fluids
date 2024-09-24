package traben.flowing_fluids.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import traben.flowing_fluids.FFFluidUtils;

@Mixin(Level.class)
public abstract class MixinLevel {


    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void flowing_fluids$displaceFluids(final BlockPos pos, final BlockState state, final int flags, final int recursionLeft, final CallbackInfoReturnable<Boolean> cir, final LevelChunk levelChunk, final Block block, final BlockState originalState) {
        FFFluidUtils.displaceFluids((Level) (Object) this,pos, state, flags, levelChunk, originalState);
    }

}
