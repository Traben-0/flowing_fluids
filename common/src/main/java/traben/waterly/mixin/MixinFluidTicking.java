package traben.waterly.mixin;


import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.*;
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
import traben.waterly.CheckedDirFromPosKey;
import traben.waterly.FluidGetterByAmount;
import traben.waterly.Waterly;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.ToIntFunction;


@Mixin(FlowingFluid.class)
public abstract class MixinFluidTicking extends Fluid implements FluidGetterByAmount {


    @Shadow
    private static short getCacheKey(final BlockPos blockPos, final BlockPos blockPos2) {
        System.out.println("MIXIN ERROR IN WATERLY");
        return 0;
    }

    @Shadow
    public abstract FluidState getSource(final boolean bl);

    @Shadow
    public abstract FluidState getFlowing(final int i, final boolean bl);

    @Shadow
    public abstract boolean canPassThroughWall(final Direction direction, final BlockGetter blockGetter, final BlockPos blockPos, final BlockState blockState, final BlockPos blockPos2, final BlockState blockState2);

    @Shadow
    public abstract boolean canHoldFluid(final BlockGetter blockGetter, final BlockPos blockPos, final BlockState blockState, final Fluid fluid);

    @Shadow
    protected abstract int getDropOff(final LevelReader levelReader);

    @Shadow
    protected abstract void spreadTo(final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState blockState, final Direction direction, final FluidState fluidState);

    @Shadow
    protected abstract int getSlopeFindDistance(final LevelReader levelReader);

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    private void waterly$tickMixin(final Level level, final BlockPos blockPos, final FluidState fluidState, final CallbackInfo ci) {
        if (true) {
            //cancel the original tick
            ci.cancel();

            //just in case, shouldn't be needed but who knows what mods do these days
            if (level.isClientSide()) return;

            BlockState thisState = level.getBlockState(blockPos);
            BlockPos posDown = blockPos.below();

            //check if we can flow down and if so how much fluid remains out of the 8 total possible
            int remainingAmount = waterly$checkAndFlowDown(level, blockPos, fluidState, thisState, posDown,
                    level.getBlockState(posDown), fluidState.getAmount());

            //if there is remaining amount still, the block below is full, or we couldn't flow down

            //if there is still water left, flow to the sides only if it is above the drop-off amount
            //the drop-off amount is teh vanilla value determining how much each block of flow reduces the amount
            //this ties in nicely with a sort of surface tension effect
            if (remainingAmount > getDropOff(level)) {//drop off is 1 for water, 2 for lava in the overworld
                waterly$flowToSides(level, blockPos, fluidState, remainingAmount, thisState, remainingAmount);
            } else if (remainingAmount > 0) {
                //if the remaining amount is less than the drop-off amount, we can still flow to the sides but only if
                //we find a nearby ledge to flow towards, as we want this water to settle when on flat ground
                Direction dir = waterly$getLowestSpreadableLookingFor4BlockDrops(level, blockPos, fluidState, remainingAmount, true);
                //dir is null if no spreadable block was found
                if (dir != null) {
                    var pos = blockPos.relative(dir);
                    waterly$setOrRemoveWaterAmountAt(level, blockPos, 0, thisState, dir);
                    waterly$spreadTo2(level, pos, level.getBlockState(pos), dir, remainingAmount);
                }
            }
        }
    }

    @Unique
    private void waterly$flowToSides(final Level level, final BlockPos blockPos, final FluidState fluidState, int amount, final BlockState thisState, final int originalAmount) {
        //get a valid direction to move into or null if no spreadable block was found
        Direction dir = waterly$getLowestSpreadableLookingFor4BlockDrops(level, blockPos, fluidState, amount, false);
        if (dir == null) return;

        var posDir = blockPos.relative(dir);

        //if we are a water-loggable block
        //need to check both container and pickup as there are some odd collisions, including the liquid blocks themselves
        if (thisState.getBlock() instanceof LiquidBlockContainer
                && thisState.getBlock() instanceof BucketPickup getWater) {
            //just totally empty all water-loggables
            getWater.pickupBlock(null, level, blockPos, thisState);
            //this may lose some water, but it's the easiest choice for now, and water-loggables aren't "full" anyway
            waterly$spreadTo2(level, posDir, level.getBlockState(posDir), dir, amount);
        } else {
            //this amount is already confirmed to be less than {amount}
            int destFluidAmount = level.getFluidState(posDir).getAmount();
            int difference = amount - destFluidAmount;

            //since the remainder is left in the source block, we can return if the difference is 1, or less (less is impossible but also a safety check)
            if (difference <= 1) return;

            //calculate the amount that would level both liquids
            int averageLevel = destFluidAmount + difference / 2;

            //if the difference is odd, we need to add 1 to the 'from' amount
            boolean hasRemainder = (difference % 2 != 0);
            int fromAmount = hasRemainder ? averageLevel + 1 : averageLevel;

            //amounts are always changed by this point, so we can safely set the source block to the new amount
            waterly$setOrRemoveWaterAmountAt(level, blockPos, fromAmount, thisState, dir.getOpposite());
            waterly$spreadTo2(level, posDir, level.getBlockState(posDir), dir, averageLevel);
        }
    }

