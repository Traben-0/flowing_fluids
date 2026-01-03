package traben.flowing_fluids.mixin.mixins;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FlowingFluids;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MixinBlockState extends StateHolder<Block, BlockState> {


    @SuppressWarnings("DeprecatedIsStillUsed")
    @Shadow
    @Final
    @Deprecated
    private boolean liquid;

    @Shadow
    public abstract FluidState getFluidState();

    //#if MC > 12001
    protected MixinBlockState(final Block owner, final Reference2ObjectArrayMap<Property<?>, Comparable<?>> values, final MapCodec<BlockState> propertiesCodec) {
        super(owner, values, propertiesCodec);
    }
    //#else
    //$$ protected MixinBlockState(final Block owner, final ImmutableMap<Property<?>, Comparable<?>> values, final MapCodec<BlockState> propertiesCodec) {
    //$$     super(owner, values, propertiesCodec);
    //$$ }
    //#endif


    @Inject(method = "isRandomlyTicking", at = @At(value = "RETURN"), cancellable = true)
    private void flowing_fluids$overrideRandomTickCheck(final CallbackInfoReturnable<Boolean> cir) {
        if (liquid
                && FlowingFluids.config.enableMod
                && FlowingFluids.config.isFluidAllowed(getFluidState())) {
            cir.setReturnValue(true);
        }
    }


}
