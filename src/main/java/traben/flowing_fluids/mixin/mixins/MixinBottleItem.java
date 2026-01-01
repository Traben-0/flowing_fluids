package traben.flowing_fluids.mixin.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
//#if MC > 12100
import net.minecraft.world.InteractionResult;
//#else
//$$ import net.minecraft.world.InteractionResultHolder;
//#endif
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

import java.util.List;

@Mixin(BottleItem.class)
public class MixinBottleItem {

    @ModifyArg(
            method = "use",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BottleItem;getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;"),
            index = 2
    )
    private ClipContext.Fluid flowing_fluids$allowAnyFluid(final ClipContext.Fluid par3) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.isWaterAllowed()
                && par3 == ClipContext.Fluid.SOURCE_ONLY) {
            return ClipContext.Fluid.ANY;
        }
        return par3;
    }

    @Inject(
            method = "use",
            at = @At(value = "INVOKE",
                    //#if MC>=12105
                    target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
                    //#else
                    //$$ target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
                    //#endif
                    ordinal = 1,
                    shift = At.Shift.BEFORE),
            cancellable = true)
    private void ff$drainWater(
                                    //#if MC > 12100
                                    CallbackInfoReturnable<InteractionResult> cir,
                                    //#else
                                    //$$ CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir,
                                    //$$ @Local final ItemStack itemStack,
                                    //#endif
                               @Local(argsOnly = true) final Level level, @Local final BlockPos blockPos) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.isWaterAllowed()){
            int foundAmount = FFFluidUtils.collectConnectedFluidAmountAndRemove(level, blockPos, 2, 3, Fluids.WATER);
            if (foundAmount == 0) {
                cir.setReturnValue(
                        //#if MC > 12100
                        InteractionResult.PASS
                        //#else
                        //$$ InteractionResultHolder.pass(itemStack)
                        //#endif
                        );
            }
        }
    }
}