    @Unique
    private int waterly$checkAndFlowDown(final Level level, final BlockPos blockPos, final FluidState fluidState, final BlockState thisState, final BlockPos posDown, final BlockState stateDown, int amount) {
        var downFState = level.getFluidState(posDown);
        //check and then handle if we can flow down
        if (waterly$canSpreadTo(fluidState.getType(), fluidState.getAmount(), level, blockPos, thisState,
                Direction.DOWN, posDown, stateDown, downFState)) {

            //handle other liquid vanilla collisions by causing a flow
            if (!downFState.isEmpty() && !downFState.getType().isSame(fluidState.getType())) {
                //send like vanilla flow to perform fluid collision
                //only use 1 for the amount, as we are only checking the collision behaviour
                //example: lava flowing down onto water creates stone in this case
                waterly$setOrRemoveWaterAmountAt(level, blockPos, amount - 1, thisState, Direction.DOWN);
                waterly$spreadTo2(level, posDown, stateDown, Direction.DOWN, 1);
                return amount - 1;
            } else {
                //flow into lower space
                int fluidDownAmount = downFState.getAmount();
                int amountDestCanAccept = Math.min(8 - fluidDownAmount, amount);
                //can fit some liquid
                if (amountDestCanAccept > 0) {
                    int destNewAmount = fluidDownAmount + amountDestCanAccept;
                    int sourceNewAmount = amount - amountDestCanAccept;
                    //set both amounts
                    waterly$setOrRemoveWaterAmountAt(level, blockPos, sourceNewAmount, thisState, Direction.DOWN);
                    waterly$spreadTo2(level, posDown, stateDown, Direction.DOWN, destNewAmount);
                    return sourceNewAmount;
                }
            }
        }
        //return the remaining amount of the source liquid
        return amount;
    }

    @Unique
    private void waterly$setOrRemoveWaterAmountAt(final Level level, final BlockPos blockPos, final int amount, final BlockState thisState, Direction direction) {
        if (amount > 0) {
            waterly$spreadTo2(level, blockPos, thisState, direction, amount);
        } else {
            waterly$removeWater(level, blockPos, thisState);
        }
    }


    @Inject(method = "getNewLiquid", at = @At(value = "HEAD"), cancellable = true)
    private void waterly$validateLiquidMixin(final Level level, final BlockPos blockPos, final BlockState blockState, final CallbackInfoReturnable<FluidState> cir) {
        if (true) {
            cir.setReturnValue(waterly$getFluidStateOfAmount(level.getFluidState(blockPos).getAmount()));
        }
    }

