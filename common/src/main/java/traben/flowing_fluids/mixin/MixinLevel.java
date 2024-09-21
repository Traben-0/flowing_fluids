package traben.flowing_fluids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.FluidFlowReceiver;

@Mixin(Level.class)
public abstract class MixinLevel {

    @Shadow
    public abstract RandomSource getRandom();

    @Shadow
    public abstract BlockState getBlockState(final BlockPos pos);

    @Shadow
    public abstract boolean setBlock(final BlockPos pos, final BlockState newState, final int flags);

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void flowing_fluids$displaceFluids(final BlockPos pos, final BlockState state, final int flags, final int recursionLeft, final CallbackInfoReturnable<Boolean> cir, final LevelChunk levelChunk, final Block block, final BlockState originalState) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.enableDisplacement && !FlowingFluids.isManeuveringFluids
                && !state.isAir() && state.getFluidState().isEmpty() && !originalState.getFluidState().isEmpty()
                && originalState.getFluidState().getType() instanceof FluidFlowReceiver flowSource
                && !state.is(Blocks.SPONGE)) {
            //fluid block was replaced, lets try and displace the fluid
            FlowingFluids.isManeuveringFluids = true;

            try {
                //try spread to the side as much as possible
                int amountRemaining = originalState.getFluidState().getAmount();
                for (Direction direction : FlowingFluids.getCardinalsShuffle(getRandom())) {
                    BlockPos offset = pos.relative(direction);
                    BlockState offsetState = getBlockState(offset);
                    if (offsetState.getFluidState().getType() instanceof FluidFlowReceiver) {
                        amountRemaining = flowSource.ff$tryFlowAmountIntoAndReturnRemainingAmount(amountRemaining, (Fluid) flowSource,
                                offsetState, (Level) (Object) this, offset, direction);
                        if (amountRemaining == 0) break;
                    } else if (offsetState.isAir()) {
                        setBlock(offset, originalState, 3);
                        amountRemaining = 0;
                        break;
                    }
                }
                if (amountRemaining > 0) {
                    //if we still have fluid left, try to displace upwards recursively
                    BlockPos.MutableBlockPos posTraversing = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
                    int height = levelChunk.getMaxBuildHeight();
                    while (amountRemaining > 0 && posTraversing.getY() < height) {
                        posTraversing.move(Direction.UP);
                        BlockState offsetState = getBlockState(posTraversing);
                        if (offsetState.getFluidState().getType() instanceof FluidFlowReceiver) {
                            amountRemaining = flowSource.ff$tryFlowAmountIntoAndReturnRemainingAmount(amountRemaining, (Fluid) flowSource,
                                    offsetState, (Level) (Object) this, posTraversing, Direction.UP);
                        } else if (offsetState.isAir()) {
                            setBlock(posTraversing, originalState, 3);
                            amountRemaining = 0;
                        } else {
                            break;
                        }
                    }

                    if (amountRemaining > 0 && FlowingFluids.config.debugSpreadPrint) {
                        //lost fluid will just have to happen
                        FlowingFluids.LOG.info("Failed to displace all fluid at {} remaining: {}, originalAmount {}", pos.toShortString(), amountRemaining, originalState.getFluidState().getAmount());
                    }
                }

            } finally {
                FlowingFluids.isManeuveringFluids = false;
            }
        }
    }
}
