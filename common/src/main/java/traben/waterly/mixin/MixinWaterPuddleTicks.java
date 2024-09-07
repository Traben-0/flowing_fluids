package traben.waterly.mixin;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
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

import java.util.*;
import java.util.function.ToIntFunction;


@Mixin(WaterFluid.class)
public abstract class MixinWaterPuddleTicks extends FlowingFluid implements FluidGetterByAmount {

    @Shadow public abstract int getDropOff(final LevelReader levelReader);

    @Unique
//    private final Random waterly$random = new Random();

    @Override
    protected void randomTick(final Level level, final BlockPos blockPos, final FluidState fluidState, final RandomSource randomSource) {
        super.randomTick(level, blockPos, fluidState, randomSource);

        if(true ){
            int amount = fluidState.getAmount();
            if(amount > 0 && amount <= getDropOff(level)
//                    && waterly$random.nextInt(20) == 0
            ){
                if (level.isRaining() && level.canSeeSky(blockPos)) {
                    if (amount < 8) level.setBlockAndUpdate(blockPos, waterly$getOfAmount(level, blockPos, level.getBlockState(blockPos), amount + 1).createLegacyBlock());
                } else {
                    level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
}
