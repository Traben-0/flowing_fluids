package traben.flowing_fluids.mixin.mixins;

import net.minecraft.core.BlockPos;
//#if MC > 12001
import net.minecraft.core.dispenser.BlockSource;
//#else
//$$ import net.minecraft.core.BlockSource;
//#endif
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.flowing_fluids.FFBucketItem;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

import java.util.Map;
import java.util.function.BiFunction;

@Mixin(value = DispenserBlock.class, priority = 2000)
public class MixinDispenserBlock {

    @Shadow
    @Final
    public static Map<Item, DispenseItemBehavior> DISPENSER_REGISTRY;

    @Inject(method = "registerBehavior", at = @At(value = "TAIL"))
    private static void ff$wrapBehaviour(final ItemLike item, final DispenseItemBehavior behavior, final CallbackInfo ci) {
        //execute on tail to allow any other mixins to apply
        if (item.asItem() instanceof FFBucketItem bucket){

            BiFunction<BlockSource, ItemStack, ItemStack> delegate = behavior instanceof DefaultDispenseItemBehavior d ? d::execute : behavior::dispense;

            DefaultDispenseItemBehavior wrappedBehaviour;

            if (bucket == Items.BUCKET){
                wrappedBehaviour = new DefaultDispenseItemBehavior() {
                    public @NotNull ItemStack execute(BlockSource blockSource, ItemStack item) {
                        if (FlowingFluids.config.enableMod && item.getItem() instanceof FFBucketItem bucket) {
                            BlockPos blockPos = blockSource.
                                    //#if MC == 12001
                                    //$$ getPos()
                                    //#else
                                    pos()
                                    //#endif
                                        .relative(blockSource.
                                                //#if MC == 12001
                                                //$$ getBlockState()
                                                //#else
                                                state()
                                                //#endif
                                    .getValue(DispenserBlock.FACING));
                            Level level = blockSource.
                                //#if MC == 12001
                                //$$ getLevel();
                                //#else
                                level();
                                //#endif
                            var fState = level.getFluidState(blockPos);
                            if (fState.getType() instanceof FlowingFluid flowingFluid
                                    && FlowingFluids.config.isFluidAllowed(fState)
                                    && fState.getAmount() > 0 && fState.getAmount() < 8){
                                //we intervene for partial blocks
                                int found = FFFluidUtils.collectConnectedFluidAmountAndRemove(level, blockPos, 1, 8, flowingFluid);
                                if (found > 0){
                                    //#if MC == 12001
                                    return bucket.ff$bucketOfAmount(flowingFluid.getBucket().getDefaultInstance(), found);
                                    //#else
                                    //$$ return this.consumeWithRemainder(blockSource, item, bucket.ff$bucketOfAmount(flowingFluid.getBucket().getDefaultInstance(),found));
                                    //#endif
                                }
                                //this should never be reached as there will always be a partial fluid block to draw from
                                return item;
                            }
                        }
                        //vanilla handling
                        return delegate.apply(blockSource, item);
                    }
                };
            }else{
                wrappedBehaviour = new DefaultDispenseItemBehavior() {
                    public @NotNull ItemStack execute(BlockSource blockSource, ItemStack item) {
                        if (FlowingFluids.config.enableMod
                                && item.getItem() instanceof FFBucketItem bucket
                                && FlowingFluids.config.isFluidAllowed(bucket.ff$getFluid())) {
                            BlockPos blockPos = blockSource.
                                    //#if MC == 12001
                                    //$$ getPos()
                                    //#else
                                            pos()
                                    //#endif
                                    .relative(blockSource.
                                            //#if MC == 12001
                                            //$$ getBlockState()
                                            //#else
                                                    state()
                                            //#endif
                                            .getValue(DispenserBlock.FACING));
                            Level level = blockSource.
                                    //#if MC == 12001
                                    //$$ getLevel();
                                    //#else
                                            level();
                            //#endif
                            var fState = level.getFluidState(blockPos);
                            if (fState.getAmount() > 0 || item.getDamageValue() > 0){
                                //we intervene if the block can only accept partial water or if the bucket is not full
                                int amountInBucket = 8 - item.getDamageValue();
                                int remainder = bucket.ff$emptyContents_AndGetRemainder(null, level, blockPos, null, amountInBucket, false);
                                if (remainder != amountInBucket){
                                    ((DispensibleContainerItem)bucket).checkExtraContent(null, level, item, blockPos);
                                    //#if MC == 12001
                                    return bucket.ff$bucketOfAmount(item, remainder);
                                    //#else
                                    //$$ return this.consumeWithRemainder(blockSource, item, bucket.ff$bucketOfAmount(item,remainder));
                                    //#endif
                                }
                                return item;//no change and don't toss bucket as we want to prevent clogs from breaking these systems
                            }
                        }
                        //vanilla handling
                        return delegate.apply(blockSource, item);
                    }
                };
            }

            DISPENSER_REGISTRY.put(item.asItem(), wrappedBehaviour);
        }
    }
}
