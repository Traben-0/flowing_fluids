package traben.flowing_fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;

public interface IFFFlowListener {
    void ff$acceptRecentFlow(BlockPos pos, Direction direction, Fluid fluid, boolean downAlso);
}
