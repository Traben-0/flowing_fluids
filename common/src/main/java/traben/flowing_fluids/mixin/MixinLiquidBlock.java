package traben.flowing_fluids.mixin;


import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
#if MC > MC_20_1
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
#endif
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
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

    public MixinLiquidBlock() {
        //noinspection DataFlowIssue
        super(null);
    }




    @ModifyExpressionValue(method = "shouldSpreadLiquid", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    // this is a real dodgey mixin target but any other way failed to grab the locals
    private boolean ff$consumeLevelObsidianOrCobbleCreation(final boolean original,
                                                            @Local(argsOnly = true) Level level,
                                                            @Local(ordinal = 1) BlockPos blockPos) {
        if (original && FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this.fluid)) {
            var state = level.getFluidState(blockPos);
            FFFluidUtils.setFluidStateAtPosToNewAmount(level, blockPos, state.getType(), state.getAmount() - 1);
        }
        return original;
    }

    @WrapOperation(method = "shouldSpreadLiquid", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;isSource()Z"))
    private boolean ff$modifyObsidianCondition(final FluidState instance, final Operation<Boolean> original) {
        boolean source = original.call(instance); // so any other mixin may run
        if (!source
                && FlowingFluids.config.enableMod
                && FlowingFluids.config.isFluidAllowed(this.fluid)
                && instance.getAmount() >= FlowingFluids.config.minLavaLevelForObsidian) {
            return true;
        }
        return source;
    }


    @Inject(method = "pickupBlock", at = @At(value = "RETURN"), cancellable = true)
    private void ff$modifyBucket(final CallbackInfoReturnable<ItemStack> cir, @Local(argsOnly = true, ordinal = 0) LevelAccessor levelAccessor, @Local(argsOnly = true, ordinal = 0) BlockPos blockPos) {
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
