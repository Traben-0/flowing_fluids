package traben.flowing_fluids.mixin.mixins;

import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.PlugWaterFeature;

@Mixin(ChunkGenerator.class)
public abstract class MixinAfterChunkGen  {

    @Inject(method = "applyBiomeDecoration", at = @At(value = "TAIL"))
    private void ff$postGen(final WorldGenLevel worldGenLevel, final ChunkAccess chunkAccess, final StructureManager structureManager, final CallbackInfo ci) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.encloseAllFluidOnWorldGen) {
            PlugWaterFeature.processChunk(worldGenLevel, chunkAccess.getPos(), chunkAccess);
        }
    }
}
