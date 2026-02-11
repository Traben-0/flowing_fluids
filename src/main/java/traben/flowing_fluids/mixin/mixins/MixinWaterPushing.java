package traben.flowing_fluids.mixin.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
//#if MC > 12100
import net.minecraft.world.entity.vehicle.AbstractBoat;
//#else
//$$ import net.minecraft.world.entity.vehicle.Boat;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import traben.flowing_fluids.FlowingFluids;

@Mixin(Entity.class)
public class MixinWaterPushing {

    //#if FABRIC
    private static final String METHOD_NAME = "updateFluidHeightAndDoFluidPushing";
    //#elseif FORGE
    //$$ private static final String METHOD_NAME = "updateFluidHeightAndDoFluidPushing(Ljava/util/function/Predicate;)V";
    //#elseif NEOFORGE && MC >= 1.21.11
    //$$ private static final String METHOD_NAME = "updateFluidHeightAndDoFluidPushing(Z)V";
    //#else
    //$$ private static final String METHOD_NAME = "updateFluidHeightAndDoFluidPushing()V";
    //#endif

    @ModifyExpressionValue(
            method = METHOD_NAME,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isPushedByFluid()Z")

    )
    private boolean ff$isPushed(final boolean original) {
        if (!original) return false;

        if (FlowingFluids.config.enableMod) {
            // if it doesn't anyway just take that
            Object entity = this;
            if (entity instanceof Player) return FlowingFluids.config.waterFlowAffectsPlayers;
            if (entity instanceof
                    //#if MC > 12100
                    AbstractBoat
                    //#else
                    //$$ Boat
                    //#endif
            ) return FlowingFluids.config.waterFlowAffectsBoats;
            if (entity instanceof ItemEntity) return FlowingFluids.config.waterFlowAffectsItems;

            return FlowingFluids.config.waterFlowAffectsEntities;
        }
        return true;
    }
}
