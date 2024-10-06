package traben.flowing_fluids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
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
                            BlockPos blockPos = blockSource.pos().relative(blockSource.state().getValue(DispenserBlock.FACING));
                            Level level = blockSource.level();
                            var fState = level.getFluidState(blockPos);
                            if (fState.getType() instanceof FlowingFluid flowingFluid
                                    && fState.getAmount() > 0 && fState.getAmount() < 8){
                                //we intervene for partial blocks
                                int found = FFFluidUtils.collectConnectedFluidAmountAndRemove(level, blockPos, 1, 8, flowingFluid);
                                if (found > 0){
                                    return this.consumeWithRemainder(blockSource, item, bucket.ff$bucketOfAmount(flowingFluid.getBucket().getDefaultInstance(),found));
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
                        if (FlowingFluids.config.enableMod && item.getItem() instanceof FFBucketItem bucket) {
                            BlockPos blockPos = blockSource.pos().relative(blockSource.state().getValue(DispenserBlock.FACING));
                            Level level = blockSource.level();
                            var fState = level.getFluidState(blockPos);
                            if (fState.getAmount() > 0 || item.getDamageValue() > 0){
                                //we intervene if the block can only accept partial water or if the bucket is not full
                                int amountInBucket = 8 - item.getDamageValue();
                                int remainder = bucket.ff$emptyContents_AndGetRemainder(null, level, blockPos, null, amountInBucket);
                                if (remainder != amountInBucket){
                                    ((DispensibleContainerItem)bucket).checkExtraContent(null, level, item, blockPos);
                                    return this.consumeWithRemainder(blockSource, item, bucket.ff$bucketOfAmount(item,remainder));
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
