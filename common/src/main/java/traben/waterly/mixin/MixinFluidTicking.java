package traben.waterly.mixin;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.waterly.FluidGetterByAmount;
import traben.waterly.Waterly;

import java.util.*;
import java.util.function.ToIntFunction;


@Mixin(FlowingFluid.class)
public abstract class MixinFluidTicking extends Fluid implements FluidGetterByAmount {


    @Shadow public abstract FluidState getSource(final boolean bl);

    @Shadow public abstract FluidState getFlowing(final int i, final boolean bl);

    @Shadow public abstract boolean canPassThroughWall(final Direction direction, final BlockGetter blockGetter, final BlockPos blockPos, final BlockState blockState, final BlockPos blockPos2, final BlockState blockState2);

    @Shadow public abstract boolean canHoldFluid(final BlockGetter blockGetter, final BlockPos blockPos, final BlockState blockState, final Fluid fluid);

    @Shadow protected abstract int getDropOff(final LevelReader levelReader);

    @Shadow protected abstract void spreadTo(final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState blockState, final Direction direction, final FluidState fluidState);


    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    private void waterly$tickMixin(final Level level, final BlockPos blockPos, final FluidState fluidState, final CallbackInfo ci) {
        if (true) {
            if (!fluidState.isEmpty() && fluidState.getAmount() == level.getFluidState(blockPos).getAmount()) {
                BlockState thisState = level.getBlockState(blockPos);
                BlockPos posDown = blockPos.below();
                BlockState stateDown = level.getBlockState(posDown);

                int originalAmount = fluidState.getAmount();
                int amount = originalAmount;
                if (waterly$canSpreadTo2(fluidState.getType(), fluidState.getAmount(),level, blockPos, thisState, Direction.DOWN, posDown, stateDown, level.getFluidState(posDown))) {
                    int fluidDownAmount = level.getFluidState(posDown).getAmount();
                    int transferAmount = Math.min(8 - fluidDownAmount, amount);
                    if (transferAmount > 0) {

                        int fluidDownNewAmount = fluidDownAmount + transferAmount;
                        amount = amount - transferAmount;

//                        System.out.println("downflow result: "+ originalAmount +", "+ fluidDownAmount +" -> " + amount +", "+ fluidDownNewAmount);

                        if (amount != originalAmount) {
                            originalAmount = amount;
                            waterly$setOrRemoveWaterAmountAt(level, blockPos, amount, thisState, Direction.DOWN);
                        }

                        waterly$spreadTo2(level, posDown, stateDown, Direction.DOWN, fluidDownNewAmount);

                    }
                }

                if (amount > getDropOff(level)) {//1 for water  2 for lava in overworld
                    Direction dir = waterly$getLowestSpreadable(level, blockPos, fluidState, amount);
                    //dir is null if no spreadable block was found
                    if (dir != null) {
                        var pos = blockPos.relative(dir);
                        //dir is < amount if spreadable block was found
                        int fluidDirAmount = level.getFluidState(pos).getAmount();
                        int difference = amount - fluidDirAmount;
                        //calculcate the amount that would level both liquids without loosing any total value using integers
                        int postTransfer = fluidDirAmount + difference / 2;
                        //if the difference is odd, the difference is not divisible by 2 and we need to add 1 to the transfer amount
                        boolean offBalance = (difference % 2 != 0);
                        int toLeft = postTransfer;
                        int toRight = postTransfer;
                        if (offBalance) {
                            if (waterly$randomBool.nextBoolean()) {
                                toLeft++;
                            } else {
                                toRight++;
                            }
                        }
//                        System.out.println("spread result: "+ amount +", " + fluidDirAmount +" -> "+toLeft + ", " + toRight);
                        amount = toLeft;
                        if (amount != originalAmount) {
                            waterly$setOrRemoveWaterAmountAt(level, blockPos, amount, thisState, dir.getOpposite());
                        }

                        if (toRight != fluidDirAmount)
                            waterly$spreadTo2(level, pos, level.getBlockState(pos), dir, toRight);
                    }

                }else if (amount > 0) {
                    //spill over edge if possible
                    Direction dir = waterly$getLowestSpreadableEdge(level, blockPos, fluidState, amount);
                    //dir is null if no spreadable block was found
                    if (dir != null) {
                        var pos = blockPos.relative(dir);
                        waterly$setOrRemoveWaterAmountAt(level, blockPos, 0, thisState, dir);
                        waterly$spreadTo2(level, pos, level.getBlockState(pos), dir, amount);
                    }
                }

            }else{
                System.out.println("ticked invalid fluid state: "+ fluidState.getAmount() + " vs " + level.getFluidState(blockPos).getAmount());
            }
            ci.cancel();
        }
    }



    @Unique
    private void waterly$setOrRemoveWaterAmountAt(final Level level, final BlockPos blockPos, final int amount, final BlockState thisState, Direction direction) {
        if (amount > 0) {
            waterly$spreadTo2(level, blockPos, thisState, direction, amount);
        }else{
            waterly$removeWater(level, blockPos, thisState);
        }
    }

