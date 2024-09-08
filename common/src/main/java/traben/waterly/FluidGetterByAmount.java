package traben.waterly;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Unique;

public interface FluidGetterByAmount {
    @Unique
    FluidState waterly$getOfAmount(/*LevelAccessor level, BlockPos blockPos, BlockState blockState,*/ int amount) ;
}
