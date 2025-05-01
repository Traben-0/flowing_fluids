package traben.flowing_fluids.mixin;


import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static traben.flowing_fluids.FFFluidUtils.getStateForFluidByAmount;


@Mixin(FlowingFluid.class)
public abstract class MixinFlowingFluid extends Fluid {




    @Unique
    private static short ffCacheKey(final BlockPos sourcePos, final BlockPos spreadPos) {
        int i = spreadPos.getX() - sourcePos.getX();
        int j = spreadPos.getZ() - sourcePos.getZ();
        return (short)((i + 128 & 255) << 8 | j + 128 & 255);
    }

    @Unique
    private static boolean ff$handleWaterLoggedFlowAndReturnIfHandled(final Level level, final BlockPos posFrom, final FluidState fluidState, final int amount,
                                                                      final BlockState thisState, final BlockPos posTo, final int destFluidAmount,
                                                                      boolean flowingDown
    ) {
        //check if either too or from is water loggable and if so exit early if we cannot perform this flow due to settings
        boolean fromIsWaterloggable = thisState.getBlock() instanceof LiquidBlockContainer && thisState.getBlock() instanceof BucketPickup;
        if (fromIsWaterloggable
                && (flowingDown ? //cannot flow out
                FlowingFluids.config.waterLogFlowMode.blocksFlowOutDown()
                : FlowingFluids.config.waterLogFlowMode.blocksFlowOutSides())) {
            return true;
        }

        var blockTo = level.getBlockState(posTo).getBlock();
        boolean toIsWaterloggable = blockTo instanceof LiquidBlockContainer && blockTo instanceof BucketPickup;
        if (toIsWaterloggable && FlowingFluids.config.waterLogFlowMode.blocksFlowIn(flowingDown)) {//cannot flow in
            return true;
        }

        //from here the flow is allowed to proceed, but there is special handling for water loggables that might need to happen

        if (fromIsWaterloggable || toIsWaterloggable) {
            //here we are handling flow to or from a waterloggable block
            int totalAmount = destFluidAmount + amount;
            if (totalAmount < 8) { //crucial this only runs after we confirm they are waterloggables, as otherwise return should be false
                return true; //do nothing
            } else {
                //both should only be possible when flowing down
                if (toIsWaterloggable && fromIsWaterloggable) {
                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, posFrom, fluidState.getType(), 0);
                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, posTo, fluidState.getType(), 8);
                } else if (toIsWaterloggable) {
                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, posFrom, fluidState.getType(), totalAmount - 8);
                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, posTo, fluidState.getType(), 8);
                } else {//from
                    //don't flow out if destination cannot take all 8 levels of fluid
                    if (destFluidAmount > 0) return true;
                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, posFrom, fluidState.getType(), 0);
                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, posTo, fluidState.getType(), 8);
                }
            }
            return true;
        }
        //no water loggables
        return false;
    }

    @Override
    protected boolean isRandomlyTicking() {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.isFluidAllowed(this))
            return true;
        return super.isRandomlyTicking();
    }

    @Override
    protected void randomTick(final #if MC > MC_21 ServerLevel #else Level #endif level, final BlockPos pos, final FluidState state, final RandomSource random) {
        super.randomTick(level, pos, state, random);
        //random settle behaviour
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.randomTickLevelingDistance > 0
                && level.getChunkAt(pos).getFluidTicks().count() < 16 //ignore chunks with many updating fluids
                && FlowingFluids.config.isFluidAllowed(this)
                && !level.getFluidState(pos.above()).getType().isSame(this)//don't settle if there is a fluid above
        ) {
            //search in a random direction up to 32 blocks for a lower fluid to level out with

            final int amount = state.getAmount();
            if (amount <= getDropOff(level)) return;

            final int amountLess = amount - 1;

            final Direction randomDirection = FFFluidUtils.getCardinalsShuffle(level.getRandom()).get(0);

            BiConsumer<BlockPos.MutableBlockPos, BlockPos.MutableBlockPos> move;
            if(level.getRandom().nextBoolean()){
               move = (mbp, up)->{
                   mbp.move(randomDirection);
                   up.move(randomDirection);
               };
            } else {
                final Direction offStep = level.getRandom().nextBoolean() ? randomDirection.getClockWise() : randomDirection.getCounterClockWise();
                var rand = level.getRandom();
                move = (mbp, up)->{
                    var dir = rand.nextBoolean() ? randomDirection : offStep;
                    mbp.move(dir);
                    up.move(dir);
                };
            }

            final BlockPos.MutableBlockPos movingDir = pos.mutable();
            final BlockPos.MutableBlockPos movingDirAbove = pos.above().mutable();

            for (int i = 0; i < FlowingFluids.config.randomTickLevelingDistance; i++) {
                move.accept(movingDir, movingDirAbove);

                var stateDir = level.getBlockState(movingDir);
                if (!(stateDir.getBlock() instanceof LiquidBlock)) return;

                var fluidStateDir = stateDir.getFluidState();
                if (!fluidStateDir.getType().isSame(this)) return;

                if (level.getFluidState(movingDirAbove).getType().isSame(this)) return;

                int amountDir = fluidStateDir.getAmount();
                if (amountDir > amount) return;

                if (amountDir < amountLess) {
                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, movingDir, this, amountDir + 1);
                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, pos, this, amountLess);
                    return;
                }
                //continue;
            }
        }
    }

    @Shadow
    protected abstract int getDropOff(final LevelReader levelReader);

    @Shadow
    protected abstract void spreadTo(final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState blockState, final Direction direction, final FluidState fluidState);

    @Shadow
    protected abstract int getSlopeFindDistance(final LevelReader levelReader);


