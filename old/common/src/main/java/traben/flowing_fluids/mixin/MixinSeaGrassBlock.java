package traben.flowing_fluids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

#if MC >= MC_21_5
import net.minecraft.world.level.block.VegetationBlock;
#else
import net.minecraft.world.level.block.BushBlock;
#endif

@Mixin(SeagrassBlock.class)
public abstract class MixinSeaGrassBlock extends #if MC >= MC_21_5 VegetationBlock #else BushBlock #endif {

    protected MixinSeaGrassBlock(final Properties properties) {
        super(properties);
    }

    @Override
    #if MC > MC_20_1 protected #else public  #endif boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        boolean canSurvive = super.canSurvive(state, level, pos);
        if (canSurvive && FlowingFluids.config.enableMod
                && FlowingFluids.config.isWaterAllowed()
                && level instanceof LevelAccessor accessor) {
            //break the plant if its water can flow out of it
            if(FFFluidUtils.canFluidFlowToNeighbourFromPos(accessor, pos, Fluids.WATER, 8)) {
                return false;
            }
        }
        return canSurvive;
    }


}
