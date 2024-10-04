package traben.flowing_fluids.mixin.create;

#if CREATE == 0

import org.spongepowered.asm.mixin.Mixin;
import traben.flowing_fluids.config.FFCommands;

@Mixin(FFCommands.class)
public abstract class MixinPipe{
}
#else


import com.simibubi.create.content.fluids.OpenEndedPipe;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import traben.flowing_fluids.FlowingFluids;

@Pseudo
@Mixin(OpenEndedPipe.class)
public abstract class MixinPipe{

//!(Boolean)AllConfigs.server().fluids.pipesPlaceFluidSourceBlocks.get()

    @ModifyArg(method = "removeFluidFromSpace",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            , ordinal = 1), index = 1)
    private BlockState ff$modifyWaterRemoval(final BlockState blockState) {
        if (FlowingFluids.config.enableMod
                && !FlowingFluids.config.create_infinitePipes
                && AllConfigs.server().fluids.pipesPlaceFluidSourceBlocks.get()){
            return Blocks.AIR.defaultBlockState();
        }
        return blockState;
    }
}
#endif