package traben.flowing_fluids;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
        return Direction.Plane.HORIZONTAL.shuffledCopy(random);
    }



    private static boolean checkBlockIsNonDisplacer(Fluid fluid, BlockState state) {
        return FlowingFluids.nonDisplacerTags.stream().anyMatch(pair ->
                        (pair.first() == Fluids.EMPTY || pair.first().isSame(fluid)) && state.is(pair.second()))
                || FlowingFluids.nonDisplacers.stream().anyMatch(pair ->
                        (pair.first() == Fluids.EMPTY || pair.first().isSame(fluid)) && state.is(pair.second()));
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
                && FlowingFluids.config.isFluidAllowed(flowSource) // check if the fluid is not in the ignored list
                && !state.isAir() // covers most block breaking updates
                && state.getFluidState().isEmpty()// not placing a waterlogged or fluid block
                && !((flags & 64) == 64) //Piston moved flag
                && !(state.getBlock() instanceof LiquidBlockContainer && originalState.getBlock() instanceof BucketPickup)
                && !checkBlockIsNonDisplacer(flowSource, state) // check if the block is a displacer
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

    public static boolean matchInfiniteBiomes(Holder<Biome> biome){
        return FlowingFluids.infiniteBiomeTags.stream().anyMatch(biome::is)
                || FlowingFluids.infiniteBiomes.stream().anyMatch(biome::is);
    }

    /**
     * Public helper method to check if water at a position is connected to an infinite source.
     * Used for debugging and display purposes.
     * This checks both direct connections and recursive connections through water.
     * 
     * @param level The level to check in
     * @param pos The position to check
     * @param sourceY The Y level to check connections at
     * @return true if water at this position is connected to an infinite source
     */
    public static boolean isWaterConnectedToInfiniteSource(Level level, BlockPos pos, int sourceY) {
        return isWaterConnectedToInfiniteSource(level, pos, sourceY, false);
    }
    
    /**
     * Internal version with fast-path option for performance-critical flow operations.
     * 
     * @param level The level to check in
     * @param pos The position to check
     * @param sourceY The Y level to check connections at
     * @param fastPath If true, uses reduced recursion depth and check limits for better performance
     * @return true if water at this position is connected to an infinite source
     */
    public static boolean isWaterConnectedToInfiniteSource(Level level, BlockPos pos, int sourceY, boolean fastPath) {
        if (!FlowingFluids.config.infiniteSourceEqualizeToFullHeight) {
            return false;
        }
        
        var fluidState = level.getFluidState(pos);
        if (!fluidState.is(net.minecraft.tags.FluidTags.WATER)) {
            return false;
        }
        
        // Check if at sea level OR 1 block below sea level (for trenches)
        boolean atSeaLevelOrJustBelow = pos.getY() == level.getSeaLevel() || pos.getY() == level.getSeaLevel() - 1;
        
        if (atSeaLevelOrJustBelow) {
            boolean withinInfBiomeHeights = FlowingFluids.config.fastBiomeRefillAtSeaLevelOnly
                    ? level.getSeaLevel() == pos.getY() || level.getSeaLevel() - 1 == pos.getY()
                    : pos.getY() >= level.getSeaLevel() - 1 && pos.getY() <= level.getSeaLevel() && pos.getY() > 0;
            
            if (withinInfBiomeHeights
                    && level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos) > 0) {
                // Check if water is in infinite biome - but also verify it's part of a large enough body
                // Small isolated pools in infinite biomes shouldn't be infinite if walled off
                BlockPos seaLevelPos = new BlockPos(pos.getX(), level.getSeaLevel(), pos.getZ());
                boolean inInfiniteBiome = matchInfiniteBiomes(level.getBiome(seaLevelPos));
                
                // Check large water body - only if we're at sea level
                // Large water bodies are only infinite if they're connected to an infinite biome
                // This prevents walled-off areas from being considered infinite
                if (pos.getY() == level.getSeaLevel()) {
                    // Check if it's part of a large water body
                    // Use reduced checks for fast path (flow operations) to improve performance
                    int maxChecks = fastPath ? 100 : 200;
                    java.util.Set<BlockPos> bodyChecked = new java.util.HashSet<>();
                    boolean isLargeBody = isPartOfLargeWaterBody(level, pos, bodyChecked, maxChecks);
                    
                    if (isLargeBody && inInfiniteBiome) {
                        // Large body in infinite biome - definitely infinite
                        return true;
                    }
                    
                    // Even if not large, check if it's connected to a large body that's in infinite biome
                    if (isPartOfLargeWaterBodyConnectedToInfinite(level, pos, new java.util.HashSet<>(), maxChecks)) {
                        return true;
                    }
                } else {
                    // We're 1 block below - check multiple things:
                    // 1. Check if water above us is part of large body
                    BlockPos above = pos.above();
                    if (above.getY() == level.getSeaLevel()) {
                        var aboveFluid = level.getFluidState(above);
                        if (aboveFluid.is(net.minecraft.tags.FluidTags.WATER) && aboveFluid.getAmount() >= 1) {
                            int maxChecks = fastPath ? 100 : 200;
                            if (isPartOfLargeWaterBodyConnectedToInfinite(level, above, new java.util.HashSet<>(), maxChecks)) {
                                return true;
                            }
                        }
                    }
                    
                    // 2. Also check adjacent positions at sea level - trace horizontally to find lake
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockPos adjacentAtSeaLevel = pos.relative(direction).atY(level.getSeaLevel());
                        var seaLevelFluid = level.getFluidState(adjacentAtSeaLevel);
                        if (seaLevelFluid.is(net.minecraft.tags.FluidTags.WATER) && seaLevelFluid.getAmount() >= 1) {
                            int maxChecks = fastPath ? 100 : 200;
                            if (isPartOfLargeWaterBodyConnectedToInfinite(level, adjacentAtSeaLevel, new java.util.HashSet<>(), maxChecks)) {
                                return true;
                            }
                        }
                    }
                    
                    // 3. Check if this Y:62 position itself is part of a large water body at this Y level
                    // This handles cases where the entire lake is at Y:62 (below sea level)
                    // But only if it's connected to an infinite biome
                    int maxChecks = fastPath ? 100 : 200;
                    if (isLargeWaterBodyAtYLevelConnectedToInfinite(level, pos, pos.getY(), new java.util.HashSet<>(), maxChecks)) {
                        return true;
                    }
                }
            }
        }
        
        // Also check recursive connection - trace back through water to find infinite source
        // Use reduced depth for fast path (flow operations) to improve performance
        int maxDepth = fastPath ? 32 : 64;
        return isWaterConnectedToInfiniteSourceRecursive(level, pos, sourceY, new java.util.HashSet<>(), maxDepth);
    }

    /**
     * Recursively checks if water is connected to an infinite source through connected water blocks.
     */
    public static boolean isWaterConnectedToInfiniteSourceRecursive(Level level, BlockPos pos, int sourceY,
                                                                      java.util.Set<BlockPos> checked, int maxDepth) {
        if (maxDepth <= 0 || checked.contains(pos)) {
            return false;
        }
        checked.add(pos);
        
        // Only check at same Y level, sea level, or 1 block below sea level
        if (pos.getY() != sourceY && pos.getY() != level.getSeaLevel() && pos.getY() != level.getSeaLevel() - 1) {
            return false;
        }
        
        var fluidState = level.getFluidState(pos);
        if (!fluidState.is(net.minecraft.tags.FluidTags.WATER) || fluidState.getAmount() < 1) {
            return false;
        }
        
        // Check if this position is at sea level and an infinite source
        if (pos.getY() == level.getSeaLevel()) {
            boolean withinInfBiomeHeights = FlowingFluids.config.fastBiomeRefillAtSeaLevelOnly
                    ? level.getSeaLevel() == pos.getY() || level.getSeaLevel() - 1 == pos.getY()
                    : level.getSeaLevel() == pos.getY() && pos.getY() > 0;
            
            if (withinInfBiomeHeights
                    && level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos) > 0) {
                boolean inInfiniteBiome = matchInfiniteBiomes(level.getBiome(pos));
                
                // Only consider infinite if it's both in infinite biome AND part of large enough body
                // This prevents small walled-off pools in infinite biomes from being infinite
                if (inInfiniteBiome) {
                    java.util.Set<BlockPos> bodyChecked = new java.util.HashSet<>();
                    if (isPartOfLargeWaterBody(level, pos, bodyChecked, 200)) {
                        // Large body in infinite biome - definitely infinite
                        return true;
                    }
                }
                
                // Also check if connected to large body that's in infinite biome
                if (isPartOfLargeWaterBodyConnectedToInfinite(level, pos, new java.util.HashSet<>(), 200)) {
                    return true;
                }
            }
        }
        
        // Also check if we're 1 block below sea level and the biome above us is infinite
        // This allows tracing through water at Y:62 to find river/ocean connections
        if (pos.getY() == level.getSeaLevel() - 1) {
            boolean withinInfBiomeHeights = FlowingFluids.config.fastBiomeRefillAtSeaLevelOnly
                    ? level.getSeaLevel() == pos.getY() || level.getSeaLevel() - 1 == pos.getY()
                    : pos.getY() >= level.getSeaLevel() - 1 && pos.getY() <= level.getSeaLevel() && pos.getY() > 0;
            
            if (withinInfBiomeHeights
                    && level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos) > 0) {
                // Check biome at sea level position above us
                // But only if it's part of a large enough body (prevents small walled-off pools)
                BlockPos seaLevelPos = pos.atY(level.getSeaLevel());
                boolean inInfiniteBiome = matchInfiniteBiomes(level.getBiome(seaLevelPos));
                
                if (inInfiniteBiome) {
                    java.util.Set<BlockPos> bodyChecked = new java.util.HashSet<>();
                    if (isPartOfLargeWaterBody(level, seaLevelPos, bodyChecked, 200)) {
                        // Large body in infinite biome - definitely infinite
                        return true;
                    }
                }
                
                // Also check if this Y:62 position is part of a large water body at this Y level
                // This handles cases where the entire lake is at Y:62 (below sea level)
                // But only if it's connected to an infinite biome
                if (isLargeWaterBodyAtYLevelConnectedToInfinite(level, pos, pos.getY(), new java.util.HashSet<>(), 200)) {
                    return true;
                }
            }
        }
        
        // If we're 1 block below sea level, also check adjacent positions at sea level
        // This helps find connections when the trench is below but adjacent to sea level water
        if (pos.getY() == level.getSeaLevel() - 1) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos adjacentAtSeaLevel = pos.relative(direction).atY(level.getSeaLevel());
                if (!checked.contains(adjacentAtSeaLevel)) {
                    var seaLevelFluid = level.getFluidState(adjacentAtSeaLevel);
                    if (seaLevelFluid.is(net.minecraft.tags.FluidTags.WATER) && seaLevelFluid.getAmount() >= 1) {
                        // Check if this sea level water is part of infinite source
                        if (level.getBrightness(net.minecraft.world.level.LightLayer.SKY, adjacentAtSeaLevel) > 0) {
                            boolean inInfiniteBiome = matchInfiniteBiomes(level.getBiome(adjacentAtSeaLevel));
                            
                            // Only consider infinite if it's both in infinite biome AND part of large enough body
                            if (inInfiniteBiome) {
                                java.util.Set<BlockPos> bodyChecked = new java.util.HashSet<>();
                                if (isPartOfLargeWaterBody(level, adjacentAtSeaLevel, bodyChecked, 200)) {
                                    // Large body in infinite biome - definitely infinite
                                    return true;
                                }
                            }
                            
                            if (isPartOfLargeWaterBodyConnectedToInfinite(level, adjacentAtSeaLevel, new java.util.HashSet<>(), 200)) {
                                return true;
                            }
                        }
                        // Also recursively check this sea level position
                        if (isWaterConnectedToInfiniteSourceRecursive(level, adjacentAtSeaLevel, sourceY, checked, maxDepth - 1)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        // Check adjacent water
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacent = pos.relative(direction);
            // Allow checking at same Y, sea level, or 1 block below sea level
            if ((adjacent.getY() == sourceY || adjacent.getY() == level.getSeaLevel() || adjacent.getY() == level.getSeaLevel() - 1)
                    && isWaterConnectedToInfiniteSourceRecursive(level, adjacent, sourceY, checked, maxDepth - 1)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Checks if a position is part of a large connected water body (like a lake).
     * This is a public version of the check for debugging purposes.
     * 
     * @param level The level to check in
     * @param pos The position to check
     * @param checked Set of positions already checked
     * @param maxChecks Maximum number of blocks to check (increased to 200 for better coverage)
     * @return true if this position is part of a large water body (10+ connected blocks found)
     */
    public static boolean isPartOfLargeWaterBody(Level level, BlockPos pos, java.util.Set<BlockPos> checked, int maxChecks) {
        int threshold = FlowingFluids.config.largeWaterBodyThreshold;
        if (checked.contains(pos)) {
            return checked.size() >= threshold;
        }
        
        if (maxChecks <= 0) {
            return checked.size() >= threshold;
        }
        
        if (pos.getY() != level.getSeaLevel()) {
            return checked.size() >= threshold;
        }
        
        var fluidState = level.getFluidState(pos);
        if (!fluidState.is(net.minecraft.tags.FluidTags.WATER) || fluidState.getAmount() < 1) {
            return checked.size() >= threshold;
        }
        
        checked.add(pos);
        
        if (checked.size() >= threshold) {
            return true;
        }
        
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacent = pos.relative(direction);
            if (adjacent.getY() == level.getSeaLevel()) {
                if (isPartOfLargeWaterBody(level, adjacent, checked, maxChecks - 1)) {
                    return true;
                }
                if (checked.size() >= threshold) {
                    return true;
                }
            }
        }
        
        return checked.size() >= threshold;
    }
    
    /**
     * Checks if a position is part of a large connected water body at a specific Y level.
     * Similar to isPartOfLargeWaterBody but works at any Y level (not just sea level).
     * 
     * @param level The level to check in
     * @param pos The position to check
     * @param targetY The Y level to check at (must match pos.getY())
     * @param checked Set of positions already checked
     * @param maxChecks Maximum number of blocks to check
     * @return true if this position is part of a large water body (10+ connected blocks found)
     */
    public static boolean isLargeWaterBodyAtYLevel(Level level, BlockPos pos, int targetY,
                                                     java.util.Set<BlockPos> checked, int maxChecks) {
        int threshold = FlowingFluids.config.largeWaterBodyThreshold;
        if (checked.contains(pos)) {
            return checked.size() >= threshold;
        }
        
        if (maxChecks <= 0) {
            return checked.size() >= threshold;
        }
        
        if (pos.getY() != targetY) {
            return checked.size() >= threshold;
        }
        
        var fluidState = level.getFluidState(pos);
        if (!fluidState.is(net.minecraft.tags.FluidTags.WATER) || fluidState.getAmount() < 1) {
            return checked.size() >= threshold;
        }
        
        checked.add(pos);
        
        if (checked.size() >= threshold) {
            return true;
        }
        
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacent = pos.relative(direction);
            if (adjacent.getY() == targetY) {
                if (isLargeWaterBodyAtYLevel(level, adjacent, targetY, checked, maxChecks - 1)) {
                    return true;
                }
                if (checked.size() >= threshold) {
                    return true;
                }
            }
        }
        
        return checked.size() >= threshold;
    }
    
    /**
     * Checks if a position is part of a large water body that's connected to an infinite biome.
     * A large water body is only considered infinite if it's both large enough AND has at least
     * one block that's in an infinite biome (ocean/river). This prevents walled-off areas from being infinite.
     * 
     * @param level The level to check in
     * @param pos The position to check
     * @param checked Set of positions already checked (not used here, kept for signature compatibility)
     * @param maxChecks Maximum number of blocks to check
     * @return true if this position is part of a large water body connected to infinite biome
     */
    private static boolean isPartOfLargeWaterBodyConnectedToInfinite(Level level, BlockPos pos, 
                                                                      java.util.Set<BlockPos> checked, int maxChecks) {
        java.util.Set<BlockPos> bodyChecked = new java.util.HashSet<>();
        int threshold = FlowingFluids.config.largeWaterBodyThreshold;
        
        // First, check if it's large enough (optimized: fail fast if too small)
        if (!isPartOfLargeWaterBody(level, pos, bodyChecked, maxChecks)) {
            return false;
        }
        
        // Now verify that at least one block in this large body is in an infinite biome
        // Check biome as we iterate (early exit once found)
        for (BlockPos bodyPos : bodyChecked) {
            if (level.getBrightness(net.minecraft.world.level.LightLayer.SKY, bodyPos) > 0) {
                if (matchInfiniteBiomes(level.getBiome(bodyPos))) {
                    return true; // Early exit - found infinite biome
                }
            }
        }
        
        // If no block in the body is in an infinite biome, it's not infinite (e.g., walled off)
        return false;
    }
    
    /**
     * Checks if a position is part of a large water body at a specific Y level that's connected to infinite biome.
     * Similar to isPartOfLargeWaterBodyConnectedToInfinite but works at any Y level.
     * Checks if the sea level position above each body block is in an infinite biome.
     */
    private static boolean isLargeWaterBodyAtYLevelConnectedToInfinite(Level level, BlockPos pos, int targetY,
                                                                          java.util.Set<BlockPos> checked, int maxChecks) {
        java.util.Set<BlockPos> bodyChecked = new java.util.HashSet<>();
        
        // First, check if it's large enough
        if (!isLargeWaterBodyAtYLevel(level, pos, targetY, bodyChecked, maxChecks)) {
            return false;
        }
        
        // Now verify that at least one block in this large body (at sea level above) is in an infinite biome
        // Check the sea level position above each body block
        for (BlockPos bodyPos : bodyChecked) {
            BlockPos seaLevelPos = bodyPos.atY(level.getSeaLevel());
            if (level.getBrightness(net.minecraft.world.level.LightLayer.SKY, seaLevelPos) > 0) {
                if (matchInfiniteBiomes(level.getBiome(seaLevelPos))) {
                    return true;
                }
            }
        }
        
        // If no sea level position above the body is in an infinite biome, it's not infinite
        return false;
    }
}
