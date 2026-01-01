package traben.flowing_fluids.forge.mixin.create;
#if MC!=MC_20_1

import org.spongepowered.asm.mixin.Mixin;
import traben.flowing_fluids.config.FFCommands;

@Mixin(FFCommands.class)
public abstract class FluidDrainingBehaviourAccessor{
}
#else
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(FluidDrainingBehaviour.class)
public interface FluidDrainingBehaviourAccessor {

    @Accessor("fluid")
    Fluid ff$getFluid();

    @Accessor("fluid")
    void ff$setFluid(Fluid fluid);
}
#endif

