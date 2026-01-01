package traben.flowing_fluids.forge.mixin.create;

#if MC!=MC_20_1

import org.spongepowered.asm.mixin.Mixin;
import traben.flowing_fluids.config.FFCommands;

@Mixin(FFCommands.class)
public abstract class MixinWaterWheel{
}
#else

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfig;

import java.util.Set;

@Pseudo
@Mixin(WaterWheelBlockEntity.class)
public abstract class MixinWaterWheel extends GeneratingKineticBlockEntity {


    @Shadow(remap = false) protected abstract Set<BlockPos> getOffsetsToCheck();

    @Shadow(remap = false) public abstract void setFlowScoreAndUpdate(final int score);

    public MixinWaterWheel(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "determineAndApplyFlowScore",
            at = @At(value = "HEAD"),
            cancellable = true, remap = false)
    private void ff$modifyWaterCheck(final CallbackInfo ci) {
        try {
            //leave if no change or level is null
            if (!FlowingFluids.config.enableMod
                    || FlowingFluids.config.create_waterWheelMode == FFConfig.CreateWaterWheelMode.REQUIRE_FLOW
                    || level == null
            ) return;

            //if REQUIRE_FLOW_OR_RIVER, check for river else fallback to regular flow check
            if (FlowingFluids.config.create_waterWheelMode.isRiver()
                    && !(level.getBiome(worldPosition).is(BiomeTags.IS_RIVER)
                    && Math.abs(worldPosition.getY() - level.getSeaLevel()) <= 5)
            ) {
                if (FlowingFluids.config.create_waterWheelMode.isRiverOnly()) {
                    ci.cancel();
                    this.setFlowScoreAndUpdate(0);
                }
                return;
            }

            //from here onwards the only possibilities are
            // - REQUIRE_FULL_FLUID
            // - REQUIRE_FLUID
            // - REQUIRE_FLOW_OR_RIVER and are in a river biome near sea level
            // - RIVER_ONLY and are in a river biome near sea level
            //all of these only require simple water count checks and don't need complex flow checks


            //the mixin will now always cancel the default method
            ci.cancel();

            //settings for alternative checks
            boolean fluidCanBeAnyHeight = !FlowingFluids.config.create_waterWheelMode.needsFullFluid();
            boolean oppositeSpin = FlowingFluids.config.create_waterWheelMode.isCounterSpin();
            boolean alwaysSpin = FlowingFluids.config.create_waterWheelMode.always();

            //search for valid fluids
            boolean lava = false;
            int score = 0;

            for (final BlockPos blockPos : this.getOffsetsToCheck()) {
                BlockPos checkPos = blockPos.offset(this.worldPosition);
                var fState = level.getFluidState(checkPos);
                lava |= fState.getType().isSame(Fluids.LAVA);

                if (alwaysSpin || (!fState.isEmpty() && (fluidCanBeAnyHeight || fState.getAmount() == 8))) {
                    score += oppositeSpin ? -1 : 1;
                }
            }


            //end setters from super method
            if (score != 0 && !this.level.isClientSide()) {
                this.award(lava ? AllAdvancements.LAVA_WHEEL : AllAdvancements.WATER_WHEEL);
            }

            this.setFlowScoreAndUpdate(score);

        }catch (final Exception ignored){}
    }
}
#endif