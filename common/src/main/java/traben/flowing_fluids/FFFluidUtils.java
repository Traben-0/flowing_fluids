package traben.flowing_fluids;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;

public class FFFluidUtils {

    public static @NotNull ResourceLocation res(String fullPath){
        #if MC >= MC_21
        return ResourceLocation.parse(fullPath);
        #else
        return new ResourceLocation(fullPath);
        #endif
    }

    public static @NotNull ResourceLocation res(String namespace, String path){
        #if MC >= MC_21
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        #else
        return new ResourceLocation(namespace, path);
        #endif
    }

    static final List<Direction> CARDINALS = new ArrayList<>();

    static {
        CARDINALS.add(Direction.NORTH);
        CARDINALS.add(Direction.SOUTH);
        CARDINALS.add(Direction.EAST);
        CARDINALS.add(Direction.WEST);
    }

    public static boolean canFluidFlowToNeighbourFromPos(LevelAccessor accessor, BlockPos pos, FlowingFluid fluid, int amount) {
        for (Direction direction :Direction.Plane.HORIZONTAL) {
            if (FFFluidUtils.canFluidFlowFromPosToDirection(fluid, amount, accessor, pos, direction)) {

                return true;
            }
        }
        return false;
    }

    public static FluidState getStateForFluidByAmount(Fluid fluid, int amount) {
        if (amount < 1) {
            return Fluids.EMPTY.defaultFluidState();
        }
        if (fluid instanceof FlowingFluid flowing) {
            return amount >= 8 ? flowing.getSource(false) : flowing.getFlowing(amount, false);
        }
        return amount >= 8 ? fluid.defaultFluidState() : fluid.defaultFluidState().trySetValue(FlowingFluid.LEVEL, amount);
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
                bucketPickup.pickupBlock(#if MC > MC_20_1 null, #endif levelAccessor, pos, blockState);
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
            bucketPickup.pickupBlock(#if MC > MC_20_1 null, #endif levelAccessor, pos, blockState);
            return true;
        }

        if (!blockState.isAir() && fluid instanceof FlowingFluid flowingFluid) {//todo needed for remove??
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

    public static int addAmountToFluidAtPosWithRemainderAndTrySpreadIfFull(LevelAccessor levelAccessor, BlockPos pos, FlowingFluid fluid, int addAmount) {
        var data = placeConnectedFluidAmountAndPlaceAction(levelAccessor, pos, addAmount, fluid);
        if (data.first() != addAmount) {
            data.second().run();
            return data.first();
        }
        return addAmount;
    }

    public static int addAmountToFluidAtPosWithRemainderAndTrySpreadIfFull(LevelAccessor levelAccessor, BlockPos pos, FlowingFluid fluid, int addAmount, boolean canSpreadUp, boolean canSpreadDown) {
        var data = placeConnectedFluidAmountAndPlaceAction(levelAccessor, pos, addAmount, fluid, 80, canSpreadUp, canSpreadDown);
        if (data.first() != addAmount) {
            data.second().run();
            return data.first();
        }
        return addAmount;
    }

    public static int addAmountToFluidAtPosWithRemainder(LevelAccessor levelAccessor, BlockPos pos, Fluid fluid, int addAmount) {
        FluidState state = levelAccessor.getFluidState(pos);
        if (state.isEmpty() || state.getType().isSame(fluid)) {
            int currentAmount = state.getAmount();
            if (currentAmount == 8) {
                return addAmount;
            }
            if (currentAmount + addAmount <= 8) {
                if (setFluidStateAtPosToNewAmount(levelAccessor, pos, fluid, currentAmount + addAmount)) {
                    return 0;
                }
            } else {
                if (setFluidStateAtPosToNewAmount(levelAccessor, pos, fluid, 8)) {
                    return currentAmount + addAmount - 8;
                }
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

    public static boolean canFluidFlowFromPosToDirectionFitOverride(FlowingFluid sourceFluid, BlockGetter blockGetter,
                                                         BlockPos blockPos, BlockState blockState, Direction direction,
                                                         BlockPos blockPos2, BlockState blockState2) {
        //add extra fluid check for replacing into self
        return sourceFluid.canPassThroughWall(direction, blockGetter, blockPos, blockState, blockPos2, blockState2) && sourceFluid.canHoldFluid(blockGetter, blockPos2, blockState2, sourceFluid);
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

    public static Pair<Integer, Runnable> placeConnectedFluidAmountAndPlaceAction(final LevelAccessor levelAccessor, final BlockPos blockPos, final int amountToPlace, final FlowingFluid fluid) {
        return placeConnectedFluidAmountAndPlaceAction(levelAccessor, blockPos, amountToPlace, fluid, 80, true, true);
    }

    public static Pair<Integer, Runnable> placeConnectedFluidAmountAndPlaceAction(final LevelAccessor levelAccessor, final BlockPos blockPos, final int amountToPlace, final FlowingFluid fluid, int depth, boolean doUp, boolean doDown) {
        var originalState = levelAccessor.getFluidState(blockPos);
        int originalAmount = originalState.getAmount();
        if (originalState.getType().isSame(fluid) && originalAmount > 0) {

            //check for quick exit
            if (originalAmount + amountToPlace <= 8) {
                return Pair.of(0,()->FFFluidUtils.setFluidStateAtPosToNewAmount(levelAccessor, blockPos, fluid, originalAmount + amountToPlace));
            }

            List<BlockPos> toCheck = new ArrayList<>();
            toCheck.add(blockPos);

            final Consumer<BlockPos> addSurroundingPositions = blockPos1 -> {
                for (Direction direction : getCardinalsShuffle(levelAccessor.getRandom())) {
                    BlockPos offset = blockPos1.relative(direction);
                    if (!toCheck.contains(offset)) toCheck.add(offset);
                }
                if (doUp) {
                    // do these last just as preference
                    BlockPos up = blockPos1.above();
                    if (!toCheck.contains(up)) toCheck.add(up);
                }
                if (doDown) {
                    BlockPos down = blockPos1.below();
                    if (!toCheck.contains(down)) toCheck.add(down);
                }

            };
            addSurroundingPositions.accept(blockPos);

            List<Runnable> onSuccessPlacers = new ArrayList<>();
            int amountLeftToPlace = amountToPlace;

            for (int i = 0; i < toCheck.size(); i++) {
                var pos = toCheck.get(i);

                if (toCheck.size() > depth) break;

                var state = levelAccessor.getFluidState(pos);
                if (fluid.isSame(state.getType())
                        || (state.isEmpty() && levelAccessor.getBlockState(pos).isAir())) { // only concerned with air blocks, we aren't going to f around with waterloggables here
                    int space = 8-state.getAmount();
                    if (space > 0) {

                        if (space >= amountLeftToPlace) {
                            int newAmount = state.getAmount() + amountLeftToPlace;
                            onSuccessPlacers.add(() -> FFFluidUtils.setFluidStateAtPosToNewAmount(levelAccessor, pos, fluid, newAmount));
                            amountLeftToPlace = 0;
                            break;
                        } else {
                            onSuccessPlacers.add(() -> FFFluidUtils.setFluidStateAtPosToNewAmount(levelAccessor, pos, fluid, 8));
                            amountLeftToPlace -= space;
                        }
                    }
                    // keep searching
                    addSurroundingPositions.accept(pos);
                }
            }
            if (amountLeftToPlace == amountToPlace) {
                //failed to find enough fluid so cancel
                return Pair.of(amountToPlace, null);
            }

            return Pair.of(amountLeftToPlace, () -> onSuccessPlacers.forEach(Runnable::run));
        }
        return Pair.of(amountToPlace, null);
    }


    public static int collectConnectedFluidAmountAndRemove(final LevelAccessor levelAccessor, final BlockPos blockPos, final int minAmountRequired, final int maxAmountToFind, final FlowingFluid fluid) {
        var data = collectConnectedFluidAmountAndRemoveAction(levelAccessor,blockPos, minAmountRequired, maxAmountToFind, fluid);
        if (data.first() != 0) {
            data.second().run();
            return data.first();
        }
        return 0;
    }

    public static Pair<Integer, Runnable> collectConnectedFluidAmountAndRemoveAction(final LevelAccessor levelAccessor, final BlockPos blockPos, final int minAmountRequired, final int maxAmountToFind, final FlowingFluid fluid) {
        return collectConnectedFluidAmountAndRemoveAction(levelAccessor, blockPos, minAmountRequired, maxAmountToFind, fluid, 40);
    }

    public static Pair<Integer, Runnable> collectConnectedFluidAmountAndRemoveAction(final LevelAccessor levelAccessor, final BlockPos blockPos, final int minAmountRequired, final int maxAmountToFind, final FlowingFluid fluid, int depth) {
        var originalState = levelAccessor.getFluidState(blockPos);
        int originalAmount = originalState.getAmount();
        if (originalState.getType().isSame(fluid) && originalAmount > 0) {

            //check for quick exit
            if (originalAmount >= maxAmountToFind) {
                return Pair.of(maxAmountToFind,()->{FFFluidUtils.setFluidStateAtPosToNewAmount(levelAccessor, blockPos, fluid, originalAmount - maxAmountToFind);});
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

                if (toCheck.size() > depth) break;

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
                return Pair.of(0, null);
            }

            return Pair.of(foundAmount, ()->{onSuccessAirSetters.forEach(Runnable::run);});
        }
        return Pair.of(0, null);
    }

    public static List<Direction> getCardinalsShuffle(RandomSource random) {
        Collections.shuffle(CARDINALS #if MC > MC_20_1 , random::nextInt #endif);
        return CARDINALS;
    }

    private static boolean checkBlockIsNonDisplacer(BlockState state) {
        return FlowingFluids.nonDisplacerTags.stream().anyMatch(state::is)
                || FlowingFluids.nonDisplacers.stream().anyMatch(state::is);
    }

    public static void displaceFluids(final Level level, final BlockPos pos, final BlockState state, final int flags, final LevelChunk levelChunk, final BlockState originalState) {
        // oof, this is a big one
        // try and order in most likely to least likely to avoid unnecessary checks
        // configs first
        if (!level.isClientSide()
                && FlowingFluids.config.enableMod
                && FlowingFluids.config.enableDisplacement
                && !FlowingFluids.isManeuveringFluids
                && !originalState.getFluidState().isEmpty()// assert that the original state is a fluid
                && originalState.getFluidState().getType() instanceof FlowingFluid flowSource
                && !state.isAir() // covers most block breaking updates
                && state.getFluidState().isEmpty()// not placing a waterlogged or fluid block
                && !((flags & 64) == 64) //Piston moved flag
                && !(state.getBlock() instanceof LiquidBlockContainer && originalState.getBlock() instanceof BucketPickup)
                && !checkBlockIsNonDisplacer(state) // check if the block is a displacer
               ) {
            // fluid block was replaced, lets try and displace the fluid
            FlowingFluids.isManeuveringFluids = true;


            try {
                // try spread to the side as much as possible
                int amountRemaining = originalState.getFluidState().getAmount();
                for (Direction direction : getCardinalsShuffle(level.getRandom())) {
                    BlockPos offset = pos.relative(direction);
                    BlockState offsetState = level.getBlockState(offset);

                    if (offsetState.getFluidState().getType() instanceof FlowingFluid) {
                        amountRemaining = addAmountToFluidAtPosWithRemainder(level, offset, flowSource, amountRemaining);
                        if (amountRemaining == 0) break;
                    } else if (offsetState.isAir()) {
                        level.setBlock(offset, originalState.getFluidState().createLegacyBlock(), 3);
                        amountRemaining = 0;
                        break;
                    }
                }
                if (amountRemaining > 0) {
                    // if we still have fluid left, try to displace upwards recursively
                    BlockPos.MutableBlockPos posTraversing = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
                    int height = levelChunk
                            #if MC > MC_21
                                .getMaxY();
                            #else
                                .getMaxBuildHeight();
                            #endif

                    while (amountRemaining > 0 && posTraversing.getY() < height) {
                        posTraversing.move(Direction.UP);
                        BlockState offsetState = level.getBlockState(posTraversing);
                        if (offsetState.getFluidState().getType() instanceof FlowingFluid) {
                            amountRemaining = addAmountToFluidAtPosWithRemainder(level, posTraversing, flowSource, amountRemaining);
                        } else if (offsetState.isAir()) {
                            level.setBlock(posTraversing, originalState.getFluidState().createLegacyBlock(), 3);
                            amountRemaining = 0;
                        } else {
                            break;
                        }
                    }
                }
            } finally {
                FlowingFluids.isManeuveringFluids = false;
            }
        }
    }

}
