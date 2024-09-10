package traben.waterly;

import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Unique;

public interface FluidGetterByAmount {
    FluidState waterly$getFluidStateOfAmount(int amount) ;
}