//    @Inject(method = "getFlow", at = @At(value = "HEAD"), cancellable = true)
//    private void ff$hideFlowingTexture(final BlockGetter blockReader, final BlockPos pos, final FluidState fluidState, final CallbackInfoReturnable<Vec3> cir) {
//        if (RenderSystem.isOnRenderThread()
//                && FlowingFluids.config.enableMod
//                && FlowingFluids.config.hideFlowingTexture) {
//            cir.setReturnValue(Vec3.ZERO);
//        }
//    }

    @Shadow
    public abstract int getAmount(final FluidState state);


    @Inject(method = "getOwnHeight", at = @At(value = "HEAD"), cancellable = true)
    private void ff$differentRenderHeight(final FluidState state, final CallbackInfoReturnable<Float> cir) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.isFluidAllowed(state)
                && FlowingFluids.config.fullLiquidHeight != FFConfig.LiquidHeight.REGULAR) {
            cir.setReturnValue(
                    switch (FlowingFluids.config.fullLiquidHeight) {
                        case BLOCK -> state.getAmount() / 8F;
                        case SLAB -> state.getAmount() / 16F;
                        case CARPET -> 0.0625f;
                        case REGULAR_LOWER_BOUND -> (state.getAmount() - 0.9F) * (8.0F / 9.0F) / 7.0F;
                        case BLOCK_LOWER_BOUND -> (state.getAmount() - 0.9F) / 7.0F;
                        default -> state.getAmount() / 9.0F;
                    }
            );
        }
    }

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    private void ff$tickMixin(final #if MC > MC_21 ServerLevel #else Level #endif level, final BlockPos blockPos,#if MC > MC_21 BlockState thisState, #endif final FluidState fluidState, final CallbackInfo ci) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.isFluidAllowed(fluidState)) {
            // cancel the original tick
            ci.cancel();


            if (System.currentTimeMillis() < FlowingFluids.debug_killFluidUpdatesUntilTime) {
                return; // kill this update
            }

            FlowingFluids.isManeuveringFluids = true;

            boolean isWaterAndInfiniteBiome = fluidState.is(FluidTags.WATER)
                    && level.getSeaLevel() >= blockPos.getY()
                    && blockPos.getY() > 0
                    && FFFluidUtils.matchInfiniteBiomes(level.getBiome(blockPos))
                    && level.getBrightness(LightLayer.SKY, blockPos) > 0;

            boolean dontConsumeWater = isWaterAndInfiniteBiome
                    && level.getRandom().nextFloat() < FlowingFluids.config.infiniteWaterBiomeNonConsumeChance;

            try {

                #if MC <= MC_21
                BlockState thisState = level.getBlockState(blockPos);
                #endif


                BlockPos posDown = blockPos.below();
                // check if we can flow down and if so how much fluid remains out of the 8 total possible
                int remainingAmount = flowing_fluids$checkAndFlowDown(level, blockPos, fluidState, thisState, posDown,
                        level.getBlockState(posDown), fluidState.getAmount());

                // if there is remaining amount still, the block below is full, or we couldn't flow down so also flow to the sides
                if (remainingAmount <= 0) {
                    return;
                }

                // simple pressure algorithm that might skip some hassle
                if (fluidState.getAmount() == 8 && thisState.liquid()) { // not messing with waterloggables here
                    BlockPos abovePos = blockPos.above();
                    var above = level.getBlockState(abovePos);
                    if (above.liquid()) { // not messing with waterloggables here
                        var aboveF = above.getFluidState();
                        int aboveAmount = aboveF.getAmount();
                        if (aboveAmount > 0){
                            var flow = (FlowingFluid) aboveF.getType();
                            if (FFFluidUtils.canFluidFlowFromPosToDirectionFitOverride(flow, level, abovePos, above, Direction.DOWN, blockPos, thisState)) {
                                var remainder = FFFluidUtils.placeConnectedFluidAmountAndPlaceAction(level, blockPos, aboveAmount,
                                        flow, 40, false, !FlowingFluids.pistonTick);
                                if (remainder.first() < aboveAmount) {
                                    remainder.second().run();
                                    if (!dontConsumeWater) FFFluidUtils.setFluidStateAtPosToNewAmount(level, abovePos, flow, remainder.first());
                                    return;
                                }
                            }
                        }
                    }
                }

                // if there is still water left, flow to the sides only if it is above the drop-off amount
                // the drop-off amount is the vanilla value determining how much each block of flow reduces the amount
                // this ties in nicely with a sort of surface tension effect
                if (remainingAmount > getDropOff(level)) {//drop off is 1 for water, 2 for lava in the overworld
                    ff$flowToSides(level, blockPos, fluidState, remainingAmount, thisState);//, remainingAmount);
                } else if (FlowingFluids.config.flowToEdges) {
                    // if the remaining amount is less than the drop-off amount, we can still flow to the sides but only if
                    // we find a nearby ledge to flow towards, as we want this water to settle when on flat ground
                    // use 1 as the amount as we don't spread to lower values than the drop-off, so we only want empty destination tiles
                    Direction dir = flowing_fluids$getLowestSpreadableLookingFor4BlockDrops(level, blockPos, fluidState, 1, true);

                    // dir is null if no spreadable block was found
                    if (dir != null) {
                        // much simpler logic than flowing_fluids$flowToSides() as we are only flowing our total remaining value into an empty space
                        var pos = blockPos.relative(dir);
                        flowing_fluids$setOrRemoveWaterAmountAt(level, blockPos, 0, thisState, dir);
                        flowing_fluids$spreadTo2(level, pos, level.getBlockState(pos), dir, remainingAmount);
                    }
                }



            } finally {

                if (isWaterAndInfiniteBiome) {
                    if (level.getSeaLevel() == blockPos.getY()) {
                        if (level.getRandom().nextFloat() < FlowingFluids.config.infiniteWaterBiomeDrainSurfaceChance) {
                            // drain into water if there is some below
                            var amount = level.getFluidState(blockPos).getAmount();
                            if (amount > 0) {
                                var below = level.getFluidState(blockPos.below());
                                if (below.getAmount() == 8 && below.is(FluidTags.WATER)) {
                                    level.setBlockAndUpdate(blockPos, FFFluidUtils.getBlockForFluidByAmount(this, amount - 1));
                                }
                            }
                        }
                    } else if (dontConsumeWater) {
                        // if we are in a truly infinite biome, we need to set this back to the original state
                        // as we don't want to lose water in these biomes
                        level.setBlock(blockPos, thisState, 0);
                    }
                }

                FlowingFluids.isManeuveringFluids = false;
                FlowingFluids.pistonTick = false;
            }
        }

    }

    @Unique
    private void ff$flowToSides(final Level level, final BlockPos blockPos, final FluidState fluidState, int amount, final BlockState thisState) {

        // get a valid direction to move into or null if no spreadable block was found
        Direction dir = flowing_fluids$getLowestSpreadableLookingFor4BlockDrops(level, blockPos, fluidState, amount, false);
        if (dir == null) return;

        var posDir = blockPos.relative(dir);

        // this amount is already confirmed to be less than {amount}
        final int destFluidAmount = level.getFluidState(posDir).getAmount();

        // must force total flow of fluid because of waterloggables
        if (ff$handleWaterLoggedFlowAndReturnIfHandled(level, blockPos, fluidState, amount, thisState, posDir, destFluidAmount, false))
            return;

        int fromAmount;
        int toAmount;


        // calculate the amount that would level both liquids
        final int difference = amount - destFluidAmount;
        final int averageLevel = destFluidAmount + difference / 2;

        // if the difference is odd, we need to add 1 to the 'from' amount
        boolean hasRemainder = (difference % 2 != 0);

        fromAmount = averageLevel;
        if (hasRemainder) {
            toAmount = averageLevel + 1;
        } else {
            toAmount = averageLevel;
        }

        FFFluidUtils.setFluidStateAtPosToNewAmount(level, blockPos, fluidState.getType(), fromAmount);
        FFFluidUtils.setFluidStateAtPosToNewAmount(level, posDir, fluidState.getType(), toAmount);
    }



    @Unique
    private int flowing_fluids$checkAndFlowDown(final Level level, final BlockPos blockPos, final FluidState fluidState, final BlockState thisState, final BlockPos posDown, final BlockState stateDown, int amount) {
        var downFState = level.getFluidState(posDown);
        // check and then handle if we can flow down
        if (flowing_fluids$canSpreadTo(fluidState.getType(), fluidState.getAmount(), level, blockPos, thisState,
                Direction.DOWN, posDown, stateDown, downFState)) {

            // handle other liquid vanilla collisions by causing a flow
            if (!downFState.isEmpty() && !downFState.getType().isSame(fluidState.getType())) {
                // send like vanilla flow to perform fluid collision
                // only use 1 for the amount, as we are only checking the collision behaviour
                // example: lava flowing down onto water creates stone in this case
                flowing_fluids$setOrRemoveWaterAmountAt(level, blockPos, amount - 1, thisState, Direction.DOWN);
                flowing_fluids$spreadTo2(level, posDown, stateDown, Direction.DOWN, 1);
                return amount - 1;
            } else {
                if (FlowingFluids.config.easyPistonPump && FlowingFluids.config.enablePistonPushing) {
                    // check if an upwards piston is present one block further below, and is still moving, and delay this tick
                    var block = level.getBlockState(posDown.below());
                    if (block.is(Blocks.MOVING_PISTON) && block.getValue(DirectionalBlock.FACING) == Direction.UP) {
                        // delay this tick
                        level.scheduleTick(blockPos, this, 10);
                        FlowingFluids.pistonTick = true;
                        return amount;
                    }
                }

                // flow into lower space
                int fluidDownAmount = downFState.getAmount();

                if (ff$handleWaterLoggedFlowAndReturnIfHandled(level, blockPos, fluidState, amount, thisState, posDown, fluidDownAmount, true))
                    return level.getFluidState(blockPos).getAmount();

                int amountDestCanAccept = Math.min(8 - fluidDownAmount, amount);
                // can fit some liquid
                if (amountDestCanAccept > 0) {
                    int destNewAmount = fluidDownAmount + amountDestCanAccept;
                    int sourceNewAmount = amount - amountDestCanAccept;
                    // set both amounts
                    flowing_fluids$setOrRemoveWaterAmountAt(level, blockPos, sourceNewAmount, thisState, Direction.DOWN);
                    flowing_fluids$spreadTo2(level, posDown, stateDown, Direction.DOWN, destNewAmount);
                    return sourceNewAmount;
                }
            }
        }
        // return the remaining amount of the source liquid
        return amount;
    }

    @Unique
    private void flowing_fluids$setOrRemoveWaterAmountAt(final Level level, final BlockPos blockPos, final int amount, final BlockState thisState, Direction direction) {
        if (amount > 0) {
            flowing_fluids$spreadTo2(level, blockPos, thisState, direction, amount);
        } else {
            FFFluidUtils.removeAllFluidAtPos(level, blockPos, this);
        }
    }

    @Inject(method = "getNewLiquid", at = @At(value = "HEAD"), cancellable = true)
    private void flowing_fluids$validateLiquidMixin(final #if MC > MC_21 ServerLevel #else Level #endif level, final BlockPos blockPos, final BlockState blockState, final CallbackInfoReturnable<FluidState> cir) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.isFluidAllowed(this)) {
            var state = level.getFluidState(blockPos);
            cir.setReturnValue(getStateForFluidByAmount(state.getType(), state.getAmount()));
        }
    }

    @Unique
    private @Nullable Direction flowing_fluids$getLowestSpreadableLookingFor4BlockDrops(
            Level level, BlockPos blockPos, FluidState fluidState, int amount, final boolean requiresSlope) {

        // state cache for each position
        Short2ObjectMap<Pair<BlockState, FluidState>> statesAtPos = new Short2ObjectOpenHashMap<>();

        //  flags if we should perform checks to override the slope requirement
        AtomicBoolean anyFlowableNeighbours2LevelsLowerOrMore = new AtomicBoolean(requiresSlope);

        // get the cardinal directions that are valid flow locations sorted by the amount of fluid in them,
        // ties are randomly sorted by initial shuffle
        List<Direction> directionsCanSpreadToSortedByAmount = FFFluidUtils.getCardinalsShuffle(level.random).stream()
                .sorted(Comparator.comparingInt((dir1) -> level.getFluidState(blockPos.relative(dir1)).getAmount()))
                .filter(dir -> {
                    BlockPos posDir = blockPos.relative(dir);
                    short key = ffCacheKey(blockPos, posDir);
                    var statesDir = flowing_fluids$getSetPosCache(key, level, statesAtPos, posDir);
                    BlockState stateDir = statesDir.getFirst();
                    var fluidStateDir = statesDir.getSecond();
                    int amountDir = fluidStateDir.getAmount();
                    boolean canFlow = flowing_fluids$canSpreadToOptionallySameOrEmpty(fluidState.getType(), amount, level, blockPos,
                            level.getBlockState(blockPos), dir, posDir, stateDir, fluidStateDir, requiresSlope);
                    if (canFlow && !anyFlowableNeighbours2LevelsLowerOrMore.get()) {
                        anyFlowableNeighbours2LevelsLowerOrMore.set(amountDir < amount - 1);
                    }
                    return canFlow;
                })
                .toList();

        // early escape if no valid neighbours to spread too
        if (directionsCanSpreadToSortedByAmount.isEmpty()) return null;

        // force require slope to true if no neighbours are 2 levels lower or more, ignore override if forceFluidLeveling is enabled
        boolean requiresSlopeWithOverride = requiresSlope || !anyFlowableNeighbours2LevelsLowerOrMore.get();

        // perform a deep search for the best direction to spread to with the nearest slope
        Direction spreadDirection = flowing_fluids$getValidDirectionFromDeepSpreadSearch(level, blockPos, fluidState, amount, requiresSlopeWithOverride, directionsCanSpreadToSortedByAmount, statesAtPos);

        // if none, then choose from the initial sorted & filtered list
        if (spreadDirection == null && !requiresSlopeWithOverride) {
            return directionsCanSpreadToSortedByAmount.get(0);
        }
        return spreadDirection;
    }

    @Unique
    private @Nullable Direction flowing_fluids$getValidDirectionFromDeepSpreadSearch(final Level level, final BlockPos blockPos, final FluidState fluidState, final int amount, final boolean requiresSlope, final List<Direction> directionsCanSpreadToSortedByAmount, final Short2ObjectMap<Pair<BlockState, FluidState>> statesAtPos) {

        // vanilla slope distance factor
        int slopeFindDistance = getSlopeFindDistance(level);
        if (slopeFindDistance < 1) return null;

        // cache for flowing down checks
        Short2BooleanMap posCanFlowDown = new Short2BooleanOpenHashMap();
        posCanFlowDown.put(ffCacheKey(blockPos, blockPos), false); // set not to flow back into source


        // perform a deep search for the best direction to spread to with the nearest slope
        // filter out any directions that are too far away to be considered unless we don't require a slope, in which
        // case we just sort by distance for preference of spreading
        // we already know we can spread to this direction, so we can just check if we can flow down or
        // if we need to perform a deeper search
        // check if we can flow down here, if so, return the direction
        // else, perform a deep search for the nearest slope
        return directionsCanSpreadToSortedByAmount.stream()
                .map(dir -> {
                    // we already know we can spread to this direction, so we can just check if we can flow down or
                    // if we need to perform a deeper search
                    var posDir = blockPos.relative(dir);
                    short key = ffCacheKey(blockPos, posDir);
                    // check if we can flow down here, if so, return the direction
                    if (level.getFluidState(posDir).getAmount() < (amount - 1) || flowing_fluids$getSetFlowDownCache(key, level, posCanFlowDown, posDir, fluidState.getType(), requiresSlope)) {
                        return Pair.of(dir, 0);
                    } else {
                        // else, perform a deep search for the nearest slope
                        return Pair.of(dir, flowing_fluids$getSlopeDistance(
                                level, blockPos, 1, dir.getOpposite(),
                                fluidState.getType(), amount + 1, posDir, statesAtPos,
                                posCanFlowDown, requiresSlope, slopeFindDistance));
                    }
                })
                .filter(pair -> !requiresSlope || pair.getSecond() <= slopeFindDistance)
                .min(Comparator.comparingInt(Pair::getSecond))
                .map(Pair::getFirst).orElse(null);
    }

    @Unique
    protected int flowing_fluids$getSlopeDistance(LevelReader level, BlockPos sourcePosForKey, int distance, Direction fromDir, Fluid sourceFluid, int sourceAmount,
                                                  BlockPos newPos, Short2ObjectMap<Pair<BlockState, FluidState>> statesAtPos, Short2BooleanMap posCanFlowDown,
                                                  boolean forceSlopeDownSameOrEmpty, int slopeFindDistance) {
        // currently in a worse case scenario, water spreading on flat ground, this deep search will perform:
        // 160 side spread, flowing_fluids$canSpreadToOptionallySameOrEmpty() checks
        // 40 downwards spread, flowing_fluids$getSetFlowDownCache() checks,
        // for a total of 200 checks per original source on totally flat ground

        // the 40 checks are perfectly cached and optimized and cannot be improved as there are exactly 40 possible blocks requiring downwards checks

        // the 160 checks can infact be optimized down to 130 by storing the results of checks using the to and from positions as the key as well as
        // the distance accepting any previously cached values that had lower or equal search distances (meaning those cached results searched further)
        // However, in practise the additional overhead of storing and checking the cache for all 160 searches, was not worth the 30 checks saved.
        // With the result cache we did 130 checks averaging 0.8~ms per spread check, without the cache we did 160 checks averaging 0.4~ms per tick
        // further result caching is detrimental!

        // default distance return
        int smallest = 1000;

        int searchDistance = distance + 1;

        //check all directions except the one we came from
        for (final Direction searchDir : Direction.Plane.HORIZONTAL) {
            if (searchDir != fromDir) {
                // get search context
                var searchPos = newPos.relative(searchDir);
                var searchKey = ffCacheKey(sourcePosForKey, searchPos);
                var searchStates = flowing_fluids$getSetPosCache(searchKey, level, statesAtPos, searchPos);

                // if we can spread to the searched direction
                if (flowing_fluids$canSpreadToOptionallySameOrEmpty(sourceFluid, sourceAmount, level, newPos,
                        level.getBlockState(newPos), searchDir, searchPos,
                        searchStates.getFirst(), searchStates.getSecond(), forceSlopeDownSameOrEmpty)) {

                    // if we can flow down, cache the result of this and return this distance as it's the smallest
                    if (searchStates.getSecond().getAmount() < (sourceAmount - 2)
                            || flowing_fluids$getSetFlowDownCache(searchKey, level, posCanFlowDown, searchPos, sourceFluid, forceSlopeDownSameOrEmpty)) {
                        //cache the result to both keys as we may also come back to this position from another direction
                        return searchDistance;
                    }
                    // if we can't flow down here, check the next distance via iteration as long as we are within the slope search distance
                    if (searchDistance < slopeFindDistance) {
                        int next = flowing_fluids$getSlopeDistance(level, sourcePosForKey, searchDistance,
                                searchDir.getOpposite(), sourceFluid, sourceAmount, searchPos,
                                statesAtPos, posCanFlowDown, forceSlopeDownSameOrEmpty, slopeFindDistance);
                        // if the next distance is less than the current smallest, update the smallest
                        if (next < smallest) {
                            smallest = next;
                        }
                        // continue to check all directions for the smallest distance
                    }
                }
            }
        }

        return smallest;
    }

    @Unique
    private Pair<BlockState, FluidState> flowing_fluids$getSetPosCache(short key, LevelReader level, Short2ObjectMap<Pair<BlockState, FluidState>> statesAtPos, BlockPos pos) {
        return statesAtPos.computeIfAbsent(key, (sx) -> {
            BlockState blockState = level.getBlockState(pos);
            return Pair.of(blockState, blockState.getFluidState());
        });
    }

    @Unique
    private boolean flowing_fluids$getSetFlowDownCache(short key, LevelReader level, Short2BooleanMap boolAtPos, BlockPos pos, Fluid sourceFluid, boolean forceSlopeDownSameOrEmpty) {
        return boolAtPos.computeIfAbsent(key, (sx) -> {
            var posDown = pos.below();
            return (flowing_fluids$canSpreadToOptionallySameOrEmpty(sourceFluid, 8, level, pos, level.getBlockState(pos),
                    Direction.DOWN, posDown, level.getBlockState(posDown), level.getFluidState(posDown),
                    forceSlopeDownSameOrEmpty));
        });
    }


    @Unique
    protected void flowing_fluids$spreadTo2(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, Direction direction, int amount) {
        this.spreadTo(levelAccessor, blockPos, blockState, direction, getStateForFluidByAmount(this, amount));
    }


    @Unique
    private boolean flowing_fluids$canSpreadToOptionallySameOrEmpty(Fluid sourceFluid, int sourceAmount, BlockGetter blockGetter,
                                                                    BlockPos blockPos, BlockState blockState, Direction direction,
                                                                    BlockPos blockPos2, BlockState blockState2, FluidState fluidState2,
                                                                    boolean enforceSameFluidOrEmpty) {
        //add extra fluid check for enforcing replacing into own fluid type, or empty, only
        if (enforceSameFluidOrEmpty && !(fluidState2.isEmpty() || fluidState2.getType().isSame(sourceFluid)))
            return false;

        return flowing_fluids$canSpreadTo(sourceFluid, sourceAmount, blockGetter, blockPos, blockState, direction, blockPos2, blockState2, fluidState2);
    }

    @Unique
    private boolean flowing_fluids$canSpreadTo(Fluid sourceFluid, int sourceAmount, BlockGetter blockGetter,
                                               BlockPos blockPos, BlockState blockState, Direction direction,
                                               BlockPos blockPos2, BlockState blockState2, FluidState fluidState2) {
        //add extra fluid check for replacing into self
        return FFFluidUtils.canFluidFlowFromPosToDirection((FlowingFluid) sourceFluid, sourceAmount, blockGetter, blockPos, blockState, direction, blockPos2, blockState2, fluidState2);
    }

}
