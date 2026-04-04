package traben.flowing_fluids.mixin.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.flowing_fluids.FFBucketItem;
import traben.flowing_fluids.FlowingFluids;

//#if MC >= 260100
//$$ import static net.minecraft.core.cauldron.CauldronInteractions.*;
//#else
import static net.minecraft.core.cauldron.CauldronInteraction.*;
//#endif

@Mixin(Bootstrap.class)
public abstract class MixinCauldronInteraction {

    //#if MC >= 260100
    //$$ @Inject(method = "bootStrap", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/cauldron/CauldronInteractions;bootStrap()V", shift =  At.Shift.AFTER))
    //#else
    @Inject(method = "bootStrap", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/cauldron/CauldronInteraction;bootStrap()V", shift =  At.Shift.AFTER))
    //#endif
    private static void ff$bootStrap(CallbackInfo ci) {

        // todo api for mods, will require making these real generic somehow
        //#if MC >= 260100
        //$$ { final var prev = EMPTY.get(Items.LAVA_BUCKET.getDefaultInstance());
        //$$ ff$put(EMPTY, Items.LAVA_BUCKET, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#else
        EMPTY
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.LAVA_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#endif
            if (!allow(Fluids.LAVA)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            if (!level.isClientSide()) {
                if (isUnderWater(level, blockPos)
                        // only full lava buckets can fill cauldrons
                        || itemStack.getDamageValue() != 0)
                    return result(false);
                player.setItemInHand(interactionHand, Items.BUCKET.getDefaultInstance());
                player.awardStat(Stats.FILL_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                level.setBlockAndUpdate(blockPos, Blocks.LAVA_CAULDRON.defaultBlockState());
                level.playSound(null, blockPos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PLACE, blockPos);
                return result(true);
            }
            return result(true);
        //#if MC >= 260100
        //$$ }); }
        //#else
        });
        //#endif
        //#if MC >= 260100
        //$$ { final var prev = EMPTY.get(Items.WATER_BUCKET.getDefaultInstance());
        //$$ ff$put(EMPTY, Items.WATER_BUCKET, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#else
        EMPTY
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.WATER_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#endif
            if (!allow(Fluids.WATER)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            if (!level.isClientSide()) {
                return fillCauldron(blockState, level, blockPos, player, interactionHand, itemStack,
                        Blocks.WATER_CAULDRON.defaultBlockState());
            }
            return result(true);
        //#if MC >= 260100
        //$$ }); }
        //#else
        });
        //#endif



        //#if MC >= 260100
        //$$ { final var prev = WATER.get(Items.WATER_BUCKET.getDefaultInstance());
        //$$ ff$put(WATER, Items.WATER_BUCKET, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#else
        WATER
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.WATER_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#endif
            if (!allow(Fluids.WATER)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            if (!level.isClientSide()) {
                if (fillCauldron(blockState, level, blockPos, player, interactionHand, itemStack,
                        Blocks.WATER_CAULDRON.defaultBlockState()) != result(true)) {
                    return emptyCauldron(blockState, level, blockPos, player, interactionHand, itemStack,
                            Items.WATER_BUCKET.getDefaultInstance(), SoundEvents.BUCKET_FILL);
                }
            }
            return result(true);
        //#if MC >= 260100
        //$$ }); }
        //#else
        });
        //#endif
        //#if MC >= 260100
        //$$ { final var prev = WATER.get(Items.LAVA_BUCKET.getDefaultInstance());
        //$$ ff$put(WATER, Items.LAVA_BUCKET, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#else
        WATER
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.LAVA_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#endif
            if (!allow(Fluids.LAVA)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            return result(false);
        //#if MC >= 260100
        //$$ }); }
        //#else
        });
        //#endif
        //#if MC >= 260100
        //$$ { final var prev = WATER.get(Items.POWDER_SNOW_BUCKET.getDefaultInstance());
        //$$ ff$put(WATER, Items.POWDER_SNOW_BUCKET, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#else
        WATER
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.POWDER_SNOW_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#endif
            if (!allow()) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            return result(false);
        //#if MC >= 260100
        //$$ }); }
        //#else
        });
        //#endif
        //#if MC >= 260100
        //$$ { final var prev = WATER.get(Items.BUCKET.getDefaultInstance());
        //$$ ff$put(WATER, Items.BUCKET, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#else
        WATER
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#endif
            if (!allow(Fluids.WATER)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            if (!level.isClientSide()) {
                return emptyCauldron(blockState, level, blockPos, player, interactionHand, itemStack,
                            Items.WATER_BUCKET.getDefaultInstance(), SoundEvents.BUCKET_FILL);
            }
            return result(true);
        //#if MC >= 260100
        //$$ }); }
        //#else
        });
        //#endif


        //#if MC >= 260100
        //$$ { final var prev = LAVA.get(Items.WATER_BUCKET.getDefaultInstance());
        //$$ ff$put(LAVA, Items.WATER_BUCKET, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#else
        LAVA
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.WATER_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#endif
            if (!allow(Fluids.WATER)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            return result(false);
        //#if MC >= 260100
        //$$ }); }
        //#else
        });
        //#endif
        //#if MC >= 260100
        //$$ { final var prev = LAVA.get(Items.POWDER_SNOW_BUCKET.getDefaultInstance());
        //$$ ff$put(LAVA, Items.POWDER_SNOW_BUCKET, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#else
        LAVA
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.POWDER_SNOW_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#endif
            if (!allow()) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            return result(false);
        //#if MC >= 260100
        //$$ }); }
        //#else
        });
        //#endif



        //#if MC >= 260100
        //$$ { final var prev = POWDER_SNOW.get(Items.LAVA_BUCKET.getDefaultInstance());
        //$$ ff$put(POWDER_SNOW, Items.LAVA_BUCKET, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#else
        POWDER_SNOW
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.LAVA_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#endif
            if (!allow(Fluids.LAVA)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            return result(false);
        //#if MC >= 260100
        //$$ }); }
        //#else
        });
        //#endif
        //#if MC >= 260100
        //$$ { final var prev = POWDER_SNOW.get(Items.WATER_BUCKET.getDefaultInstance());
        //$$ ff$put(POWDER_SNOW, Items.WATER_BUCKET, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#else
        POWDER_SNOW
                //#if MC > 12001
                .map()
                //#endif
                .compute(Items.WATER_BUCKET, (k, prev) ->  (blockState, level, blockPos, player, interactionHand, itemStack) -> {
        //#endif
            if (!allow(Fluids.WATER)) return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            return result(false);
        //#if MC >= 260100
        //$$ }); }
        //#else
        });
        //#endif
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
        int cauldronNeeds = cauldron.is(Blocks.CAULDRON) ? 3 : 3 - cauldron.getValue(LayeredCauldronBlock.LEVEL);
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
                    bucketAmount -= 8;
                    level.setBlockAndUpdate(blockPos, defaultFilledCauldronState.setValue(LayeredCauldronBlock.LEVEL, 3));
                } else if (bucketAmount >= 5) {
                    bucketAmount -= 5;
                    level.setBlockAndUpdate(blockPos, defaultFilledCauldronState.setValue(LayeredCauldronBlock.LEVEL, 2));
                } else if (bucketAmount >= 2) {
                    bucketAmount -= 2;
                    level.setBlockAndUpdate(blockPos, defaultFilledCauldronState.setValue(LayeredCauldronBlock.LEVEL, 1));
                }
                break;
            default:
                break;
        }

        System.out.println("Bucket amount after: " + bucketAmount + " (was " + ogAmount + ") cauldron was " + (cauldron.is(Blocks.CAULDRON) ? 0 : cauldron.getValue(LayeredCauldronBlock.LEVEL)) + " level, is now " + (level.getBlockState(blockPos).is(Blocks.CAULDRON) ? 0 : level.getBlockState(blockPos).getValue(LayeredCauldronBlock.LEVEL)));
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
        System.out.println("Bucket amount after: " + bucketAmount + " (was " + ogAmount + ") cauldron was " + cauldronHas + " level, is now " + (level.getBlockState(blockPos).is(Blocks.CAULDRON) ? 0 : level.getBlockState(blockPos).getValue(LayeredCauldronBlock.LEVEL)));
        if (bucketAmount != ogAmount) {
            player.setItemInHand(interactionHand, ((FFBucketItem) defaultFilledBucket.getItem()).ff$bucketOfAmount(defaultFilledBucket, bucketAmount));
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(bucket.getItem()));
            level.playSound(null, blockPos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PICKUP, blockPos);
            return result(true);
        }
        return result(false);
    }


    //#if MC >= 260100
    //$$ @Unique
    //$$ private static void ff$put(Object dispatcher, net.minecraft.world.item.Item item, net.minecraft.core.cauldron.CauldronInteraction interaction) {
    //$$     try {
    //$$         java.lang.reflect.Method m = dispatcher.getClass().getDeclaredMethod("put", net.minecraft.world.item.Item.class, net.minecraft.core.cauldron.CauldronInteraction.class);
    //$$         m.setAccessible(true);
    //$$         m.invoke(dispatcher, item, interaction);
    //$$     } catch (Exception e) { e.printStackTrace(); }
    //$$ }
    //#endif
}