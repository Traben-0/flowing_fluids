package traben.flowing_fluids.mixin;


import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
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
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.FluidGetterByAmount;


@Mixin(WaterFluid.class)
public abstract class MixinWaterFluid extends FlowingFluid implements FluidGetterByAmount {

    @Shadow
    public abstract int getDropOff(final LevelReader levelReader);

    @Shadow
    public abstract boolean isSame(final Fluid fluid);


    @Override
    protected void randomTick(final Level level, final BlockPos blockPos, final FluidState fluidState, final RandomSource randomSource) {

        super.randomTick(level, blockPos, fluidState, randomSource);
        if (level.isClientSide()) return;

        if (FlowingFluids.config.enableMod && !fluidState.isEmpty()) {

            boolean print = FlowingFluids.config.printRandomTicks;
            if (print) FlowingFluids.LOG.info("[Flowing Fluids] - Random water ticked at {}", blockPos.toShortString());

            int amount = fluidState.getAmount();
            if (amount < 8) {
                if (ff$tryRainFill(level, blockPos, amount, level.random.nextFloat())) {
                    if (print)
                        FlowingFluids.LOG.info("[Flowing Fluids] --- Water was filled by rain. Chance: {}", FlowingFluids.config.rainRefillChance);
                    return;
                }
                if (ff$tryBiomeFill(level, blockPos, amount, level.random.nextFloat())) {
                    if (print)
                        FlowingFluids.LOG.info("[Flowing Fluids] --- Water was filled by biome. Chance: {}", FlowingFluids.config.oceanRiverSwampRefillChance);
                    return;
                }
                if (ff$tryEvaporateNether(level, blockPos, amount, level.random.nextFloat())) {
                    if (print)
                        FlowingFluids.LOG.info("[Flowing Fluids] --- Water was evaporated via Nether. Chance: {}", FlowingFluids.config.evaporationChance);
                    return;
                }
                if (ff$tryEvaporate(level, blockPos, amount, level.random.nextFloat())) {
                    if (print)
                        FlowingFluids.LOG.info("[Flowing Fluids] --- Water was evaporated - non Nether. Chance: {}", FlowingFluids.config.evaporationChance);
                    return;
                }

                if (print)
                    FlowingFluids.LOG.info("[Flowing Fluids] --- Random tick did nothing. Chances:\nRain: {}\nBiome: {}\nEvaporation: {}",
                            FlowingFluids.config.rainRefillChance, FlowingFluids.config.oceanRiverSwampRefillChance, FlowingFluids.config.evaporationChance);
            } else {
                if (print) FlowingFluids.LOG.info("[Flowing Fluids] --- Water was full. No action taken.");
            }
        }
    }

    @Override
    protected boolean isRandomlyTicking() {
        if (FlowingFluids.config.enableMod) return true;
        return super.isRandomlyTicking();
    }


    @Unique
    private boolean ff$tryRainFill(final Level level, final BlockPos blockPos, int amount, float chance) {

        if (chance < FlowingFluids.config.rainRefillChance && level.isRaining() && level.canSeeSky(blockPos)) {//can see sky and raining
            level.setBlockAndUpdate(blockPos, flowing_fluids$getFluidStateOfAmount(/*level, blockPos, level.getBlockState(blockPos),*/ amount + 1).createLegacyBlock());
            return true;
        }
        return false;
    }

    @Unique
    private boolean ff$tryBiomeFill(final Level level, final BlockPos blockPos, int amount, float chance) {

        if (chance < FlowingFluids.config.oceanRiverSwampRefillChance) {
            //if in ocean or river and below or at sea level and above 0
            var biome = level.getBiome(blockPos);
            if (level.getSeaLevel() >= blockPos.getY()//between sea level and 0
                    && level.getBrightness(LightLayer.SKY, blockPos) > 0 // is close enough to sky/atmosphere access
                    && (biome.is(BiomeTags.IS_OCEAN)// is biome with refilling water
                    || biome.is(BiomeTags.IS_RIVER)
                    || biome.is(BiomeTags.IS_BEACH)
                    || biome.is(Biomes.SWAMP)
                    || biome.is(Biomes.MANGROVE_SWAMP))) {

                level.setBlockAndUpdate(blockPos, flowing_fluids$getFluidStateOfAmount(amount + 1).createLegacyBlock());
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean ff$tryEvaporate(final Level level, final BlockPos blockPos, int amount, float chance) {
        if (chance < FlowingFluids.config.evaporationChance){
            //evaporate over time if exposed to any sky light and is day
            if(amount <= getDropOff(level)
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

        if (chance < FlowingFluids.config.evaporationNetherChance){
            //evaporate always if nether
            if (level.getBiome(blockPos).is(BiomeTags.IS_NETHER)){
                if (amount == 1){
                    level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
                }else {
                    level.setBlockAndUpdate(blockPos, flowing_fluids$getFluidStateOfAmount(amount - 1).createLegacyBlock());
                }
                return true;
            }
        }
        return false;
    }
}
