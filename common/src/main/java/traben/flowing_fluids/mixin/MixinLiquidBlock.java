package traben.flowing_fluids.mixin;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import traben.flowing_fluids.FlowingFluids;

import java.util.ArrayList;
import java.util.List;


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
    private void flowing_fluids$modifyBucket(final Player player, final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState blockState, final CallbackInfoReturnable<ItemStack> cir) {
//        System.out.println("pickup");
        if (cir.getReturnValue().isEmpty() && FlowingFluids.config.enableMod) {
            int level = levelAccessor.getFluidState(blockPos).getAmount();
            if (level > 0) {
                List<BlockPos> toCheck = new ArrayList<>();
                toCheck.add(blockPos);
                for (Direction direction : FlowingFluids.getCardinalsAndDownShuffle(levelAccessor.getRandom())) {
                    BlockPos offset = blockPos.relative(direction);
                    toCheck.add(offset);
                }

                List<Runnable> onSuccessAirSetters = new ArrayList<>();

                for (int i = 1; i < toCheck.size(); i++) {
                    var pos = toCheck.get(i);

                    if (toCheck.size() > 40) break;

                    var state = levelAccessor.getFluidState(pos);
                    if (this.fluid.isSame(state.getType())) {
                        int amount = state.getAmount();
                        if (amount > 0) {
                            level += amount;
                            if (level > 8) {
                                final int finalLevel = level - 8;
                                onSuccessAirSetters.add(() -> levelAccessor.setBlock(pos, fluid.getFlowing(finalLevel, false).createLegacyBlock(), 11));
                                break;
                            } else {
                                onSuccessAirSetters.add(() -> levelAccessor.setBlock(pos, Blocks.AIR.defaultBlockState(), 11));
                                if (level == 8) break;
                                for (Direction direction : FlowingFluids.getCardinalsAndDownShuffle(levelAccessor.getRandom())) {
                                    BlockPos offset = pos.relative(direction);
                                    if (!toCheck.contains(offset)) toCheck.add(offset);
                                }
                            }
                        }
                    }
                }

                levelAccessor.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 11);
                onSuccessAirSetters.forEach(Runnable::run);
                if (level >= 8) {
                    //success for full vanilla friendly bucket
                    cir.setReturnValue(new ItemStack(this.fluid.getBucket()));
                } else {
                    //add damage flag
                    var stack = new ItemStack(this.fluid.getBucket());
                    stack.applyComponents(DataComponentMap.builder()
                            .set(DataComponents.DAMAGE, 8 - level)
                            .set(DataComponents.MAX_DAMAGE, 8).build());
                    cir.setReturnValue(stack);
                }
            }
        }

    }
}
