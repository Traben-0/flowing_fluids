package traben.flowing_fluids;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.DimensionTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
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
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class FFFluidUtils {

    public static int seaLevel(LevelReader level) {
        // Dimension override
        int override = FlowingFluids.config.dimensionSeaLevelOverrides
                .getOrDefault(level.dimensionType().hashCode(), Integer.MIN_VALUE);
        if (override != Integer.MIN_VALUE) return override;

        // Default override
        int def = FlowingFluids.config.defaultSeaLevelOverride;
        if (def != Integer.MIN_VALUE) return def;

        // Vanilla
        return level.getSeaLevel();
    }

    public static @NotNull ResourceLocation res(String fullPath){
        //#if MC >= 12100
        return ResourceLocation.parse(fullPath);
        //#else
        //$$ return new ResourceLocation(fullPath);
        //#endif
    }

    public static @NotNull ResourceLocation res(String namespace, String path){
        //#if MC >= 12100
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        //#else
        //$$ return new ResourceLocation(namespace, path);
        //#endif
    }

    public static boolean dimensionEvaporatesWaterVanilla(LevelReader level) {
        //#if MC >= 12111
        //$$ return level.dimensionType().attributes().contains(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES);
        //#else
        return level.dimensionType().ultraWarm();
        //#endif
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
                bucketPickup.pickupBlock(
                        //#if MC > 12001
                        null,
                        //#endif
                        levelAccessor, pos, blockState);
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
            bucketPickup.pickupBlock(
                    //#if MC > 12001
                    null,
                    //#endif
                    levelAccessor, pos, blockState);
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
        return placeConnectedFluidAmountAndPlaceActionAndNotify(levelAccessor, blockPos, amountToPlace, fluid, depth, doUp, doDown, false);
    }

    public static Pair<Integer, Runnable> placeConnectedFluidAmountAndPlaceActionAndNotify(final LevelAccessor levelAccessor, final BlockPos blockPos, final int amountToPlace, final FlowingFluid fluid, int depth, boolean doUp, boolean doDown, boolean notifyListeners) {
        var originalState = levelAccessor.getFluidState(blockPos);
        int originalAmount = originalState.getAmount();
        if (originalState.getType().isSame(fluid) && originalAmount > 0) {

            //check for quick exit
            if (originalAmount + amountToPlace <= 8) {
                return Pair.of(0,()->{
                    FFFluidUtils.setFluidStateAtPosToNewAmount(levelAccessor, blockPos, fluid, originalAmount + amountToPlace);
                    if (notifyListeners) notifyListeners(levelAccessor, blockPos, Direction.DOWN, fluid, false);
                });
            }
            return placeConnectedFluidAmountAndPlaceActionCanIgnoreStart(levelAccessor, blockPos, amountToPlace, fluid, depth, doUp, doDown, null, notifyListeners);
        }
        return Pair.of(amountToPlace, null);
    }

    public static @NotNull Pair<Integer, Runnable> placeConnectedFluidAmountAndPlaceActionCanIgnoreStart(
            LevelAccessor levelAccessor, BlockPos blockPos, int amountToPlace, FlowingFluid fluid, int depth, boolean doUp, boolean doDown, @Nullable List<BlockPos> ignore, boolean notifyListeners) {

        List<BlockPos> toCheck = new ArrayList<>(); // for O(1) ordered index access
        Set<BlockPos> seen = new HashSet<>(); // for O(1) containment checks
        List<Direction> dirs = new ArrayList<>();

        toCheck.add(blockPos);
        if (ignore != null) seen.addAll(ignore);

        final Consumer<BlockPos> addSurroundingPositions = blockPos1 -> {
            for (Direction direction : getCardinalsShuffle(levelAccessor.getRandom())) {
                BlockPos offset = blockPos1.relative(direction);
                if (seen.add(offset)) {
                    toCheck.add(offset);
                    if (notifyListeners) dirs.add(direction);
                }
            }
            if (doUp) {
                // do these last just as preference
                BlockPos up = blockPos1.above();
                if (seen.add(up)) {
                    toCheck.add(up);
                    if (notifyListeners) dirs.add(Direction.UP);
                }
            }
            if (doDown) {
                BlockPos down = blockPos1.below();
                if (seen.add(down)) {
                    toCheck.add(down);
                    if (notifyListeners) dirs.add(Direction.DOWN);
                }
            }

        };
        addSurroundingPositions.accept(blockPos);

        List<Runnable> onSuccessPlacers = new ArrayList<>();
        int amountLeftToPlace = amountToPlace;
        int i = ignore != null && ignore.contains(blockPos) ? 1 : 0;
        for (; i < toCheck.size(); i++) {
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

        int finalI = i;
        return Pair.of(amountLeftToPlace, () -> {
            onSuccessPlacers.forEach(Runnable::run);
            if (notifyListeners) {
                // This technically differs from the flowing fluids tick notifier as it notifies of the flow To this pos
                // rather than the flow From this pos, but for our purposes this is fine
                IntStream.range(0, Math.min(finalI, toCheck.size() - 1))
                        .forEach(j ->  notifyListeners(levelAccessor, toCheck.get(j), dirs.get(j), fluid, false));
            }
        });
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
        if (matchesAny(FlowingFluids.nonDisplacers.get(fluid), state::is)) return true;
        if (matchesAny(FlowingFluids.nonDisplacerTags.get(fluid), state::is)) return true;

        //#if MC > 1.20.1
        String blockId = BuiltInRegistries.BLOCK.wrapAsHolder(state.getBlock()).getRegisteredName();
        //#else
        //$$ String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        //#endif
        if (matchesAny(FlowingFluids.nonDisplacerIds.get(fluid), blockId::equals)) return true;

        return matchesAny(FlowingFluids.nonDisplacerTagIds.get(fluid),
                tag -> state.getTags().anyMatch(it -> it.location().toString().equals(tag)));
    }

    private static <T> boolean matchesAny(Iterable<T> items, Predicate<T> condition) {
        if (items == null) return false;
        for (T item : items) {
            if (condition.test(item)) return true;
        }
        return false;
    }

    public static void displaceFluids(final Level level, final BlockPos pos, final BlockState state, final int flags, final LevelChunk levelChunk, final BlockState originalState) {
        // oof, this check is a big one, but important! as displacement is costly but necessary
        // try and order in most likely to least likely to avoid unnecessary checks
        if (level.isClientSide() || !FlowingFluids.config.enableMod || FlowingFluids.isManeuveringFluids) return;

        boolean isPistonMove = FlowingFluids.config.enablePistonPushing && (flags & 18) == 18; // flag 18 is set by piston destruction
        if ((FlowingFluids.config.enableDisplacement || isPistonMove)
                && !originalState.getFluidState().isEmpty() // assert that the original state is a fluid
                && originalState.getFluidState().getType() instanceof FlowingFluid flowSource
                && FlowingFluids.config.isFluidAllowed(flowSource) // check if the fluid is not in the ignored list
                && (isPistonMove || !state.isAir()) // covers most block breaking updates
                && state.getFluidState().isEmpty() // not placing a waterlogged or fluid block
                && (flags & 64) != 64 // Piston moved flag
                && !(state.getBlock() instanceof LiquidBlockContainer && originalState.getBlock() instanceof BucketPickup)
                && !checkBlockIsNonDisplacer(flowSource, state) // check if the block is a displacer
               ) {
            // fluid block was replaced, lets try and displace the fluid
            FlowingFluids.isManeuveringFluids = true;

            // we will displace upwards first with a simpler algorithm to any height simply as a preference
            // upwards recursively until it hits air or non-fluid),
            // then we more thoroughly search through connected fluid blocks to find further away blocks to place into

            try {
                int amountRemaining = originalState.getFluidState().getAmount();

                boolean didUp = false;
                boolean piston = isPistonMove && FlowingFluids.lastPistonMoveDirection != null;
                // if piston has caused this try to displace via it's direction first
                if (piston) {
                    if (FlowingFluids.lastPistonMoveDirection == Direction.UP) {
                        amountRemaining = displaceUpFar(level, pos, levelChunk, originalState, flowSource, amountRemaining);
                        didUp = true;
                    }
                    if (amountRemaining > 0) {
                        amountRemaining = displaceDeepSearch(level, pos, flowSource, amountRemaining,
                                FlowingFluids.lastPistonMoveDirection, false);
                    }
                }

                if (FlowingFluids.config.enableDisplacement) {
                    // normal displacements, with some performance consideration to skip piston direction if we did that first
                    if (!didUp && amountRemaining > 0) {
                        amountRemaining = displaceUpFar(level, pos, levelChunk, originalState, flowSource, amountRemaining);
                    }
                    if (amountRemaining > 0) {
                        amountRemaining = displaceDeepSearch(level, pos, flowSource, amountRemaining, FlowingFluids.lastPistonMoveDirection, piston);
                    }
                }

                // play sounds
                if (FlowingFluids.config.displacementSounds.allow(piston)) {
                    boolean lava = flowSource.isSame(Fluids.LAVA);
                    level.playSound(null, pos,
                            amountRemaining > 0
                                    ? lava ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL
                                    : lava ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY,
                            SoundSource.BLOCKS, 1.0F, 1.0F
                    );
                }
            } finally {
                FlowingFluids.isManeuveringFluids = false;
            }
        }
    }

    private static int displaceDeepSearch(Level level, BlockPos pos, FlowingFluid flowSource, int amountRemaining,
                                          @Nullable Direction pistonDirection, boolean skipPistonDirection) {
        // we have already considered the priority direct neighbors and recursively upward
        // lets now search more thoroughly as we really don't want to lose fluid

        var positionsToIgnore = new ArrayList<BlockPos>();
        positionsToIgnore.add(pos);

        if (pistonDirection != null) {
            if (skipPistonDirection) {
                positionsToIgnore.add(pos.relative(pistonDirection));
            } else {
                for (Direction direction : Direction.values()) {
                    if (direction != pistonDirection) {
                        positionsToIgnore.add(pos.relative(direction));
                    }
                }
            }
        }

        var result = placeConnectedFluidAmountAndPlaceActionCanIgnoreStart(level, pos,
                amountRemaining, flowSource, (int) (200 * FlowingFluids.config.displacementDepthMultiplier), true, true, positionsToIgnore, false);

        // finalize placement if we found anywhere to put it and break if we are done
        if (result.first() < amountRemaining) {
            result.second().run();
            amountRemaining = result.first(); //last line of method its redundant
        }
        return amountRemaining;
    }

    private static int displaceUpFar(Level level, BlockPos pos, LevelChunk levelChunk, BlockState originalState, FlowingFluid flowSource, int amountRemaining) {
        BlockPos.MutableBlockPos posTraversing = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        int height = Math.min(levelChunk
                //#if MC > 12100
                .getMaxY()
                //#else
                //$$ .getMaxBuildHeight()
                //#endif
            , pos.getY() + (int) (48 * FlowingFluids.config.displacementDepthMultiplier));

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
        return amountRemaining;
    }

    public static boolean matchInfiniteBiomes(Holder<Biome> biome){
        return FlowingFluids.infiniteBiomeTags.stream().anyMatch(biome::is)
                || FlowingFluids.infiniteBiomes.stream().anyMatch(biome::is);
    }


    private static long lastFlowSoundTime = 0;

    public static void playFlowSound(Level level, BlockPos blockPos, Fluid fluid) {
        if (FlowingFluids.config.flowSoundChance == 0f) return;
        if (level.getRandom().nextFloat() > FlowingFluids.config.flowSoundChance) return;

        long currentTime = level.getGameTime();
        if (lastFlowSoundTime + 60 > currentTime) return;

        boolean lava = fluid.isSame(Fluids.LAVA);
        level.playSound(null, blockPos, lava ? SoundEvents.LAVA_AMBIENT : SoundEvents.WATER_AMBIENT,
                SoundSource.BLOCKS, 1.0F, 1.0F
        );

        lastFlowSoundTime = currentTime;
    }

    public static void notifyListeners(LevelAccessor level, BlockPos pos, Direction direction, Fluid fluid, boolean downAlso) {
        if (!(level instanceof FFFlowListenerLevel)) return;
        Map<BlockPos, Set<BlockPos>> listeners = ((FFFlowListenerLevel) level).ff$getFlowListenerPositions();
        Set<BlockPos> positions = listeners.get(pos);
        if (positions != null) {
            for (BlockPos listenerPos : positions) {
                if (level.getBlockEntity(pos) instanceof IFFFlowListener listener) {
                    listener.ff$acceptRecentFlow(listenerPos, direction, fluid, downAlso);
                }
            }
        }
    }
}
