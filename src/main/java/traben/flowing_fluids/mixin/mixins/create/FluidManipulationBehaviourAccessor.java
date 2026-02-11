package traben.flowing_fluids.mixin.mixins.create;

import org.spongepowered.asm.mixin.Mixin;
//#if !CREATE

import traben.flowing_fluids.mixin.CancelTarget;

@Mixin(CancelTarget.class)
public abstract class FluidManipulationBehaviourAccessor{
}
//#else
//$$ import com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour;
//$$ import net.minecraft.core.BlockPos;
//$$ import net.minecraft.world.level.Level;
//$$ import net.minecraft.world.level.material.Fluid;
//$$ import org.spongepowered.asm.mixin.Pseudo;
//$$ import org.spongepowered.asm.mixin.gen.Invoker;
//$$
//$$ @Pseudo
//$$ @Mixin(FluidManipulationBehaviour.class)
//$$ public interface FluidManipulationBehaviourAccessor {
//$$
//$$     @Invoker("playEffect")
//$$     void ff$PlayEffect(Level world, BlockPos pos, Fluid fluid, boolean fillSound);
//$$
//$$ }
//#endif

