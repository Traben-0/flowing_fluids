package traben.flowing_fluids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

import static net.minecraft.world.level.block.TallSeagrassBlock.HALF;

@Mixin(KelpPlantBlock.class)
public abstract class MixinKelpPlantBlock extends GrowingPlantBodyBlock {


    protected MixinKelpPlantBlock(final Properties properties, final Direction growthDirection, final VoxelShape shape, final boolean scheduleFluidTicks) {
        super(properties, growthDirection, shape, scheduleFluidTicks);
    }

    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        boolean canSurvive = super.canSurvive(state, level, pos);
        if (canSurvive && FlowingFluids.config.enableMod && level instanceof LevelAccessor accessor) {
            //break the plant if its water can flow out of it
            if(FFFluidUtils.canFluidFlowToNeighbourFromPos(accessor, pos, Fluids.WATER, 8)) {
                return false;
            }
        }
        return canSurvive;
    }


}
