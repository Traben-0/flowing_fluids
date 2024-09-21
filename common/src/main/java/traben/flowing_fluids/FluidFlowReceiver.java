package traben.flowing_fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public interface FluidFlowReceiver {
    int ff$tryFlowAmountIntoAndReturnRemainingAmount(int amount, Fluid fromType, BlockState toState, final Level level, final BlockPos blockPos, Direction direction);
}
