package traben.waterly.mixin;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.function.ToIntFunction;


@Mixin(WaterFluid.class)
public abstract class MixinWaterTicking extends FlowingFluid {


    @Override
    public void tick(Level level, BlockPos blockPos, FluidState fluidState) {

        if (!fluidState.isEmpty() && fluidState.getAmount() == level.getFluidState(blockPos).getAmount()) {
            BlockState thisState = level.getBlockState(blockPos);
            BlockPos posDown = blockPos.below();
            BlockState stateDown = level.getBlockState(posDown);

            int originalAmount = fluidState.getAmount();
            int amount = originalAmount;
            if (this.canSpreadTo2(fluidState,level, blockPos, thisState, Direction.DOWN, posDown, stateDown, level.getFluidState(posDown))) {
                int fluidDownAmount = level.getFluidState(posDown).getAmount();
                int transferAmount = Math.min(8 - fluidDownAmount, amount);
                if (transferAmount > 0) {

                    int fluidDownNewAmount = fluidDownAmount + transferAmount;
                    amount = amount - transferAmount;

                    System.out.println("downflow result: "+ originalAmount +", "+ fluidDownAmount +" -> " + amount +", "+ fluidDownNewAmount);

                    if (amount != originalAmount) {
                        originalAmount = amount;
                        setOrRemoveWaterAmountAt(level, blockPos, amount, thisState);
                    }

                    spreadTo2(level, posDown, stateDown, fluidDownNewAmount);

                }
            }

            if (amount > 1) {
                Direction dir = getLowestSpreadable(level, blockPos, fluidState, amount);
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
                        if (randomBool.nextBoolean()) {
                            toLeft++;
                        } else {
                            toRight++;
                        }
                    }
                    System.out.println("spread result: "+ amount +", " + fluidDirAmount +" -> "+toLeft + ", " + toRight);
                    amount = toLeft;
                    if (amount != originalAmount) {
                        System.out.println("setting amount to "+ amount);
                        setOrRemoveWaterAmountAt(level, blockPos, amount, thisState);
                    }

                    if (toRight != fluidDirAmount)
                        spreadTo2(level, pos, level.getBlockState(pos), toRight);
                }

            }

        }else{
            System.out.println("ticked invalid fluid state: "+ fluidState.getAmount() + " vs " + level.getFluidState(blockPos).getAmount());
        }
    }

    private void setOrRemoveWaterAmountAt(final Level level, final BlockPos blockPos, final int amount, final BlockState thisState) {
        if (amount > 0) {
            spreadTo2(level, blockPos, thisState, amount);
        }else{
            removeWater(level, blockPos, thisState);
        }
    }


    @Override
    protected FluidState getNewLiquid(Level level, BlockPos blockPos, BlockState blockState) {
        int amount = level.getFluidState(blockPos).getAmount();
        return getOfAmount(level, blockPos, blockState, amount);
    }

    @Unique
    private static Random randomBool = new Random();

    @Unique
    private static final List<Direction> CARDINALS = new ArrayList<>();
    static {
        CARDINALS.add(Direction.NORTH);
        CARDINALS.add(Direction.SOUTH);
        CARDINALS.add(Direction.EAST);
        CARDINALS.add(Direction.WEST);
    }

    @Unique
    private @Nullable Direction getLowestSpreadable(Level level, BlockPos blockPos, FluidState fluidState, int amount){
        ToIntFunction<Direction> func = (dir) ->level.getFluidState(blockPos.relative(dir)).getAmount();
        Collections.shuffle(CARDINALS);
        return CARDINALS.stream()
                .filter(dir -> {
                    BlockPos pos = blockPos.relative(dir);
                    BlockState state = level.getBlockState(pos);
                    var fluidState2 = level.getFluidState(pos);
                    return this.canSpreadTo2(fluidState, level, blockPos, level.getBlockState(blockPos), dir, pos, state, fluidState2)
                            && (fluidState2.isEmpty() || fluidState2.getAmount() < amount);
                }).min(Comparator.comparingInt(func)).orElse(null);
    }

    @Unique
    private FluidState getOfAmount(LevelAccessor level, BlockPos blockPos, BlockState blockState, int amount){
        FluidState state;
        if (amount > 8) System.out.println("amount > 8");
        BlockPos posUp = blockPos.above();
        BlockState bStateUp = level.getBlockState(posUp);
        FluidState fStateUp = bStateUp.getFluidState();
        if (!fStateUp.isEmpty() && fStateUp.getType().isSame(this) && this.canPassThroughWall(Direction.UP, level, blockPos, blockState, posUp, bStateUp)) {
            state = amount == 8 ? this.getSource(false) : this.getFlowing(amount, false);
        } else {
            if (amount <= 0) {
                System.out.println("AMOUNT <= 0!!!!!!!!!!!!!!");
                state =  Fluids.EMPTY.defaultFluidState();
            } else {
                state =  amount == 8 ? this.getSource(false) : this.getFlowing(amount, false);
            }
        }
        System.out.println("getOfAmount: "+amount + ", " + state.getAmount());
        return state;//todo simplify return remove print
    }

    @Unique
    protected void spreadTo2(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, int amount) {
//        System.out.println("spreadTo2: "+amount + ", " + getOfAmount(levelAccessor, blockPos, blockState, amount).getAmount());
        if (blockState.getBlock() instanceof LiquidBlockContainer) {
            ((LiquidBlockContainer)blockState.getBlock()).placeLiquid(levelAccessor, blockPos, blockState, getOfAmount(levelAccessor, blockPos, blockState, amount));
        } else {
            if (!blockState.isAir()) {
                this.beforeDestroyingBlock(levelAccessor, blockPos, blockState);
            }

            levelAccessor.setBlock(blockPos, getOfAmount(levelAccessor, blockPos,  blockState,amount).createLegacyBlock(), 3);
        }

    }
    @Unique
    protected void removeWater(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState) {
        if (blockState.getBlock() instanceof LiquidBlockContainer) {
            ((BucketPickup)blockState.getBlock()).pickupBlock(null,levelAccessor, blockPos, blockState);
        } else {
            levelAccessor.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
        }

    }


    @Unique
    private boolean canSpreadTo2(FluidState thisFluidState, BlockGetter blockGetter, BlockPos blockPos, BlockState blockState, Direction direction, BlockPos blockPos2, BlockState blockState2, FluidState fluidState) {
        return canMoveIntoFluidOnlyCheck(thisFluidState, fluidState, direction)
                && this.canPassThroughWall(direction, blockGetter, blockPos, blockState, blockPos2, blockState2)
                && this.canHoldFluid(blockGetter, blockPos2, blockState2, fluidState.getType());
    }

    @Unique
    private boolean canMoveIntoFluidOnlyCheck(FluidState thisFluidState, FluidState fluidStateTo, Direction direction) {
        if (direction == Direction.UP) return false;

        if(fluidStateTo.isEmpty() || fluidStateTo.getType().isSame(thisFluidState.getType())){
            if(direction == Direction.DOWN){
                return fluidStateTo.getAmount() < 8;
            }else {
                return fluidStateTo.getAmount() < thisFluidState.getAmount();
            }
        }
        return false;
    }

//    @Override
//    public int getAmount(FluidState fluidState) {
//        return 8;
//    }
}
