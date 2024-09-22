package traben.flowing_fluids.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FlowingFluids;

@Mixin(LiquidBlockRenderer.class)
public class MixinLiquidBlockRenderer {

    @Inject(method = "tesselate", at = @At(value = "HEAD"))
    private void ff$markRendering(final BlockAndTintGetter level, final BlockPos pos, final VertexConsumer buffer, final BlockState blockState, final FluidState fluidState, final CallbackInfo ci) {
        FlowingFluids.isRenderingFluids = true;
    }

    @Inject(method = "tesselate", at = @At(value = "RETURN"))
    private void ff$unmarkRendering(final BlockAndTintGetter level, final BlockPos pos, final VertexConsumer buffer, final BlockState blockState, final FluidState fluidState, final CallbackInfo ci) {
        FlowingFluids.isRenderingFluids = false;
    }
}