    @Unique
    private @Nullable Direction waterly$getLowestSpreadableLookingFor4BlockDrops(
            Level level, BlockPos blockPos, FluidState fluidState, int amount, boolean requiresSlope) {

        //use simpler direction choice if fast mode is enabled
        if (Waterly.fastmode) {
            return requiresSlope
                    ? waterly$getFastLowestSpreadableEdge(level, blockPos, fluidState, amount)
                    : waterly$getFastLowestSpreadable(level, blockPos, fluidState, amount);
        }

        //state cache for each position
        Short2ObjectMap<Pair<BlockState, FluidState>> statesAtPos = new Short2ObjectOpenHashMap<>();

        //get the cardinal directions that are valid flow locations sorted by the amount of fluid in them,
        //ties are randomly sorted by initial shuffle
        List<Direction> directionsCanSpreadToSortedByAmount = Waterly.getCardinalsShuffle().stream()
                .sorted(Comparator.comparingInt((dir1) -> level.getFluidState(blockPos.relative(dir1)).getAmount()))
                .filter(dir -> {
                    BlockPos posDir = blockPos.relative(dir);
                    short key = getCacheKey(blockPos, posDir);
                    var statesDir = waterly$getSetPosCache(key, level, statesAtPos, posDir);
                    BlockState stateDir = statesDir.getFirst();
                    var fluidStateDir = statesDir.getSecond();
                    return waterly$canSpreadToOptionallySameOrEmpty(fluidState.getType(), amount, level, blockPos,
                            level.getBlockState(blockPos), dir, posDir, stateDir, fluidStateDir, requiresSlope);
                })
                .toList();

        //early escape if no valid neighbours to spread too
        if (directionsCanSpreadToSortedByAmount.isEmpty()) return null;

        //perform a deep search for the best direction to spread to with the nearest slope
        Direction spreadDirection = waterly$getValidDirectionFromDeepSpreadSearch(level, blockPos, fluidState, amount, requiresSlope, directionsCanSpreadToSortedByAmount, statesAtPos);

        //if none, then choose from the initial sorted & filtered list
        if (spreadDirection == null && !requiresSlope) {
            return directionsCanSpreadToSortedByAmount.getFirst();
        }
        return spreadDirection;
    }

    @Unique
    private static int waterly$debugCheckCountSpreads = 0;
    @Unique
    private static int waterly$debugCheckCountDowns = 0;

    @Unique
    private @Nullable Direction waterly$getValidDirectionFromDeepSpreadSearch(final Level level, final BlockPos blockPos, final FluidState fluidState, final int amount, final boolean requiresSlope, final List<Direction> directionsCanSpreadToSortedByAmount, final Short2ObjectMap<Pair<BlockState, FluidState>> statesAtPos) {

        //vanilla slope distance factor
        int slopeFindDistance = getSlopeFindDistance(level);
        if (slopeFindDistance < 1) return null;

        //cache for flowing down checks
        Short2BooleanMap posCanFlowDown = new Short2BooleanOpenHashMap();
        posCanFlowDown.put(getCacheKey(blockPos, blockPos), false);//set not to flow back into source

        if (Waterly.debugSpread) {
            waterly$debugCheckCountSpreads = 4;
            waterly$debugCheckCountDowns = 0;
        }
        long start = System.nanoTime();

        //perform a deep search for the best direction to spread to with the nearest slope
        //filter out any directions that are too far away to be considered unless we don't require a slope, in which
        //case we just sort by distance for preference of spreading
        Direction finalDirection = directionsCanSpreadToSortedByAmount.stream()
                .map(dir -> {
                    //we already know we can spread to this direction, so we can just check if we can flow down or
                    //if we need to perform a deeper search
                    var posDir = blockPos.relative(dir);
                    short key = getCacheKey(blockPos, posDir);
                    //check if we can flow down here, if so, return the direction
                    boolean canFlowBelow = waterly$getSetFlowDownCache(key, level, posCanFlowDown, posDir, fluidState.getType(), requiresSlope);
                    if (canFlowBelow) {
                        return Pair.of(dir, 0);
                    } else {
                        //else, perform a deep search for the nearest slope
                        return Pair.of(dir, waterly$getSlopeDistance(
                                level, blockPos, 1, dir.getOpposite(),
                                fluidState.getType(), amount + 1, posDir, statesAtPos,
                                posCanFlowDown, requiresSlope, slopeFindDistance));
                    }
                })
                .filter(pair -> !requiresSlope || pair.getSecond() <= slopeFindDistance)
                .min(Comparator.comparingInt(Pair::getSecond))
                .map(Pair::getFirst).orElse(null);

        if (Waterly.debugSpread) {
            long time = System.nanoTime() - start;
            Waterly.totalDebugTicks++;
            Waterly.totalDebugMilliseconds = Waterly.totalDebugMilliseconds.add(BigDecimal.valueOf(time / 1000000D));
            Waterly.LOG.info("Waterly spread tick:\n Position: {}\n Spread checks: {}\n Down checks: {}\n Result: {}\n time: {}\n avg time (since debug change): {}ms",
                    blockPos.toShortString(), waterly$debugCheckCountSpreads, waterly$debugCheckCountDowns,
                    finalDirection, time, Waterly.getAverageDebugMilliseconds());
        }
        return finalDirection;
    }

