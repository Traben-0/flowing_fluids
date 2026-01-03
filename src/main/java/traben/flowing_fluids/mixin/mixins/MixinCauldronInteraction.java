package traben.flowing_fluids.mixin.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
//#if MC > 12100 || MC == 12001
import net.minecraft.world.InteractionResult;
//#else
//$$ import net.minecraft.world.ItemInteractionResult;
//#endif
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.flowing_fluids.FFBucketItem;
import traben.flowing_fluids.FlowingFluids;

import static net.minecraft.core.cauldron.CauldronInteraction.*;

@Mixin(CauldronInteraction.class)
public abstract class MixinCauldronInteraction {

    @Inject(method = "bootStrap", at = @At("TAIL"))
    private static void ff$bootStrap(CallbackInfo ci) {
        EMPTY
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.LAVA_BUCKET, (k, prev) -> (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!allow(Fluids.LAVA)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            if (!level.isClientSide()) {
                if (isUnderWater(level, blockPos)) return result(false);
                return fillCauldron(blockState, level, blockPos, player, interactionHand, itemStack,
                        Blocks.LAVA_CAULDRON.defaultBlockState());
            }
            return result(true);
        });
        EMPTY
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.WATER_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!allow(Fluids.WATER)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            if (!level.isClientSide()) {
                return fillCauldron(blockState, level, blockPos, player, interactionHand, itemStack,
                        Blocks.WATER_CAULDRON.defaultBlockState());
            }
            return result(true);
        });



        WATER
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.WATER_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!allow(Fluids.WATER)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            if (!level.isClientSide()) {
                if (fillCauldron(blockState, level, blockPos, player, interactionHand, itemStack,
                        Blocks.WATER_CAULDRON.defaultBlockState()) != result(true)) {
                    return emptyCauldron(blockState, level, blockPos, player, interactionHand, itemStack,
                            Items.WATER_BUCKET.getDefaultInstance(), SoundEvents.BUCKET_FILL);
                }
            }
            return result(true);
        });
        WATER
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.LAVA_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!allow(Fluids.LAVA)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            return result(false);
        });
        WATER
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.POWDER_SNOW_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!allow()) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            return result(false);
        });
        WATER
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!allow(Fluids.WATER)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            if (!level.isClientSide()) {
                return emptyCauldron(blockState, level, blockPos, player, interactionHand, itemStack,
                            Items.WATER_BUCKET.getDefaultInstance(), SoundEvents.BUCKET_FILL);
            }
            return result(true);
        });



        LAVA
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.LAVA_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!allow(Fluids.LAVA))
                return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            if (!level.isClientSide()) {
                if (isUnderWater(level, blockPos)) return result(false);
                if (fillCauldron(blockState, level, blockPos, player, interactionHand, itemStack,
                        Blocks.LAVA_CAULDRON.defaultBlockState()) != result(true)) {
                    return emptyCauldron(blockState, level, blockPos, player, interactionHand, itemStack,
                            Items.LAVA_BUCKET.getDefaultInstance(), SoundEvents.BUCKET_FILL_LAVA);
                }
            }
            return result(true);
        });
        LAVA
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.WATER_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!allow(Fluids.WATER)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            return result(false);
        });
        LAVA
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.POWDER_SNOW_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!allow()) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            return result(false);
        });
        LAVA
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!allow(Fluids.LAVA)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            if (!level.isClientSide()) {
                return emptyCauldron(blockState, level, blockPos, player, interactionHand, itemStack,
                            Items.LAVA_BUCKET.getDefaultInstance(), SoundEvents.BUCKET_FILL_LAVA);
            }
            return result(true);
        });



        POWDER_SNOW
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.LAVA_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!allow(Fluids.LAVA)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            return result(false);
        });
        POWDER_SNOW
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.WATER_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!allow(Fluids.WATER)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            return result(false);
        });
    }

    @Unique
    private static boolean isUnderWater(Level arg, BlockPos arg2) {
        FluidState fluidState = arg.getFluidState(arg2.above());
        return fluidState.is(FluidTags.WATER);
    }


    @Unique
    private static boolean allow(Fluid fluid) {
        return allow() && FlowingFluids.config.isFluidAllowed(fluid);
    }

    @Unique
    private static boolean allow() {
        return FlowingFluids.config.enableMod;
    }

    //#if MC > 12100 || MC == 12001
    @Unique
    private static InteractionResult result(boolean pass) {
        return pass ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
    //#else
    //$$ @Unique
    //$$ private static ItemInteractionResult result(boolean pass) {
    //$$     return pass ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
    //$$ }
    //#endif


    @Unique
    private static
    //#if MC > 12100 || MC == 12001
    InteractionResult
    //#else
    //$$ ItemInteractionResult
    //#endif
    fillCauldron(BlockState cauldron, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, ItemStack bucket,
                                                      BlockState defaultFilledCauldronState) {
        int bucketAmount = 8 - bucket.getDamageValue();
        int ogAmount = bucketAmount;
        int cauldronNeeds = 3 - cauldron.getValue(LayeredCauldronBlock.LEVEL);
        switch (cauldronNeeds) {
            case 1:
                if (bucketAmount >= 3) {
                    bucketAmount -= 3;
                    level.setBlockAndUpdate(blockPos, defaultFilledCauldronState.setValue(LayeredCauldronBlock.LEVEL, 3));
                }
                break;
            case 2:
                if (bucketAmount >= 6) {
                    bucketAmount -= 6;
                    level.setBlockAndUpdate(blockPos, defaultFilledCauldronState.setValue(LayeredCauldronBlock.LEVEL, 3));
                } else if (bucketAmount >= 3) {
                    bucketAmount -= 3;
                    level.setBlockAndUpdate(blockPos, defaultFilledCauldronState.setValue(LayeredCauldronBlock.LEVEL, 2));
                }
                break;
            case 3:
                if (bucketAmount >= 8) {
                    bucketAmount -= 6;
                    level.setBlockAndUpdate(blockPos, defaultFilledCauldronState.setValue(LayeredCauldronBlock.LEVEL, 3));
                } else if (bucketAmount >= 6) {
                    bucketAmount -= 6;
                    level.setBlockAndUpdate(blockPos, defaultFilledCauldronState.setValue(LayeredCauldronBlock.LEVEL, 2));
                } else if (bucketAmount >= 3) {
                    bucketAmount -= 3;
                    level.setBlockAndUpdate(blockPos, defaultFilledCauldronState.setValue(LayeredCauldronBlock.LEVEL, 1));
                }
                break;
            default:
                break;
        }

        if (bucketAmount != ogAmount) {
            player.setItemInHand(interactionHand, ((FFBucketItem) bucket.getItem()).ff$bucketOfAmount(bucket, bucketAmount));
            player.awardStat(Stats.FILL_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(bucket.getItem()));
            level.playSound(null, blockPos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PLACE, blockPos);
            return result(true);
        }
        return result(false);
    }

    @Unique
    private static
    //#if MC > 12100 || MC == 12001
    InteractionResult
    //#else
    //$$ ItemInteractionResult
    //#endif
    emptyCauldron(BlockState cauldron, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, ItemStack bucket,
                                      ItemStack defaultFilledBucket, SoundEvent soundEvent) {
        int bucketAmount = bucket.is(Items.BUCKET) ? 0 : 8 - bucket.getDamageValue();
        int ogAmount = bucketAmount;
        int cauldronHas = cauldron.getValue(LayeredCauldronBlock.LEVEL);
        switch (cauldronHas) {
            case 1:
                if (bucketAmount <= 6) {
                    bucketAmount += 2;
                    level.setBlockAndUpdate(blockPos, Blocks.CAULDRON.defaultBlockState());
                }
                break;
            case 2:
                if (bucketAmount <= 3) {
                    bucketAmount += 5;
                    level.setBlockAndUpdate(blockPos, Blocks.CAULDRON.defaultBlockState());
                } else if (bucketAmount <= 6) {
                    bucketAmount += 2;
                    level.setBlockAndUpdate(blockPos, cauldron.setValue(LayeredCauldronBlock.LEVEL, 1));
                }
                break;
            case 3:
                if (bucketAmount <= 0) {
                    bucketAmount += 8;
                    level.setBlockAndUpdate(blockPos, Blocks.CAULDRON.defaultBlockState());
                } else if (bucketAmount <= 3) {
                    bucketAmount += 5;
                    level.setBlockAndUpdate(blockPos, cauldron.setValue(LayeredCauldronBlock.LEVEL, 1));
                } else if (bucketAmount <= 6) {
                    bucketAmount += 2;
                    level.setBlockAndUpdate(blockPos, cauldron.setValue(LayeredCauldronBlock.LEVEL, 2));
                }
                break;
            default:
                break;
        }

        if (bucketAmount != ogAmount) {
            player.setItemInHand(interactionHand, ((FFBucketItem) defaultFilledBucket.getItem()).ff$bucketOfAmount(bucket, bucketAmount));
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(bucket.getItem()));
            level.playSound(null, blockPos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PICKUP, blockPos);
            return result(true);
        }
        return result(false);
    }

}
