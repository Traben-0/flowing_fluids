package traben.flowing_fluids.mixin.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import traben.flowing_fluids.FFFlowListenerLevel;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.IFFFlowListener;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(Level.class)
public abstract class MixinLevel implements FFFlowListenerLevel {

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void flowing_fluids$displaceFluids(final BlockPos pos, final BlockState state, final int flags, final int recursionLeft, final CallbackInfoReturnable<Boolean> cir, @Local final LevelChunk levelChunk, @Local(ordinal = 1) final BlockState originalState) {
        FFFluidUtils.displaceFluids((Level) (Object) this, pos, state, flags, levelChunk, originalState);
    }

    @Unique
    Object2ObjectOpenHashMap<BlockPos, Set<BlockPos>> ff$flowListenerPositions = null;

    @Override
    public Map<BlockPos, Set<BlockPos>> ff$getFlowListenerPositions() {
        if (ff$flowListenerPositions == null) {
            ff$flowListenerPositions = new Object2ObjectOpenHashMap<>();
        }
        return ff$flowListenerPositions;
    }

}
