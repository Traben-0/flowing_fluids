package traben.flowing_fluids.mixin.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FlowingFluids;

@Mixin(PistonBaseBlock.class)
public abstract class MixinPistonAirPlacementMarker {

    @Inject(method = "moveBlocks", at = @At("HEAD"))
    private static void flowing_fluids$handlePistonAirPlacementMarker(Level level, BlockPos blockPos, Direction direction, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        FlowingFluids.lastPistonMoveDirection = bl ? direction : direction.getOpposite();
    }

    @Inject(method = "moveBlocks", at = @At("RETURN"))
    private static void flowing_fluids$handlePistonAirPlacementMarker2(CallbackInfoReturnable<Boolean> cir) {
        FlowingFluids.lastPistonMoveDirection = null;
    }
}
