package traben.flowing_fluids.mixin.mixins.create;

import org.spongepowered.asm.mixin.Mixin;
//#if !CREATE

import traben.flowing_fluids.mixin.CancelTarget;

@Mixin(CancelTarget.class)
public abstract class MixinHosePulley{
} // TODO collapse these better into one
//#elseif FABRIC
//$$
//$$
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//$$ import com.llamalad7.mixinextras.sugar.Local;
//$$ import com.llamalad7.mixinextras.sugar.Share;
//$$ import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
//$$ import com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler;
//$$ import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
//$$ import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
//$$ import com.simibubi.create.foundation.advancement.AllAdvancements;
//$$ import com.simibubi.create.foundation.fluid.FluidHelper;
//$$ import com.simibubi.create.foundation.fluid.SmartFluidTank;
//$$ import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
//$$ import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
//$$ import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
//$$ import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
//$$ import net.minecraft.core.BlockPos;
//$$ import net.minecraft.world.level.material.FlowingFluid;
//$$ import net.minecraft.world.level.material.Fluid;
//$$ import net.minecraft.world.level.material.Fluids;
//$$ import org.spongepowered.asm.mixin.Pseudo;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.Unique;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Constant;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.ModifyConstant;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$ import traben.flowing_fluids.FFFluidUtils;
//$$ import traben.flowing_fluids.FlowingFluids;
//$$
//$$ @SuppressWarnings("UnstableApiUsage")
//$$ @Pseudo
//$$ @Mixin(HosePulleyFluidHandler.class)
//$$ public abstract class MixinHosePulley {
//$$
//$$
//$$     @Shadow private FluidDrainingBehaviour drainer;
//$$
//$$     @Shadow private FluidFillingBehaviour filler;
//$$
//$$     @WrapOperation(method = "extract(Lnet/fabricmc/fabric/api/transfer/v1/fluid/FluidVariant;JLnet/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext;)J",
//$$             at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;getDrainableFluid(Lnet/minecraft/core/BlockPos;)Lio/github/fabricators_of_create/porting_lib/fluids/FluidStack;"))
//$$     private FluidStack ff$modifyWaterRemoval1(final FluidDrainingBehaviour instance, final BlockPos blockPos, final Operation<FluidStack> original) {
//$$         if (FlowingFluids.config.enableMod) {
//$$             var fluid = ((FluidDrainingBehaviourAccessor) drainer).ff$getFluid();
//$$             if (fluid == null) {
//$$                 var newFluid = drainer.getWorld().getFluidState(blockPos).getType();
//$$                 if (FlowingFluids.config.isFluidAllowed(newFluid)) {
//$$                     // mimic fluid set behaviour
//$$                     if (((FluidDrainingBehaviourAccessor) drainer).ff$getFluid() == null)
//$$                         ((FluidDrainingBehaviourAccessor) drainer).ff$setFluid(FluidHelper.convertToStill(newFluid));
//$$                     fluid = newFluid;
//$$                 }
//$$             }
//$$             if (fluid == Fluids.EMPTY) return FluidStack.EMPTY;
//$$             if (FlowingFluids.config.isFluidAllowed(fluid)) {
//$$
//$$                 var source = FluidHelper.convertToStill(fluid);
//$$                 return new FluidStack(source, 81000L);
//$$             }
//$$         }
//$$         return original.call(instance, blockPos);
//$$     }
//$$
//$$     @WrapOperation(method = "extract(Lnet/fabricmc/fabric/api/transfer/v1/fluid/FluidVariant;JLnet/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext;)J",
//$$             at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;pullNext(Lnet/minecraft/core/BlockPos;Lnet/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext;)Z"))
//$$     private boolean ff$modifyWaterRemoval2(final FluidDrainingBehaviour instance, final BlockPos blockPos, final TransactionContext context, final Operation<Boolean> original,
//$$                                            @Share("foundLevels") LocalIntRef foundLevels, @Local(ordinal = 0, argsOnly = true) long maxAmount) {
//$$         foundLevels.set(8);
//$$         if (FlowingFluids.config.enableMod) {
//$$             var world = drainer.getWorld();
//$$             var state = world.getBlockState(blockPos);
//$$             var fluidState = state.getFluidState();
//$$
//$$             if (fluidState.isEmpty()) return false;
//$$
//$$             if (FlowingFluids.config.isFluidAllowed(fluidState) && fluidState.getType() instanceof FlowingFluid flowing) {
//$$                 // mimic advancement behaviour
//$$                 //noinspection UnstableApiUsage
//$$                 TransactionCallback.onSuccess(context, () -> {
//$$                     ((FluidManipulationBehaviourAccessor) drainer).ff$PlayEffect(world, blockPos, flowing, true);
//$$                     drainer.blockEntity.award(AllAdvancements.HOSE_PULLEY);
//$$                     if (drainer.isInfinite() && FluidHelper.isLava(flowing)) {
//$$                         drainer.blockEntity.award(AllAdvancements.HOSE_PULLEY_LAVA);
//$$                     }
//$$                 });
//$$
//$$                 if (FlowingFluids.config.create_infinitePipes || drainer.isInfinite()) {
//$$                     return true;
//$$                 }
//$$
//$$                 // override the existing hose pulley logic as water has physics now
//$$                 var data = FFFluidUtils.collectConnectedFluidAmountAndRemoveAction(world, blockPos,1,8, flowing);
//$$                 var found = data.first();
//$$                 if (found == 0) return false; // nothing found
//$$
//$$                 TransactionCallback.onSuccess(context, () -> {
//$$                     // place water removal behind transaction callback as this may fail
//$$                     data.second().run();
//$$                 });
//$$                 foundLevels.set(found);
//$$                 return true;
//$$             }
//$$         }
//$$         return original.call(instance, blockPos, context);
//$$     }
//$$
//$$     @ModifyConstant(method = "extract(Lnet/fabricmc/fabric/api/transfer/v1/fluid/FluidVariant;JLnet/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext;)J",
//$$             constant = @Constant(longValue = 81000L, ordinal = 1), remap = false)
//$$     private long ff$modifyWaterRemoval3(final long original, @Share("foundLevels") LocalIntRef foundLevels) {
//$$         return waterModified(foundLevels.get());
//$$     }
//$$
//$$     @WrapOperation(method = INSERT_METHOD_FF,
//$$             at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/transfer/FluidFillingBehaviour;tryDeposit(Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Lnet/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext;)Z"))
//$$     private boolean ff$modifyWaterPlacing(final FluidFillingBehaviour instance, final Fluid fluid, final BlockPos currentPos, final TransactionContext context, final Operation<Boolean> original,
//$$                                           @Share("placedLevels") LocalIntRef placedLevels) {
//$$         placedLevels.set(8);
//$$         if (FlowingFluids.config.enableMod
//$$                 && !FlowingFluids.config.create_infinitePipes) {
//$$             if (FlowingFluids.config.isFluidAllowed(fluid)
//$$                 //&& AllConfigs.server().fluids.pipesPlaceFluidSourceBlocks.get()
//$$             ) {
//$$                 var world = filler.getWorld();
//$$                 // override the existing hose pulley logic as water has physics now
//$$                 // don't need to place behind transaction callback as this transaction always succeeds
//$$                 int remainder = FFFluidUtils.addAmountToFluidAtPosWithRemainder(world, currentPos.below(), fluid,8);
//$$                 if (remainder == 8) return false; // nothing placed
//$$
//$$                 placedLevels.set(8 - remainder);
//$$                 return true;
//$$             }
//$$         }
//$$         return original.call(instance, fluid, currentPos, context);
//$$     }
//$$
//$$     private static final String INSERT_METHOD_FF = "insert(Lnet/fabricmc/fabric/api/transfer/v1/fluid/FluidVariant;JLnet/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext;)J";
//$$
//$$     @ModifyConstant(method = INSERT_METHOD_FF, constant = @Constant(longValue = 81000L, ordinal = 1), remap = false)
//$$     private long ff$modifyWaterPlacing1(final long original, @Share("placedLevels") LocalIntRef placedLevels) { return waterModified(placedLevels.get()); }
//$$
//$$     @ModifyConstant(method = INSERT_METHOD_FF, constant = @Constant(longValue = 81000L, ordinal = 2), remap = false)
//$$     private long ff$modifyWaterPlacing2(final long original, @Share("placedLevels") LocalIntRef placedLevels) { return waterModified(placedLevels.get()); }
//$$
//$$     @ModifyConstant(method = INSERT_METHOD_FF, constant = @Constant(longValue = 81000L, ordinal = 3), remap = false)
//$$     private long ff$modifyWaterPlacing3(final long original, @Share("placedLevels") LocalIntRef placedLevels) { return waterModified(placedLevels.get()); }
//$$
//$$     @Unique
//$$     private long waterModified(int level) {
//$$         if (level == 0) return 0;
//$$         if (level == 8) return 81000L;
//$$         return 81000L / 8 * level; // new amount
//$$     }
//$$ }
//#elseif FORGE
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//$$ import com.llamalad7.mixinextras.sugar.Local;
//$$ import com.llamalad7.mixinextras.sugar.Share;
//$$ import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
//$$ import com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler;
//$$ import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
//$$ import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
//$$ import com.simibubi.create.foundation.advancement.AllAdvancements;
//$$ import com.simibubi.create.foundation.fluid.FluidHelper;
//$$ import com.simibubi.create.foundation.fluid.SmartFluidTank;
//$$ import net.minecraft.core.BlockPos;
//$$ import net.minecraft.world.level.material.FlowingFluid;
//$$ import net.minecraft.world.level.material.Fluid;
//$$ import net.minecraft.world.level.material.Fluids;
//$$ import net.minecraftforge.fluids.FluidStack;
//$$ import net.minecraftforge.fluids.capability.IFluidHandler;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Pseudo;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.Unique;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Constant;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.ModifyConstant;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$ import traben.flowing_fluids.FFFluidUtils;
//$$ import traben.flowing_fluids.FlowingFluids;
//$$
//$$ @Pseudo
//$$ @Mixin(HosePulleyFluidHandler.class)
//$$ public abstract class MixinHosePulley {
//$$
//$$
//$$     @Shadow(remap = false) private FluidDrainingBehaviour drainer;
//$$
//$$     @Shadow(remap = false) private FluidFillingBehaviour filler;
//$$
//$$     @WrapOperation(method = "drainInternal", remap = false,
//$$             at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;getDrainableFluid(Lnet/minecraft/core/BlockPos;)Lnet/minecraftforge/fluids/FluidStack;"))
//$$     private FluidStack ff$modifyWaterRemoval1(final FluidDrainingBehaviour instance, final BlockPos blockPos, final Operation<FluidStack> original) {
//$$         if (FlowingFluids.config.enableMod) {
//$$             var fluid = ((FluidDrainingBehaviourAccessor) drainer).ff$getFluid();
//$$             if (fluid == null) {
//$$                 var newFluid = drainer.getWorld().getFluidState(blockPos).getType();
//$$                 if (FlowingFluids.config.isFluidAllowed(newFluid)) {
//$$                     // mimic fluid set behaviour
//$$                     if (((FluidDrainingBehaviourAccessor) drainer).ff$getFluid() == null)
//$$                         ((FluidDrainingBehaviourAccessor) drainer).ff$setFluid(FluidHelper.convertToStill(newFluid));
//$$                     fluid = newFluid;
//$$                 }
//$$             }
//$$             if (fluid == Fluids.EMPTY) return FluidStack.EMPTY;
//$$             if (FlowingFluids.config.isFluidAllowed(fluid)) {
//$$
//$$                 var source = FluidHelper.convertToStill(fluid);
//$$                 return new FluidStack(source, 1000);
//$$             }
//$$         }
//$$         return original.call(instance, blockPos);
//$$     }
//$$
//$$     @WrapOperation(method = "drainInternal", remap = false,
//$$             at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;pullNext(Lnet/minecraft/core/BlockPos;Z)Z"))
//$$     private boolean ff$modifyWaterRemoval2(final FluidDrainingBehaviour instance, final BlockPos blockPos, final boolean simulate, final Operation<Boolean> original,
//$$                                            @Share("foundLevels") LocalIntRef foundLevels, @Local(ordinal = 0, argsOnly = true) int maxAmount) {
//$$         foundLevels.set(8);
//$$         if (FlowingFluids.config.enableMod) {
//$$             var world = drainer.getWorld();
//$$             var state = world.getBlockState(blockPos);
//$$             var fluidState = state.getFluidState();
//$$
//$$             if (fluidState.isEmpty()) return false;
//$$
//$$             if (FlowingFluids.config.isFluidAllowed(fluidState) && fluidState.getType() instanceof FlowingFluid flowing) {
//$$                 if (simulate) return true; // don't change the world
//$$
//$$                 // mimic advancement behaviour
//$$                 ((FluidManipulationBehaviourAccessor) drainer).ff$PlayEffect(world, blockPos, flowing, true);
//$$                 drainer.blockEntity.award(AllAdvancements.HOSE_PULLEY);
//$$                 if (drainer.isInfinite() && FluidHelper.isLava(flowing)) {
//$$                     drainer.blockEntity.award(AllAdvancements.HOSE_PULLEY_LAVA);
//$$                 }
//$$
//$$
//$$                 if (FlowingFluids.config.create_infinitePipes || drainer.isInfinite()) {
//$$                     return true;
//$$                 }
//$$
//$$                 // override the existing hose pulley logic as water has physics now
//$$                 var data = FFFluidUtils.collectConnectedFluidAmountAndRemoveAction(world, blockPos,1,8, flowing);
//$$                 var found = data.first();
//$$                 if (found == 0) return false; // nothing found
//$$
//$$                 data.second().run();
//$$
//$$                 foundLevels.set(found);
//$$                 return true;
//$$             }
//$$         }
//$$         return original.call(instance, blockPos, simulate);
//$$     }
//$$
//$$     @ModifyConstant(method = "drainInternal",
//$$             constant = @Constant(intValue = 1000, ordinal = 1), remap = false)
//$$     private int ff$modifyWaterRemoval3(final int original, @Share("foundLevels") LocalIntRef foundLevels) {
//$$         return waterModified(foundLevels.get());
//$$     }
//$$
//$$     @WrapOperation(method = INSERT_METHOD_FF, remap = false,
//$$             at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/transfer/FluidFillingBehaviour;tryDeposit(Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Z)Z"))
//$$     private boolean ff$modifyWaterPlacing(final FluidFillingBehaviour instance, final Fluid fluid, final BlockPos blockPos, final boolean b, final Operation<Boolean> original,
//$$                                           @Share("placedLevels") LocalIntRef placedLevels) {
//$$         placedLevels.set(8);
//$$         if (FlowingFluids.config.enableMod
//$$                 && !FlowingFluids.config.create_infinitePipes) {
//$$             if (FlowingFluids.config.isFluidAllowed(fluid)
//$$                 //&& AllConfigs.server().fluids.pipesPlaceFluidSourceBlocks.get()
//$$             ) {
//$$                 var world = filler.getWorld();
//$$                 // override the existing hose pulley logic as water has physics now
//$$                 // don't need to place behind transaction callback as this transaction always succeeds
//$$                 int remainder = FFFluidUtils.addAmountToFluidAtPosWithRemainder(world, blockPos.below(), fluid,8);
//$$                 if (remainder == 8) return false; // nothing placed
//$$
//$$                 placedLevels.set(8 - remainder);
//$$                 return true;
//$$             }
//$$         }
//$$         return original.call(instance, fluid, blockPos, b);
//$$     }
//$$
//$$     private static final String INSERT_METHOD_FF = "fill";
//$$
//$$     @ModifyConstant(method = INSERT_METHOD_FF, constant = @Constant(intValue = 1000, ordinal = 1), remap = false)
//$$     private int ff$modifyWaterPlacing1(final int original, @Share("placedLevels") LocalIntRef placedLevels) { return waterModified(placedLevels.get()); }
//$$
//$$     @ModifyConstant(method = INSERT_METHOD_FF, constant = @Constant(intValue = 1000, ordinal = 2), remap = false)
//$$     private int ff$modifyWaterPlacing2(final int original, @Share("placedLevels") LocalIntRef placedLevels) { return waterModified(placedLevels.get()); }
//$$
//$$     @Inject(method = INSERT_METHOD_FF, at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/FluidStack;shrink(I)V"), remap = false)
//$$     private void ff$modifyWaterPlacing3(final FluidStack resource, final IFluidHandler.FluidAction action, final CallbackInfoReturnable<Integer> cir,
//$$                                         @Share("placedLevels") LocalIntRef placedLevels, @Local(name = "diff") LocalIntRef diff) {
//$$         if (placedLevels.get() == 8) return; // nothing to change
//$$
//$$         int newDiff = diff.get() + 1000; // offset the upcoming non injected reduction
//$$         diff.set(newDiff - waterModified(placedLevels.get()));
//$$     }
//$$
//$$     @Unique
//$$     private int waterModified(int level) {
//$$         if (level == 0) return 0;
//$$         if (level == 8) return 1000;
//$$         return 1000 / 8 * level; // new amount
//$$     }
//$$ }
//#else
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//$$ import com.llamalad7.mixinextras.sugar.Local;
//$$ import com.llamalad7.mixinextras.sugar.Share;
//$$ import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
//$$ import com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler;
//$$ import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
//$$ import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
//$$ import com.simibubi.create.foundation.advancement.AllAdvancements;
//$$ import com.simibubi.create.foundation.fluid.FluidHelper;
//$$ import com.simibubi.create.foundation.fluid.SmartFluidTank;
//$$ import net.minecraft.core.BlockPos;
//$$ import net.minecraft.world.level.material.FlowingFluid;
//$$ import net.minecraft.world.level.material.Fluid;
//$$ import net.minecraft.world.level.material.Fluids;
//$$ import net.neoforged.neoforge.fluids.FluidStack;
//$$ import net.neoforged.neoforge.fluids.capability.IFluidHandler;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Pseudo;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.Unique;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Constant;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.ModifyConstant;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$ import traben.flowing_fluids.FFFluidUtils;
//$$ import traben.flowing_fluids.FlowingFluids;
//$$
//$$ @Pseudo
//$$ @Mixin(HosePulleyFluidHandler.class)
//$$ public abstract class MixinHosePulley {
//$$
//$$
//$$     @Shadow(remap = false) private FluidDrainingBehaviour drainer;
//$$
//$$     @Shadow(remap = false) private FluidFillingBehaviour filler;
//$$
//$$     @WrapOperation(method = "drainInternal", remap = false,
//$$             at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;getDrainableFluid(Lnet/minecraft/core/BlockPos;)Lnet/neoforged/neoforge/fluids/FluidStack;"))
//$$     private FluidStack ff$modifyWaterRemoval1(final FluidDrainingBehaviour instance, final BlockPos blockPos, final Operation<FluidStack> original) {
//$$         if (FlowingFluids.config.enableMod) {
//$$             var fluid = ((FluidDrainingBehaviourAccessor) drainer).ff$getFluid();
//$$             if (fluid == null) {
//$$                 var newFluid = drainer.getWorld().getFluidState(blockPos).getType();
//$$                 if (FlowingFluids.config.isFluidAllowed(newFluid)) {
//$$                     // mimic fluid set behaviour
//$$                     if (((FluidDrainingBehaviourAccessor) drainer).ff$getFluid() == null)
//$$                         ((FluidDrainingBehaviourAccessor) drainer).ff$setFluid(FluidHelper.convertToStill(newFluid));
//$$                     fluid = newFluid;
//$$                 }
//$$             }
//$$             if (fluid == Fluids.EMPTY) return FluidStack.EMPTY;
//$$             if (FlowingFluids.config.isFluidAllowed(fluid)) {
//$$
//$$                 var source = FluidHelper.convertToStill(fluid);
//$$                 return new FluidStack(source, 1000);
//$$             }
//$$         }
//$$         return original.call(instance, blockPos);
//$$     }
//$$
//$$     @WrapOperation(method = "drainInternal", remap = false,
//$$             at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;pullNext(Lnet/minecraft/core/BlockPos;Z)Z"))
//$$     private boolean ff$modifyWaterRemoval2(final FluidDrainingBehaviour instance, final BlockPos blockPos, final boolean simulate, final Operation<Boolean> original,
//$$                                            @Share("foundLevels") LocalIntRef foundLevels, @Local(ordinal = 0, argsOnly = true) int maxAmount) {
//$$         foundLevels.set(8);
//$$         if (FlowingFluids.config.enableMod) {
//$$             var world = drainer.getWorld();
//$$             var state = world.getBlockState(blockPos);
//$$             var fluidState = state.getFluidState();
//$$
//$$             if (fluidState.isEmpty()) return false;
//$$
//$$             if (FlowingFluids.config.isFluidAllowed(fluidState) && fluidState.getType() instanceof FlowingFluid flowing) {
//$$                 if (simulate) return true; // don't change the world
//$$
//$$                 // mimic advancement behaviour
//$$                 ((FluidManipulationBehaviourAccessor) drainer).ff$PlayEffect(world, blockPos, flowing, true);
//$$                 drainer.blockEntity.award(AllAdvancements.HOSE_PULLEY);
//$$                 if (drainer.isInfinite() && FluidHelper.isLava(flowing)) {
//$$                     drainer.blockEntity.award(AllAdvancements.HOSE_PULLEY_LAVA);
//$$                 }
//$$
//$$
//$$                 if (FlowingFluids.config.create_infinitePipes || drainer.isInfinite()) {
//$$                     return true;
//$$                 }
//$$
//$$                 // override the existing hose pulley logic as water has physics now
//$$                 var data = FFFluidUtils.collectConnectedFluidAmountAndRemoveAction(world, blockPos,1,8, flowing);
//$$                 var found = data.first();
//$$                 if (found == 0) return false; // nothing found
//$$
//$$                 data.second().run();
//$$
//$$                 foundLevels.set(found);
//$$                 return true;
//$$             }
//$$         }
//$$         return original.call(instance, blockPos, simulate);
//$$     }
//$$
//$$     @ModifyConstant(method = "drainInternal",
//$$             constant = @Constant(intValue = 1000, ordinal = 1), remap = false)
//$$     private int ff$modifyWaterRemoval3(final int original, @Share("foundLevels") LocalIntRef foundLevels) {
//$$         return waterModified(foundLevels.get());
//$$     }
//$$
//$$     @WrapOperation(method = INSERT_METHOD_FF, remap = false,
//$$             at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/transfer/FluidFillingBehaviour;tryDeposit(Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Z)Z"))
//$$     private boolean ff$modifyWaterPlacing(final FluidFillingBehaviour instance, final Fluid fluid, final BlockPos blockPos, final boolean b, final Operation<Boolean> original,
//$$                                           @Share("placedLevels") LocalIntRef placedLevels) {
//$$         placedLevels.set(8);
//$$         if (FlowingFluids.config.enableMod
//$$                 && !FlowingFluids.config.create_infinitePipes) {
//$$             if (FlowingFluids.config.isFluidAllowed(fluid)
//$$                 //&& AllConfigs.server().fluids.pipesPlaceFluidSourceBlocks.get()
//$$             ) {
//$$                 var world = filler.getWorld();
//$$                 // override the existing hose pulley logic as water has physics now
//$$                 // don't need to place behind transaction callback as this transaction always succeeds
//$$                 int remainder = FFFluidUtils.addAmountToFluidAtPosWithRemainder(world, blockPos.below(), fluid,8);
//$$                 if (remainder == 8) return false; // nothing placed
//$$
//$$                 placedLevels.set(8 - remainder);
//$$                 return true;
//$$             }
//$$         }
//$$         return original.call(instance, fluid, blockPos, b);
//$$     }
//$$
//$$     private static final String INSERT_METHOD_FF = "fill";
//$$
//$$     @ModifyConstant(method = INSERT_METHOD_FF, constant = @Constant(intValue = 1000, ordinal = 1), remap = false)
//$$     private int ff$modifyWaterPlacing1(final int original, @Share("placedLevels") LocalIntRef placedLevels) { return waterModified(placedLevels.get()); }
//$$
//$$     @ModifyConstant(method = INSERT_METHOD_FF, constant = @Constant(intValue = 1000, ordinal = 2), remap = false)
//$$     private int ff$modifyWaterPlacing2(final int original, @Share("placedLevels") LocalIntRef placedLevels) { return waterModified(placedLevels.get()); }
//$$
//$$     @Inject(method = INSERT_METHOD_FF, at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/fluids/FluidStack;shrink(I)V"), remap = false)
//$$     private void ff$modifyWaterPlacing3(final FluidStack resource, final IFluidHandler.FluidAction action, final CallbackInfoReturnable<Integer> cir,
//$$                                         @Share("placedLevels") LocalIntRef placedLevels, @Local(name = "diff") LocalIntRef diff) {
//$$         if (placedLevels.get() == 8) return; // nothing to change
//$$
//$$         int newDiff = diff.get() + 1000; // offset the upcoming non injected reduction
//$$         diff.set(newDiff - waterModified(placedLevels.get()));
//$$     }
//$$
//$$     @Unique
//$$     private int waterModified(int level) {
//$$         if (level == 0) return 0;
//$$         if (level == 8) return 1000;
//$$         return 1000 / 8 * level; // new amount
//$$     }
//$$ }
//#endif