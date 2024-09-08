package traben.waterly.mixin;


import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import traben.waterly.FluidGetterByAmount;


@Mixin(WaterFluid.class)
public abstract class MixinWaterFluid extends FlowingFluid implements FluidGetterByAmount {

    @Shadow public abstract int getDropOff(final LevelReader levelReader);



    @Override
    protected void randomTick(final Level level, final BlockPos blockPos, final FluidState fluidState, final RandomSource randomSource) {
        super.randomTick(level, blockPos, fluidState, randomSource);

        if(true && !fluidState.isEmpty()){
            int amount = fluidState.getAmount();
            if (!waterly$increase(level, blockPos, amount))
                waterly$decrease(level, blockPos, amount);
        }
    }

    @Unique
    private boolean waterly$increase(final Level level, final BlockPos blockPos, int amount){
        if (amount < 8 && level.canSeeSky(blockPos)) {
            if (level.isRaining()) {//can see sky and raining
                level.setBlockAndUpdate(blockPos, waterly$getOfAmount(/*level, blockPos, level.getBlockState(blockPos),*/ amount + 1).createLegacyBlock());
                return true;
            }
            //if in ocean or river and below or at sea level and above 0
            var biome = level.getBiome(blockPos);
            if (level.getSeaLevel() >= blockPos.getY()
                    && (biome.is(BiomeTags.IS_OCEAN)
                    || biome.is(BiomeTags.IS_RIVER)
                    || biome.is(BiomeTags.IS_BEACH)
                    || biome.is(Biomes.SWAMP)
                    || biome.is(Biomes.MANGROVE_SWAMP))) {
                level.setBlockAndUpdate(blockPos, waterly$getOfAmount(/*level, blockPos, level.getBlockState(blockPos),*/ amount + 1).createLegacyBlock());
                return true;
            }
        }
        return false;
    }

    @Unique
    private void waterly$decrease(final Level level, final BlockPos blockPos, int amount){
        if(amount > 0 && amount <= getDropOff(level)){
            level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
        }
    }
}