    @Inject(method = "getNewLiquid", at = @At(value = "HEAD"), cancellable = true)
    private void waterly$validateLiquidMixin(final Level level, final BlockPos blockPos, final BlockState blockState, final CallbackInfoReturnable<FluidState> cir) {
        if (true) {
            int amount = level.getFluidState(blockPos).getAmount();
//            return getOfAmount(level, blockPos, blockState, amount);
            cir.setReturnValue(waterly$getOfAmount(level, blockPos, blockState, amount));
        }
    }


    @Unique
    private final Random waterly$randomBool = new Random();

    @Unique
    private @Nullable Direction waterly$getLowestSpreadableEdge(Level level, BlockPos blockPos, FluidState fluidState, int amount){
        ToIntFunction<Direction> func = (dir) -> level.getFluidState(blockPos.relative(dir).below()).getAmount();

        return Waterly.getCardinalsShuffle().stream()
                .filter(dir -> {
                    BlockPos pos = blockPos.relative(dir);
                    BlockState state = level.getBlockState(pos);
                    var fluidState2 = level.getFluidState(pos);
                    var posDown = pos.below();
                    var stateDown = level.getBlockState(posDown);
                    var fluidStateDown = level.getFluidState(posDown);
                    return waterly$canSpreadTo2(fluidState.getType(), fluidState.getAmount(), level, blockPos, level.getBlockState(blockPos), dir, pos, state, fluidState2)
                            && fluidState2.isEmpty()// let that fluid flow down instead
                            && waterly$canSpreadTo2(fluidState.getType(), 8, level, pos, state, Direction.DOWN, posDown, stateDown, fluidStateDown)
                            && fluidStateDown.getAmount() < 8; //is a drop
                }).min(Comparator.comparingInt(func)).orElse(null);
    }

    @Unique
    private @Nullable Direction waterly$getLowestSpreadable(Level level, BlockPos blockPos, FluidState fluidState, int amount){
        ToIntFunction<Direction> func = (dir) ->level.getFluidState(blockPos.relative(dir)).getAmount();

        return Waterly.getCardinalsShuffle().stream()
                .filter(dir -> {
                    BlockPos pos = blockPos.relative(dir);
                    BlockState state = level.getBlockState(pos);
                    var fluidState2 = level.getFluidState(pos);
                    return waterly$canSpreadTo2(fluidState.getType(), fluidState.getAmount(), level, blockPos, level.getBlockState(blockPos), dir, pos, state, fluidState2)
                            && fluidState2.getAmount() < amount;
                }).min(Comparator.comparingInt(func)).orElse(null);
    }

    @Unique
    protected void waterly$spreadTo2(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, Direction direction, int amount) {
        this.spreadTo(levelAccessor, blockPos, blockState, direction, waterly$getOfAmount(levelAccessor, blockPos, blockState, amount));
    }


    public FluidState waterly$getOfAmount(LevelAccessor level, BlockPos blockPos, BlockState blockState, int amount) {
        if (amount > 8) System.out.println("amount > 8");

        BlockPos posUp = blockPos.above();
        BlockState bStateUp = level.getBlockState(posUp);
        FluidState fStateUp = bStateUp.getFluidState();
        if (!fStateUp.isEmpty() && fStateUp.getType().isSame(this) && this.canPassThroughWall(Direction.UP, level, blockPos, blockState, posUp, bStateUp)) {
            return amount == 8 ? this.getSource(false) : this.getFlowing(amount, false);//todo true here broke? redundant now?
        } else {
            if (amount <= 0) {
                System.out.println("AMOUNT <= 0!!!!!!!!!!!!!!");
                return Fluids.EMPTY.defaultFluidState();
            } else {
                return amount == 8 ? this.getSource(false) : this.getFlowing(amount, false);
            }
        }

    }

    @Unique
    protected void waterly$removeWater(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState) {
        if (blockState.getBlock() instanceof LiquidBlockContainer) {
            ((BucketPickup)blockState.getBlock()).pickupBlock(null,levelAccessor, blockPos, blockState);
        } else {
            levelAccessor.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
        }

    }


    @Unique
    private boolean waterly$canSpreadTo2(Fluid sourceFluid, int sourceAmount, BlockGetter blockGetter, BlockPos blockPos, BlockState blockState, Direction direction, BlockPos blockPos2, BlockState blockState2, FluidState fluidState) {
        //add extra fluid check for replacing into self
        return (fluidState.canBeReplacedWith(blockGetter, blockPos2, sourceFluid, direction) || waterly$canMoveIntoSelf(sourceFluid, fluidState, direction, sourceAmount))
                && this.canPassThroughWall(direction, blockGetter, blockPos, blockState, blockPos2, blockState2)
                && this.canHoldFluid(blockGetter, blockPos2, blockState2, fluidState.getType());
    }

    @Unique
    private boolean waterly$canMoveIntoSelf(Fluid thisFluid, FluidState fluidStateTo, Direction direction, int amount) {
        if (direction == Direction.UP) return false;

        if (fluidStateTo.isEmpty()) return true;

        if(fluidStateTo.getType().isSame(thisFluid)){
            if(direction == Direction.DOWN){
                return fluidStateTo.getAmount() < 8;
            }else {
                return fluidStateTo.getAmount() < amount;
            }
        }
        return false;
    }
}
