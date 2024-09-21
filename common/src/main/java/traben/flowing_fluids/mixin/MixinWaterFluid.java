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

    @Override
    protected void randomTick(final Level level, final BlockPos blockPos, final FluidState fluidState, final RandomSource randomSource) {
        super.randomTick(level, blockPos, fluidState, randomSource);

        //20% chance to fill from rain
        //10% chance to fill from water biome
        //1% chance to evaporate

        if (FlowingFluids.config.enableMod && !fluidState.isEmpty() && level.random.nextInt(5) == 0) {
            //20% chance to continue
            int amount = fluidState.getAmount();
            if (!flowing_fluids$increase(level, blockPos, amount)
                    && level.random.nextInt(10) == 0)
                //1% chance to continue
                flowing_fluids$decrease(level, blockPos, amount);
        }
    }

    @Override
    protected boolean isRandomlyTicking() {
        if (FlowingFluids.config.enableMod) return true;
        return super.isRandomlyTicking();
    }

    @Unique
    private boolean flowing_fluids$increase(final Level level, final BlockPos blockPos, int amount) {
        //20% chance
        if (amount < 8) {
            if (level.isRaining() && level.canSeeSky(blockPos)) {//can see sky and raining
                level.setBlockAndUpdate(blockPos, flowing_fluids$getFluidStateOfAmount(/*level, blockPos, level.getBlockState(blockPos),*/ amount + 1).createLegacyBlock());
                return true;
            }

            if (level.random.nextBoolean()) return true;
            //10% chance to continue

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
        } else return level.random.nextInt(4) < 3;
        //10% chance to continue


        return false;
    }

    @Unique
    private void flowing_fluids$decrease(final Level level, final BlockPos blockPos, int amount) {
        //evaporate over time if exposed to any sky light and is day
        if (amount <= getDropOff(level)
                && level.isDay()
                && level.getFluidState(blockPos.below()).isEmpty()
                && level.getBrightness(LightLayer.SKY, blockPos) > 0) {
            level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
        }
    }
}
