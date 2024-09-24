package traben.flowing_fluids.mixin;

import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import traben.flowing_fluids.FlowingFluids;

@Mixin(LiquidBlockRenderer.class)
public abstract class MixinLiquidBlockRenderer {

    @ModifyVariable(
            method = "tesselate",
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;",
                    shift = At.Shift.AFTER)
            , ordinal = 0
    )
    private Vec3 ff$alterFlowDir(final Vec3 value) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.hideFlowingTexture) {
            return Vec3.ZERO;
        }
        return value;
    }



}
