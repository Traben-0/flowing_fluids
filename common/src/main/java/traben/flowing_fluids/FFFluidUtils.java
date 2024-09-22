package traben.flowing_fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FFFluidUtils {

    static final List<Direction> CARDINALS = new ArrayList<>();

    static {
        CARDINALS.add(Direction.NORTH);
        CARDINALS.add(Direction.SOUTH);
        CARDINALS.add(Direction.EAST);
        CARDINALS.add(Direction.WEST);
    }

    public static FluidState getStateForFluidByAmount(Fluid fluid, int amount) {
        if (fluid instanceof FlowingFluid flowing) {
            return amount == 8 ? flowing.getSource(false) : flowing.getFlowing(amount, false);
        }
        return amount == 8 ? fluid.defaultFluidState() : fluid.defaultFluidState().trySetValue(FlowingFluid.LEVEL, amount);
    }


    public static BlockState getBlockForFluidByAmount(Fluid fluid, int amount) {
        return getStateForFluidByAmount(fluid, amount).createLegacyBlock();
    }


    public static boolean setFluidStateAtPosToNewAmount(LevelAccessor levelAccessor, BlockPos pos, Fluid fluid, int newAmount) {
        if (newAmount < 1) {
            return removeAllFluidAtPos(levelAccessor, pos, fluid);
        }

        //check if we are dealing with a waterlogged block
        var blockState = levelAccessor.getBlockState(pos);
        if (blockState.getBlock() instanceof LiquidBlockContainer liquidBlockContainer) {
            if (newAmount == 8) {
                return liquidBlockContainer.placeLiquid(levelAccessor, pos, blockState, getStateForFluidByAmount(fluid, newAmount));
            }else if (blockState.getBlock() instanceof BucketPickup bucketPickup) {
                //always drain the water loggable block if it's not full
                bucketPickup.pickupBlock(null, levelAccessor, pos, blockState);
                return true;
            }
            //if we cant fill or drain it check if we can just replace it with the new fluid level by itself
            if (!blockState.canBeReplaced(fluid)) {
                return false;//todo infinite source block possible???
            }
        }

        if (!blockState.isAir() && fluid instanceof FlowingFluid flowingFluid) {
            flowingFluid.beforeDestroyingBlock(levelAccessor, pos, blockState);
        }
        //else place fluid block
        return levelAccessor.setBlock(pos, getStateForFluidByAmount(fluid, newAmount).createLegacyBlock(), 3);
    }


    public static boolean removeAllFluidAtPos(LevelAccessor levelAccessor, BlockPos pos, Fluid fluid) {
        var blockState = levelAccessor.getBlockState(pos);
        if (blockState.getBlock() instanceof LiquidBlockContainer
                && blockState.getBlock() instanceof BucketPickup bucketPickup) {
            bucketPickup.pickupBlock(null, levelAccessor, pos, blockState);
            return true;
        }

        if (!blockState.isAir() && fluid instanceof FlowingFluid flowingFluid) {
            flowingFluid.beforeDestroyingBlock(levelAccessor, pos, blockState);
        }

        return levelAccessor.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }


    public static int removeAmountFromFluidAtPosWithRemainder(LevelAccessor levelAccessor, BlockPos pos, Fluid fluid, int removeAmount) {
        FluidState state = levelAccessor.getFluidState(pos);
        if (state.getType().isSame(fluid)) {
            int currentAmount = state.getAmount();
            if (currentAmount <= removeAmount) {
                removeAllFluidAtPos(levelAccessor, pos, fluid);
                return removeAmount - currentAmount;
            } else {
                setFluidStateAtPosToNewAmount(levelAccessor, pos, fluid, currentAmount - removeAmount);
                return 0;
            }
        }
        return removeAmount;
    }


    public static int addAmountToFluidAtPosWithRemainder(LevelAccessor levelAccessor, BlockPos pos, Fluid fluid, int addAmount) {
        FluidState state = levelAccessor.getFluidState(pos);
        if (state.getType().isSame(fluid)) {
            int currentAmount = state.getAmount();
            if (currentAmount == 8) {
                return addAmount;
            }
            if (currentAmount + addAmount <= 8) {
                setFluidStateAtPosToNewAmount(levelAccessor, pos, fluid, currentAmount + addAmount);
                return 0;
            } else {
                setFluidStateAtPosToNewAmount(levelAccessor, pos, fluid, 8);
                return currentAmount + addAmount - 8;
            }
        }
        return addAmount;
    }


    public static boolean canFluidFlowFromPosToDirection(FlowingFluid fluid, int amount, LevelAccessor levelAccessor, BlockPos fromPos, Direction direction) {
        var blockPos2 = fromPos.relative(direction);
        var blockState2 = levelAccessor.getBlockState(blockPos2);
        var fluidState2 = blockState2.getFluidState();
        return canFluidFlowFromPosToDirection(fluid, amount, levelAccessor, fromPos, levelAccessor.getBlockState(fromPos), direction, blockPos2, blockState2, fluidState2);
    }


    public static boolean canFluidFlowFromPosToDirection(FlowingFluid sourceFluid, int sourceAmount, BlockGetter blockGetter,
                                                         BlockPos blockPos, BlockState blockState, Direction direction,
                                                         BlockPos blockPos2, BlockState blockState2, FluidState fluidState2) {
        //add extra fluid check for replacing into self
        return (fluidState2.canBeReplacedWith(blockGetter, blockPos2, sourceFluid, direction) || canFitIntoFluid(sourceFluid, fluidState2, direction, sourceAmount, blockState2))
                && sourceFluid.canPassThroughWall(direction, blockGetter, blockPos, blockState, blockPos2, blockState2)
                && sourceFluid.canHoldFluid(blockGetter, blockPos2, blockState2, sourceFluid);
    }


    private static boolean canFitIntoFluid(Fluid thisFluid, FluidState fluidStateTo, Direction direction, int amount, BlockState blockStateTo) {
        if (fluidStateTo.isEmpty()){
            return true;
        }
        if (fluidStateTo.getType().isSame(thisFluid)) {
            if (direction == Direction.DOWN) {
                return fluidStateTo.getAmount() < 8;
            } else {
                return fluidStateTo.getAmount() < amount;
            }
        }
        return false;
    }


    public static int collectConnectedFluidAmountAndRemove(final LevelAccessor levelAccessor, final BlockPos blockPos, final int minAmountRequired, final int maxAmountToFind, final FlowingFluid fluid) {
        var originalState = levelAccessor.getFluidState(blockPos);
        int originalAmount = originalState.getAmount();
        if (originalState.getType().isSame(fluid) && originalAmount > 0) {

            //check for quick exit
            if (originalAmount >= maxAmountToFind) {
                FFFluidUtils.setFluidStateAtPosToNewAmount(levelAccessor, blockPos, fluid, originalAmount - maxAmountToFind);
                return maxAmountToFind;
            }

            List<BlockPos> toCheck = new ArrayList<>();
            toCheck.add(blockPos);
            for (Direction direction : Direction.allShuffled(levelAccessor.getRandom())) {
                BlockPos offset = blockPos.relative(direction);
                toCheck.add(offset);
            }

            List<Runnable> onSuccessAirSetters = new ArrayList<>();
            int foundAmount = 0;

            for (int i = 0; i < toCheck.size(); i++) {
                var pos = toCheck.get(i);

                if (toCheck.size() > 40) break;

                var state = levelAccessor.getFluidState(pos);
                if (fluid.isSame(state.getType())) {
                    int amount = state.getAmount();
                    if (amount > 0) {
                        foundAmount += amount;
                        if (foundAmount > maxAmountToFind) {
                            final int finalLevel = foundAmount - maxAmountToFind;
                            onSuccessAirSetters.add(() -> FFFluidUtils.setFluidStateAtPosToNewAmount(levelAccessor, pos, fluid, finalLevel));
                            foundAmount = maxAmountToFind;
                            break;
                        } else {
                            onSuccessAirSetters.add(() -> FFFluidUtils.removeAllFluidAtPos(levelAccessor, pos, fluid));
                            if (foundAmount == maxAmountToFind) break;
                            for (Direction direction : Direction.allShuffled(levelAccessor.getRandom())) {
                                BlockPos offset = pos.relative(direction);
                                if (!toCheck.contains(offset)) toCheck.add(offset);
                            }
                        }
                    }
                }
            }
            if (foundAmount < minAmountRequired) {
                //failed to find enough fluid so cancel
                return 0;
            }
            //levelAccessor.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 11);
            onSuccessAirSetters.forEach(Runnable::run);
            return foundAmount;
        }
        return 0;
    }

    public static List<Direction> getCardinalsShuffle(RandomSource random) {
        Collections.shuffle(CARDINALS, random::nextInt);
        return CARDINALS;
    }

}