    @Unique
    protected int waterly$getSlopeDistance(LevelReader level, BlockPos sourcePosForKey, int distance, Direction fromDir, Fluid sourceFluid, int sourceAmount,
                                           BlockPos newPos, Short2ObjectMap<Pair<BlockState, FluidState>> statesAtPos, Short2BooleanMap posCanFlowDown,
                                           boolean forceSlopeDownSameOrEmpty, int slopeFindDistance) {
        //default distance return
        int smallest = 1000;

        int searchDistance = distance + 1;

        //check all directions except the one we came from
        for (final Direction searchDir : Direction.Plane.HORIZONTAL) {
            if (searchDir != fromDir) {
                //get search context
                var searchPos = newPos.relative(searchDir);
                var searchKey = getCacheKey(sourcePosForKey, searchPos);
                var searchStates = waterly$getSetPosCache(searchKey, level, statesAtPos, searchPos);

                //if we can spread to the searched direction
                if (Waterly.debugSpread) waterly$debugCheckCountSpreads++;
                if (waterly$canSpreadToOptionallySameOrEmpty(sourceFluid, sourceAmount, level, newPos,
                        level.getBlockState(newPos), searchDir, searchPos,
                        searchStates.getFirst(), searchStates.getSecond(), forceSlopeDownSameOrEmpty)) {

                    //if we can flow down, cache the result of this and return this distance as it's the smallest
                    if (waterly$getSetFlowDownCache(searchKey, level, posCanFlowDown, searchPos, sourceFluid, forceSlopeDownSameOrEmpty)) {
                        //cache the result to both keys as we may also come back to this position from another direction
                        return searchDistance;
                    }
                    //if we can't flow down here, check the next distance via iteration as long as we are within the slope search distance
                    if (searchDistance < slopeFindDistance) {
                        int next = waterly$getSlopeDistance(level, sourcePosForKey, searchDistance,
                                searchDir.getOpposite(), sourceFluid, sourceAmount, searchPos,
                                statesAtPos, posCanFlowDown, forceSlopeDownSameOrEmpty, slopeFindDistance);
                        //if the next distance is less than the current smallest, update the smallest
                        if (next < smallest) {
                            smallest = next;
                        }
                        //continue to check all directions for the smallest distance
                    }
                }
            }
        }

        return smallest;
    }

    @Unique
    private Pair<BlockState, FluidState> waterly$getSetPosCache(short key, LevelReader level, Short2ObjectMap<Pair<BlockState, FluidState>> statesAtPos, BlockPos pos) {
        return statesAtPos.computeIfAbsent(key, (sx) -> {
            BlockState blockState = level.getBlockState(pos);
            return Pair.of(blockState, blockState.getFluidState());
        });
    }

    @Unique
    private boolean waterly$getSetFlowDownCache(short key, LevelReader level, Short2BooleanMap boolAtPos, BlockPos pos, Fluid sourceFluid, boolean forceSlopeDownSameOrEmpty) {
        return boolAtPos.computeIfAbsent(key, (sx) -> {
            if (Waterly.debugSpread) waterly$debugCheckCountDowns++;
            var posDown = pos.below();
            return (waterly$canSpreadToOptionallySameOrEmpty(sourceFluid, 8, level, pos, level.getBlockState(pos),
                    Direction.DOWN, posDown, level.getBlockState(posDown), level.getFluidState(posDown),
                    forceSlopeDownSameOrEmpty));
        });
    }

    @Unique
    private @Nullable Direction waterly$getFastLowestSpreadableEdge(Level level, BlockPos blockPos, FluidState fluidState, int amount) {
        ToIntFunction<Direction> func = (dir) -> level.getFluidState(blockPos.relative(dir).below()).getAmount();
        //just search neighbours for if we can spread to and below them
        return Waterly.getCardinalsShuffle().stream()
                .filter(dir -> {
                    BlockPos pos = blockPos.relative(dir);
                    BlockState state = level.getBlockState(pos);
                    var fluidState2 = level.getFluidState(pos);
                    var posDown = pos.below();
                    var stateDown = level.getBlockState(posDown);
                    var fluidStateDown = level.getFluidState(posDown);
                    return waterly$canSpreadTo(fluidState.getType(), fluidState.getAmount(), level, blockPos, level.getBlockState(blockPos), dir, pos, state, fluidState2)
                            && fluidState2.isEmpty()// let that fluid flow down instead
                            && waterly$canSpreadTo(fluidState.getType(), 8, level, pos, state, Direction.DOWN, posDown, stateDown, fluidStateDown)
                            && fluidStateDown.getAmount() < 8; //is a drop
                }).min(Comparator.comparingInt(func)).orElse(null);
    }

