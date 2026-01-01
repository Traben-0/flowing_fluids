package traben.flowing_fluids.mixin.mixins.create;

import org.spongepowered.asm.mixin.Mixin;

//#if !CREATE
import traben.flowing_fluids.mixin.CancelTarget;

@Mixin(CancelTarget.class)
public abstract class MixinPipe{
}
//#elseif FABRIC
//$$
//$$
//$$ import com.simibubi.create.content.fluids.OpenEndedPipe;
//$$ import com.simibubi.create.infrastructure.config.AllConfigs;
//$$ import net.minecraft.core.BlockPos;
//$$ import net.minecraft.world.level.Level;
//$$ import net.minecraft.world.level.block.Blocks;
//$$ import net.minecraft.world.level.block.state.BlockState;
//$$ import net.minecraft.world.ticks.ScheduledTick;
//$$ import org.spongepowered.asm.mixin.Pseudo;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.ModifyArg;
//$$ import traben.flowing_fluids.FlowingFluids;
//$$
//$$ @Pseudo
//$$ @Mixin(OpenEndedPipe.class)
//$$ public abstract class MixinPipe{
//$$
//$$ //!(Boolean)AllConfigs.server().fluids.pipesPlaceFluidSourceBlocks.get()
//$$
//$$     @Shadow
//$$     private Level world;
//$$
//$$     @Shadow
//$$     private BlockPos outputPos;
//$$
//$$     @ModifyArg(method = "removeFluidFromSpace",
//$$             at = @At(value = "INVOKE",
//$$                     target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
//$$             , ordinal = 1), index = 1)
//$$     private BlockState ff$modifyWaterRemoval(final BlockState blockState) {
//$$         if (FlowingFluids.config.enableMod
//$$                 && !FlowingFluids.config.create_infinitePipes
//$$                 && FlowingFluids.config.isFluidAllowed(blockState.getFluidState())
//$$                 && AllConfigs.server().fluids.pipesPlaceFluidSourceBlocks.get()){
//$$             world.getChunkAt(outputPos).getBlockTicks().schedule(
//$$                     //todo test
//$$                     new ScheduledTick<>(blockState.getBlock(), outputPos, world.getGameTime() + 5,99));
//$$             return Blocks.AIR.defaultBlockState();
//$$         }
//$$         return blockState;
//$$     }
//$$ }
//#endif