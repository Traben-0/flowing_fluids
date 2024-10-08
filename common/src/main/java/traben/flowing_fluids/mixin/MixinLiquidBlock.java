package traben.flowing_fluids.mixin;


import net.minecraft.core.BlockPos;
#if MC > MC_20_1
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
#endif
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;


@Mixin(LiquidBlock.class)
public abstract class MixinLiquidBlock extends Block implements BucketPickup {

    @Shadow
    @Final
    protected FlowingFluid fluid;


    public MixinLiquidBlock(final Properties properties) {
        super(properties);
    }


//    @ModifyArg(
//            method = "<init>",
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;<init>(Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V"),
//            index = 0
//    )
//    private static BlockBehaviour.Properties flowing_fluids$modifyBlockProperties(final Properties properties) {
//
//        return properties.randomTicks();
//
//        //handled by blockstate mixin for config support
//        //        return FlowingFluids.enable ?
////                properties.pushReaction(PushReaction.PUSH_ONLY).randomTicks()
////                : properties;
//    }


    @Inject(method = "pickupBlock", at = @At(value = "RETURN"), cancellable = true)
#if MC > MC_20_1
    private void ff$modifyBucket(final Player player, final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState blockState, final CallbackInfoReturnable<ItemStack> cir) {
#else
    private void ff$modifyBucket(final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState blockState, final CallbackInfoReturnable<ItemStack> cir) {
#endif

        if (cir.getReturnValue().isEmpty()
                && FlowingFluids.config.enableMod
                && FlowingFluids.config.isFluidAllowed(this.fluid)) {

            int level = FFFluidUtils.collectConnectedFluidAmountAndRemove(levelAccessor, blockPos, 1, 8, this.fluid);
            if (level > 0) {
                if (level >= 8) {
                    //success for full vanilla friendly bucket
                    cir.setReturnValue(new ItemStack(this.fluid.getBucket()));
                } else {
                    //add damage flag
                    var stack = new ItemStack(this.fluid.getBucket());
                            #if MC > MC_20_1
                            stack.applyComponents(DataComponentMap.builder()
                                    .set(DataComponents.DAMAGE, 8 - level)
                                    .set(DataComponents.MAX_DAMAGE, 8).build());
                            #else
                    stack.setDamageValue(8 - level);
                            #endif
                    cir.setReturnValue(stack);
                }
            }
        }

    }

}
