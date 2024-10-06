package traben.flowing_fluids.mixin;


import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FFBucketItem;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;


@Mixin(BucketItem.class)
public abstract class MixinBucketItem extends Item implements FFBucketItem {

    @Shadow
    @Final
    private Fluid content;

    public MixinBucketItem(final Properties properties) {
        super(properties);
    }

    @Shadow
    public static ItemStack getEmptySuccessItem(final ItemStack itemStack, final Player player) {
        return null;
    }

    @Shadow
    public abstract void checkExtraContent(@Nullable final Player player, final Level level, final ItemStack itemStack, final BlockPos blockPos);

    @Shadow
    protected abstract void playEmptySound(@Nullable final Player player, final LevelAccessor levelAccessor, final BlockPos blockPos);

    @ModifyArg(
            method = "use",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BucketItem;getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;"),
            index = 2
    )
    private ClipContext.Fluid flowing_fluids$allowAnyFluid(final ClipContext.Fluid par3) {
        if (FlowingFluids.config.enableMod && par3 == ClipContext.Fluid.SOURCE_ONLY) {
            return ClipContext.Fluid.ANY;
        }
        return par3;
    }

    //always place if partial
    @Inject(method = "use", at = @At(value = "HEAD"), cancellable = true)
    private void flowing_fluids$alterBehaviourIfPartial(final Level level, final Player player, final InteractionHand interactionHand, final CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (FlowingFluids.config.enableMod
                && content instanceof FlowingFluid
                && FlowingFluids.config.isFluidAllowed(content)
        ) {//not empty and is flowing
            ItemStack heldBucket = player.getItemInHand(interactionHand);

            //todo infinite logic

            BlockHitResult blockHitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
            if (blockHitResult.getType() == HitResult.Type.MISS || blockHitResult.getType() != HitResult.Type.BLOCK) {
                cir.setReturnValue(InteractionResultHolder.pass(heldBucket));
            } else {
                BlockPos blockPos = blockHitResult.getBlockPos();
                Direction direction = blockHitResult.getDirection();
                BlockPos blockPos2 = blockPos.relative(direction);
                if (level.mayInteract(player, blockPos) && player.mayUseItemAt(blockPos2, direction, heldBucket)) {

                    BlockState blockState = level.getBlockState(blockPos);
                    var fluidState = level.getFluidState(blockPos);
                    BlockPos blockPos3 = (blockState.getBlock() instanceof LiquidBlockContainer && this.content == Fluids.WATER)
                            || (this.content.isSame(fluidState.getType()) && fluidState.getAmount() < 8)
                            ? blockPos : blockPos2;
                    int amount = 8 - heldBucket.getDamageValue();
                    int remainder = this.ff$emptyContents_AndGetRemainder(player, level, blockPos3, blockHitResult, amount);
                    if (remainder != amount) {
                        this.checkExtraContent(player, level, heldBucket, blockPos3);
                        if (player instanceof ServerPlayer) {
                            CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, blockPos3, heldBucket);
                        }

                        player.awardStat(Stats.ITEM_USED.get(this));
                        ItemStack resultBucket;
                        if (remainder == 0) {
                            resultBucket = getEmptySuccessItem(heldBucket, player);
                        } else {
                            resultBucket = ff$bucketOfAmount(heldBucket,remainder);
                        }

                        ItemStack itemStack2 = ItemUtils.createFilledResult(heldBucket, player, resultBucket);
                        cir.setReturnValue(InteractionResultHolder.sidedSuccess(itemStack2, level.isClientSide()));
                    } else {
                        cir.setReturnValue(InteractionResultHolder.fail(heldBucket));
                    }
                } else {
                    cir.setReturnValue(InteractionResultHolder.fail(heldBucket));
                }
            }
        }
        //continue normally
    }

    @Override
    public int ff$emptyContents_AndGetRemainder(@Nullable Player player, Level level, BlockPos blockPos, @Nullable BlockHitResult blockHitResult, int amount) {
        if (!(this.content instanceof FlowingFluid flowingFluid)
                || !FlowingFluids.config.isFluidAllowed(content)) {
            return amount;
        } else {

            var state = level.getBlockState(blockPos);
            var fluidState = level.getFluidState(blockPos);
            boolean canPlaceLiquidInPos = state.canBeReplaced(this.content)
                    || state.isAir()
                    || (this.content.isSame(fluidState.getType()) && fluidState.getAmount() < 8);

            if (!canPlaceLiquidInPos && state.getBlock() instanceof LiquidBlockContainer container) {
//                System.out.println("is liquid block container");
                canPlaceLiquidInPos = amount == 8 && container.canPlaceLiquid(#if MC > MC_20_1 player,#endif level, blockPos, state, this.content);
            }

            if (!canPlaceLiquidInPos) {
//                System.out.println("cannot place liquid");
                if (blockHitResult == null) return amount;
                return this.ff$emptyContents_AndGetRemainder(player, level, blockHitResult.getBlockPos().relative(blockHitResult.getDirection()), null, amount);
            } else if (level.dimensionType().ultraWarm() && this.content.is(FluidTags.WATER)) {
                int i = blockPos.getX();
                int j = blockPos.getY();
                int k = blockPos.getZ();
                level.playSound(player, blockPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);

                for (int l = 0; l < 8; ++l) {
                    level.addParticle(ParticleTypes.LARGE_SMOKE, (double) i + Math.random(), (double) j + Math.random(), (double) k + Math.random(), 0.0, 0.0, 0.0);
                }

                return 0;
            } else {
                if (level.getBlockState(blockPos).getBlock() instanceof final LiquidBlockContainer liquidBlockContainer) {
                    if (this.content == Fluids.WATER) {
                        if (amount != 8) {
                            return amount;
                        }
                        liquidBlockContainer.placeLiquid(level, blockPos, level.getBlockState(blockPos), flowingFluid.getSource(false));
                        this.playEmptySound(player, level, blockPos);
                        return 0;
                    }
                }

                if (!level.isClientSide && level.getBlockState(blockPos).canBeReplaced(this.content) && !level.getBlockState(blockPos).liquid()) {
                    level.destroyBlock(blockPos, true);
                }

                //if (!level.setBlock(blockPos, this.content.defaultFluidState().createLegacyBlock(), 11) && !blockState.getFluidState().isSource()) {
                if (!(content instanceof FlowingFluid)) return amount;

                boolean success;
                int remainder;
                boolean matches = level.getBlockState(blockPos).getFluidState().getType().isSame(this.content);
                if (matches) {
                    int levelAtBlock = level.getBlockState(blockPos).getFluidState().getAmount();
                    int total = levelAtBlock + amount;
                    if (total > 8) {
                        success = level.setBlock(blockPos, FFFluidUtils.getBlockForFluidByAmount(content, 8), 11);
                        remainder = total - 8;
                    } else {
                        success = level.setBlock(blockPos, FFFluidUtils.getBlockForFluidByAmount(content, total), 11);
                        remainder = 0;
                    }
                } else {
                    success = level.setBlock(blockPos, FFFluidUtils.getBlockForFluidByAmount(content, amount), 11);
                    remainder = 0;
                }

                if (success) this.playEmptySound(player, level, blockPos);
                return success ? remainder : amount;
            }
        }
    }

    @Override
    public ItemStack ff$bucketOfAmount(ItemStack originalItemData ,final int amount) {
        if (amount == 0) {
            return Items.BUCKET.getDefaultInstance();
        } else if(!FlowingFluids.config.isFluidAllowed(content)){
            return originalItemData;
        } else {
            var resultBucket = originalItemData.copy();

            #if MC > MC_20_1
            resultBucket.applyComponents(DataComponentMap.builder()
                    .set(DataComponents.DAMAGE, 8 - amount)
                    .set(DataComponents.MAX_DAMAGE, 8).build());
            #else
            resultBucket.setDamageValue(8 - remainder);
            #endif
            return resultBucket;
        }
    }

    #if MC <= MC_20_1

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void getMaxDamage(final CallbackInfo ci) {
        maxDamage = 8;
    }

#endif

    @Override
    public Fluid ff$getFluid() {
        return content;
    }
}
