package traben.flowing_fluids.mixin.mixins.create;

import org.spongepowered.asm.mixin.Mixin;
//#if !CREATE

import traben.flowing_fluids.mixin.CancelTarget;

@Mixin(CancelTarget.class)
public abstract class FluidDrainingBehaviourAccessor{
}
//#else
//$$ import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
//$$ import net.minecraft.world.level.material.Fluid;
//$$ import org.spongepowered.asm.mixin.Pseudo;
//$$ import org.spongepowered.asm.mixin.gen.Accessor;
//$$
//$$ @Pseudo
//$$ @Mixin(FluidDrainingBehaviour.class)
//$$ public interface FluidDrainingBehaviourAccessor {
//$$
//$$     @Accessor("fluid")
//$$     Fluid ff$getFluid();
//$$
//$$     @Accessor("fluid")
//$$     void ff$setFluid(Fluid fluid);
//$$ }
//#endif

