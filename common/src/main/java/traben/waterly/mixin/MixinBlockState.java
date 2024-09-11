package traben.waterly.mixin;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.waterly.Waterly;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MixinBlockState extends StateHolder<Block, BlockState> {


    @SuppressWarnings("DeprecatedIsStillUsed")
    @Shadow
    @Final
    @Deprecated
    private boolean liquid;

    protected MixinBlockState(final Block owner, final Reference2ObjectArrayMap<Property<?>, Comparable<?>> values, final MapCodec<BlockState> propertiesCodec) {
        super(owner, values, propertiesCodec);
    }

    @Inject(method = "getPistonPushReaction", at = @At(value = "RETURN"), cancellable = true)
    private void waterly$overridePushReaction(final CallbackInfoReturnable<PushReaction> cir) {
        if (Waterly.enable && liquid) {
            cir.setReturnValue(PushReaction.PUSH_ONLY);
        }
    }

    @Inject(method = "isRandomlyTicking", at = @At(value = "RETURN"), cancellable = true)
    private void waterly$overrideRandomTickCheck(final CallbackInfoReturnable<Boolean> cir) {
        if (Waterly.enable && liquid) {
            cir.setReturnValue(true);
        }
    }


}
