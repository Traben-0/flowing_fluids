package traben.flowing_fluids.mixin;


import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;


@Mixin(WaterFluid.class)
public abstract class MixinWaterFluid extends FlowingFluid {

    @Shadow
    public abstract int getDropOff(final LevelReader levelReader);

    @Shadow
    public abstract boolean isSame(final Fluid fluid);


    @Override
    protected void randomTick(final Level level, final BlockPos blockPos, final FluidState fluidState, final RandomSource randomSource) {
        super.randomTick(level, blockPos, fluidState, randomSource);

        if (level.isClientSide()
                || fluidState.isEmpty()
                || !FlowingFluids.config.enableMod
                || !FlowingFluids.config.isFluidAllowed(fluidState)) return;


        // if (print) FlowingFluids.LOG.info("[Flowing Fluids] - Random water ticked at {}", blockPos.toShortString());

        int amount = fluidState.getAmount();
        if (amount < 8) {
            if (ff$tryRainFill(level, blockPos, amount, level.random.nextFloat())) {
                if (FlowingFluids.config.printRandomTicks)
                    FlowingFluids.LOG.info("[Flowing Fluids] --- Water was filled by rain. Chance: {}", FlowingFluids.config.rainRefillChance);
                return;
            }
            if (ff$tryBiomeFillOrDrain(level, blockPos, amount, level.random.nextFloat())) {
                if (FlowingFluids.config.printRandomTicks)
                    FlowingFluids.LOG.info("[Flowing Fluids] --- Water was filled by biome. Chance: {}", FlowingFluids.config.oceanRiverSwampRefillChance);
                return;
            }
            if (ff$tryEvaporateNether(level, blockPos, amount, level.random.nextFloat())) {
                if (FlowingFluids.config.printRandomTicks)
                    FlowingFluids.LOG.info("[Flowing Fluids] --- Water was evaporated via Nether. Chance: {}", FlowingFluids.config.evaporationChance);
                return;
            }
            if (ff$tryEvaporate(level, blockPos, amount, level.random.nextFloat())) {
                if (FlowingFluids.config.printRandomTicks)
                    FlowingFluids.LOG.info("[Flowing Fluids] --- Water was evaporated - non Nether. Chance: {}", FlowingFluids.config.evaporationChance);
//                    return;
            }

//                if (print)
//                    FlowingFluids.LOG.info("[Flowing Fluids] --- Random tick did nothing. Chances:\nRain: {}\nBiome: {}\nEvaporation: {}",
//                            FlowingFluids.config.rainRefillChance, FlowingFluids.config.oceanRiverSwampRefillChance, FlowingFluids.config.evaporationChance);
        }
//            else {
//                if (print) FlowingFluids.LOG.info("[Flowing Fluids] --- Water was full. No action taken.");
//            }

    }




    @Unique
    private boolean ff$tryRainFill(final Level level, final BlockPos blockPos, int amount, float chance) {
        if (chance < FlowingFluids.config.rainRefillChance && level.isRaining() && level.canSeeSky(blockPos)) {//can see sky and raining
            level.setBlockAndUpdate(blockPos, FFFluidUtils.getBlockForFluidByAmount(this, amount + 1));
            return true;
        }
        return false;
    }

    @Unique
    private boolean ff$tryBiomeFillOrDrain(final Level level, final BlockPos blockPos, int amount, float chance) {

        if (chance < FlowingFluids.config.oceanRiverSwampRefillChance) {
            //if in ocean or river and below, at, or just above, sea level
            var biome = level.getBiome(blockPos);
            int seaLevelTop = level.getSeaLevel();
            if (seaLevelTop >= blockPos.getY()
                    && level.getBrightness(LightLayer.SKY, blockPos) > 0 // is close enough to sky/atmosphere access
                    && (biome.is(BiomeTags.IS_OCEAN)// is biome with refilling water
                    || biome.is(BiomeTags.IS_RIVER)
                    || biome.is(BiomeTags.IS_BEACH)
                    || biome.is(Biomes.SWAMP)
                    || biome.is(Biomes.MANGROVE_SWAMP))) {

                //increase if below sea level, and drain if just above sea level
                int modifiedAmount = amount + (seaLevelTop == blockPos.getY() ? -1 : 1);

                level.setBlockAndUpdate(blockPos, FFFluidUtils.getBlockForFluidByAmount(this, modifiedAmount));
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean ff$tryEvaporate(final Level level, final BlockPos blockPos, int amount, float chance) {
        if (chance < FlowingFluids.config.evaporationChance) {
            //evaporate over time if exposed to any sky light and is day
            if (amount <= getDropOff(level)
                    && level.isDay() && !level.isRaining()
                    && level.getFluidState(blockPos.below()).isEmpty()
                    && level.getBrightness(LightLayer.SKY, blockPos) > 0) {
                level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
                return true;
            }
        }
        return false;
    }


    @Unique
    private boolean ff$tryEvaporateNether(final Level level, final BlockPos blockPos, int amount, float chance) {

        if (chance < FlowingFluids.config.evaporationNetherChance) {
            //evaporate always if nether
            if (level.getBiome(blockPos).is(BiomeTags.IS_NETHER)) {
                if (amount == 1) {
                    level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
                } else {
                    level.setBlockAndUpdate(blockPos, FFFluidUtils.getBlockForFluidByAmount(this, amount - 1));
                }
                return true;
            }
        }
        return false;
    }

    @Inject(method = "getSlopeFindDistance", at = @At(value = "RETURN"), cancellable = true)
    private void ff$modifySlopeDistance(final LevelReader level, final CallbackInfoReturnable<Integer> cir) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this)) {
            cir.setReturnValue(Mth.clamp(FlowingFluids.config.waterFlowDistance, 1, 8));
        }
    }

    @Inject(method = "getTickDelay", at = @At(value = "RETURN"), cancellable = true)
    private void ff$modifyTickDelay(final LevelReader level, final CallbackInfoReturnable<Integer> cir) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this)) {
            cir.setReturnValue(Mth.clamp(FlowingFluids.config.waterTickDelay, 1, 255));
        }
    }
}