    @Unique
    private @Nullable Direction waterly$getFastLowestSpreadable(Level level, BlockPos blockPos, FluidState fluidState, int amount) {
        ToIntFunction<Direction> func = (dir) -> level.getFluidState(blockPos.relative(dir)).getAmount();
        //just search neighbours for if we can spread to them
        return Waterly.getCardinalsShuffle().stream()
                .filter(dir -> {
                    BlockPos pos = blockPos.relative(dir);
                    BlockState state = level.getBlockState(pos);
                    var fluidState2 = level.getFluidState(pos);
                    return waterly$canSpreadTo(fluidState.getType(), fluidState.getAmount(), level, blockPos, level.getBlockState(blockPos), dir, pos, state, fluidState2)
                            && fluidState2.getAmount() < amount;
                }).min(Comparator.comparingInt(func)).orElse(null);
    }

    @Unique
    protected void waterly$spreadTo2(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, Direction direction, int amount) {
        this.spreadTo(levelAccessor, blockPos, blockState, direction, waterly$getFluidStateOfAmount(amount));
    }

    public FluidState waterly$getFluidStateOfAmount(int amount) {
//        BlockPos posUp = blockPos.above();
//        BlockState bStateUp = level.getBlockState(posUp);
//        FluidState fStateUp = bStateUp.getFluidState();
//        if (!fStateUp.isEmpty() && fStateUp.getType().isSame(this) && this.canPassThroughWall(Direction.UP, level, blockPos, blockState, posUp, bStateUp)) {
//            return amount == 8 ? this.getSource(true) : this.getFlowing(amount, true);//todo true here broke? redundant now?
//        } else {
        return amount == 8 ? this.getSource(false) : this.getFlowing(amount, false);
//       }
    }

    @Unique
    protected void waterly$removeWater(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState) {
        if (blockState.getBlock() instanceof LiquidBlockContainer
                && blockState.getBlock() instanceof BucketPickup bucketPickup) {
            bucketPickup.pickupBlock(null, levelAccessor, blockPos, blockState);
        } else {
            levelAccessor.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Unique
    private boolean waterly$canSpreadToOptionallySameOrEmpty(Fluid sourceFluid, int sourceAmount, BlockGetter blockGetter,
                                                             BlockPos blockPos, BlockState blockState, Direction direction,
                                                             BlockPos blockPos2, BlockState blockState2, FluidState fluidState2,
                                                             boolean enforceSameFluidOrEmpty) {
        //add extra fluid check for enforcing replacing into own fluid type, or empty, only
        if (enforceSameFluidOrEmpty && !(fluidState2.isEmpty() || fluidState2.getType().isSame(sourceFluid)))
            return false;

        return waterly$canSpreadTo(sourceFluid, sourceAmount, blockGetter, blockPos, blockState, direction, blockPos2, blockState2, fluidState2);
    }

    @Unique
    private boolean waterly$canSpreadTo(Fluid sourceFluid, int sourceAmount, BlockGetter blockGetter,
                                        BlockPos blockPos, BlockState blockState, Direction direction,
                                        BlockPos blockPos2, BlockState blockState2, FluidState fluidState2) {
        //add extra fluid check for replacing into self
        return (fluidState2.canBeReplacedWith(blockGetter, blockPos2, sourceFluid, direction) || waterly$canFitIntoFluid(sourceFluid, fluidState2, direction, sourceAmount))
                && this.canPassThroughWall(direction, blockGetter, blockPos, blockState, blockPos2, blockState2)
                && this.canHoldFluid(blockGetter, blockPos2, blockState2, sourceFluid);
    }

    @Unique
    private boolean waterly$canFitIntoFluid(Fluid thisFluid, FluidState fluidStateTo, Direction direction, int amount) {
        if (fluidStateTo.isEmpty()) return true;

        if (fluidStateTo.getType().isSame(thisFluid)) {
            if (direction == Direction.DOWN) {
                return fluidStateTo.getAmount() < 8;
            } else {
                return fluidStateTo.getAmount() < amount;
            }
        }
        return false;
    }
}
