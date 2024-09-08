package traben.waterly.mixin;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.waterly.FluidGetterByAmount;
import traben.waterly.Waterly;

import java.util.*;

import static net.minecraft.world.level.block.LiquidBlock.LEVEL;


@Mixin(LiquidBlock.class)
public abstract class MixinBucketPickup extends Block implements BucketPickup {

    @Shadow @Final protected FlowingFluid fluid;

    public MixinBucketPickup(final Properties properties) {
        super(properties);
    }

    @Inject(method = "pickupBlock", at = @At(value = "RETURN"), cancellable = true)
    private void waterly$modifyBucket(final Player player, final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState blockState, final CallbackInfoReturnable<ItemStack> cir) {
//        System.out.println("pickup");
        if (cir.getReturnValue().isEmpty() && true) {//enabled
            int level = levelAccessor.getFluidState(blockPos).getAmount();
            if (level > 0) {
                List<BlockPos> toCheck = new ArrayList<>();
                toCheck.add(blockPos);
                for (Direction direction : Waterly.getCardinalsAndDownShuffle()) {
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
                            if(level > 8) {
                                final int finalLevel = level-8;
                                onSuccessAirSetters.add(() -> levelAccessor.setBlock(pos, fluid.getFlowing(finalLevel, false).createLegacyBlock(), 11));
                                break;
                            }else{
                                onSuccessAirSetters.add(() -> levelAccessor.setBlock(pos, Blocks.AIR.defaultBlockState(), 11));
                                if (level == 8) break;
                                for (Direction direction : Waterly.getCardinalsAndDownShuffle()) {
                                    BlockPos offset = pos.relative(direction);
                                    if (!toCheck.contains(offset)) toCheck.add(offset);
                                }
                            }
                        }
                    }
                }
                //todo setting for if they even want partial buckets
                levelAccessor.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 11);
                onSuccessAirSetters.forEach(Runnable::run);
                if(level>=8){
                    //success for full vanilla friendly bucket
                    cir.setReturnValue(new ItemStack(this.fluid.getBucket()));
                }else{
                    //add damage flag
                    var stack =new ItemStack(this.fluid.getBucket());
                    stack.applyComponents(DataComponentMap.builder()
                            .set(DataComponents.DAMAGE, 8-level)
                            .set(DataComponents.MAX_DAMAGE,8).build());
                    cir.setReturnValue(stack);
                }
            }
        }
    }
}
