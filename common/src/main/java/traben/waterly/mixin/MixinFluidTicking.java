package traben.waterly.mixin;



import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanFunction;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
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


    @Shadow protected abstract int getSlopeDistance(final LevelReader levelReader, final BlockPos blockPos, final int i, final Direction direction, final BlockState blockState, final BlockPos blockPos2, final Short2ObjectMap<com.mojang.datafixers.util.Pair<BlockState, FluidState>> short2ObjectMap, final Short2BooleanMap short2BooleanMap);

    @Shadow
    private static short getCacheKey(final BlockPos blockPos, final BlockPos blockPos2) {
        System.out.println("getCacheKey shouldnt be running");
        return 0;
    }

    @Shadow protected abstract int getSlopeFindDistance(final LevelReader levelReader);

    @Shadow protected abstract boolean isWaterHole(final BlockGetter blockGetter, final Fluid fluid, final BlockPos blockPos, final BlockState blockState, final BlockPos blockPos2, final BlockState blockState2);

    @Shadow public abstract Fluid getFlowing();

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    private void waterly$tickMixin(final Level level, final BlockPos blockPos, final FluidState fluidState, final CallbackInfo ci) {
        if (true) {
            if (!fluidState.isEmpty() && fluidState.getAmount() == level.getFluidState(blockPos).getAmount()) {
                BlockState thisState = level.getBlockState(blockPos);
                BlockPos posDown = blockPos.below();
                BlockState stateDown = level.getBlockState(posDown);

                int originalAmount = fluidState.getAmount();
                int amount = originalAmount;

                if (waterly$canSpreadTo2(fluidState.getType(), fluidState.getAmount(),level, blockPos, thisState,
                        Direction.DOWN, posDown, stateDown, level.getFluidState(posDown))) {
                    var downFState = level.getFluidState(posDown);
                    if(!downFState.getType().isSame(fluidState.getType())){
                        //send like vanilla flow to perform fluid collision
                        amount--;
                        originalAmount = amount;
                        waterly$setOrRemoveWaterAmountAt(level, blockPos, amount, thisState, Direction.DOWN);
                        waterly$spreadTo2(level, posDown, stateDown, Direction.DOWN, 1);
                    }else {
                        int fluidDownAmount = downFState.getAmount();
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
                }

                if (amount > getDropOff(level)) {//1 for water  2 for lava in overworld
                    Direction dir = waterly$getLowestSpreadableLookingFor4BlockDrops(level, blockPos, fluidState, amount,false);
                    //dir is null if no spreadable block was found
                    if (dir != null) {
                        var pos = blockPos.relative(dir);
                        if(thisState.getBlock() instanceof LiquidBlockContainer
                                && thisState.getBlock() instanceof BucketPickup getWater) {
                            //just empty water-loggables
                            getWater.pickupBlock(null, level, blockPos, thisState);
                            //this will lose some water, but it's the best we can do and water-loggables aren't "full" anyway
                            waterly$spreadTo2(level, pos, level.getBlockState(pos), dir, amount);
                        }else {
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
                    }

                }else if (amount > 0) {
                    //spill over edge if possible
                    Direction dir = waterly$getLowestSpreadableLookingFor4BlockDrops(level, blockPos, fluidState, amount, true);
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
            cir.setReturnValue(waterly$getOfAmount(/*level, blockPos, blockState,*/ amount));
        }
    }


    @Unique
    private @Nullable Direction waterly$getLowestSpreadableLookingFor4BlockDrops(Level level, BlockPos blockPos, FluidState fluidState, int amount, boolean requiresSlope){

        Short2ObjectMap<Pair<BlockState, FluidState>> statesAtPos = new Short2ObjectOpenHashMap<>();
        Short2BooleanMap posCanFlowDown = new Short2BooleanOpenHashMap();
        posCanFlowDown.put(getCacheKey(blockPos, blockPos), false);//set not to flow back into source

        ToIntFunction<Direction> dirToAmount = (dir) ->level.getFluidState(blockPos.relative(dir)).getAmount();

        List<Direction> byAmounts = Waterly.getCardinalsShuffle().stream()
                .sorted(Comparator.comparingInt(dirToAmount))
                .filter(dir ->{
                    BlockPos posDir = blockPos.relative(dir);
                    short key = getCacheKey(blockPos, posDir);
                    var statesDir = getSetPosMap(key, level, statesAtPos, posDir);
                    BlockState stateDir = statesDir.getFirst();
                    var fluidStateDir = statesDir.getSecond();
                    return waterly$canSpreadTo2(fluidState.getType(), amount, level, blockPos, level.getBlockState(blockPos), dir, posDir, stateDir, fluidStateDir);
                })
                .toList();

        if (byAmounts.isEmpty()) {
            return null;
        }
        var ret = byAmounts.stream()
                .map(dir -> {
                    var posDir = blockPos.relative(dir);
                    short key = getCacheKey(blockPos, posDir);
                    boolean canFlowBelow = getSetFlowDownMap(key, level, posCanFlowDown, posDir, fluidState.getType());
                    if (canFlowBelow) {
                        return Pair.of(dir, 0);
                    } else {
                        return Pair.of(dir, waterly$getSlopeDistance(
                                level, blockPos, 1, dir.getOpposite(),
                                fluidState.getType(), amount + 1, posDir, statesAtPos, posCanFlowDown));
                    }
                })
                .filter(pair -> !requiresSlope || pair.getSecond() < getSlopeFindDistance(level))
                .min(Comparator.comparingInt(Pair::getSecond))
                .map(Pair::getFirst).orElse(null);

        if (ret == null && !requiresSlope){
            return byAmounts.getFirst();
        }
        return ret;
    }

    @Unique
    protected int waterly$getSlopeDistance(LevelReader level, BlockPos sourcePosForKey, int distance, Direction fromDir, Fluid sourceFluid, int sourceAmount,
                                           BlockPos newPos, Short2ObjectMap<Pair<BlockState, FluidState>> statesAtPos, Short2BooleanMap posCanFlowDown) {
        int smallest = 1000;

        for (final Direction searchDir : Direction.Plane.HORIZONTAL) {
            if (searchDir != fromDir) {
                var searchPos = newPos.relative(searchDir);
                var searchKey = getCacheKey(sourcePosForKey, searchPos);
                var searchStates = getSetPosMap(searchKey, level, statesAtPos, searchPos);
                if(waterly$canSpreadTo2(sourceFluid, sourceAmount, level, newPos,
                        level.getBlockState(newPos), searchDir, searchPos,
                        searchStates.getFirst(), searchStates.getSecond())){
                    boolean canFlowBelow = getSetFlowDownMap(searchKey, level, posCanFlowDown, searchPos, sourceFluid);
                    if (canFlowBelow){
                        return distance;
                    }
                    if (distance <= getSlopeFindDistance(level)) {
                        int next = waterly$getSlopeDistance(level, sourcePosForKey, distance + 1, searchDir.getOpposite(), sourceFluid, sourceAmount, searchPos, statesAtPos, posCanFlowDown);
                        if (next < smallest) {
                            smallest = next;
                        }
                    }
                }
            }
        }
        return smallest;
    }

        @Unique
    private Pair<BlockState, FluidState> getSetPosMap(short key, LevelReader level, Short2ObjectMap<Pair<BlockState, FluidState>> statesAtPos, BlockPos pos){
        return statesAtPos.computeIfAbsent(key, (sx) -> {
            BlockState blockState = level.getBlockState(pos);
            return Pair.of(blockState, blockState.getFluidState());
        });
    }
    @Unique
    private boolean getSetFlowDownMap(short key, LevelReader level, Short2BooleanMap boolAtPos, BlockPos pos, Fluid sourceFluid){
        return boolAtPos.computeIfAbsent(key, (sx) -> {
            var posDown = pos.below();
            return (waterly$canSpreadTo2(sourceFluid, 8, level, pos, level.getBlockState(pos),
                    Direction.DOWN, posDown, level.getBlockState(posDown), level.getFluidState(posDown)));
        });
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
        this.spreadTo(levelAccessor, blockPos, blockState, direction, waterly$getOfAmount(/*levelAccessor, blockPos, blockState,*/ amount));
    }


    public FluidState waterly$getOfAmount(/*LevelAccessor level, BlockPos blockPos, BlockState blockState,*/ int amount) {
        if (amount > 8) System.out.println("amount > 8");

//        BlockPos posUp = blockPos.above();
//        BlockState bStateUp = level.getBlockState(posUp);
//        FluidState fStateUp = bStateUp.getFluidState();
//        if (!fStateUp.isEmpty() && fStateUp.getType().isSame(this) && this.canPassThroughWall(Direction.UP, level, blockPos, blockState, posUp, bStateUp)) {
//            return amount == 8 ? this.getSource(false) : this.getFlowing(amount, false);//todo true here broke? redundant now?
//        } else {
            if (amount <= 0) {
                System.out.println("AMOUNT <= 0!!!!!!!!!!!!!!");
                return Fluids.EMPTY.defaultFluidState();
            } else {
                return amount == 8 ? this.getSource(false) : this.getFlowing(amount, false);
            }
//        }

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
    private boolean waterly$canSpreadTo2(Fluid sourceFluid, int sourceAmount, BlockGetter blockGetter, BlockPos blockPos, BlockState blockState, Direction direction, BlockPos blockPos2, BlockState blockState2, FluidState fluidState2) {
        //add extra fluid check for replacing into self
        return (fluidState2.canBeReplacedWith(blockGetter, blockPos2, sourceFluid, direction) || waterly$canMoveIntoSelf(sourceFluid, fluidState2, direction, sourceAmount))
                && this.canPassThroughWall(direction, blockGetter, blockPos, blockState, blockPos2, blockState2)
                && this.canHoldFluid(blockGetter, blockPos2, blockState2, sourceFluid);
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
