package traben.waterly.mixin;


import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.ClipContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;


@Mixin(BucketItem.class)
public abstract class MixinBucketItemPickup {

    @ModifyArg(
            method = "use",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BucketItem;getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;"),
            index = 2
    )
    private ClipContext.Fluid mixin(final ClipContext.Fluid par3) {
        if (true && par3 == ClipContext.Fluid.SOURCE_ONLY) { //todo enable flag
            return ClipContext.Fluid.ANY;
        }
        return par3;
    }
}
