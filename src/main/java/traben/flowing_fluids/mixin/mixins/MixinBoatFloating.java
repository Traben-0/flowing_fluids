package traben.flowing_fluids.mixin.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import traben.flowing_fluids.FlowingFluids;

//#if MC > 12100
import net.minecraft.world.entity.vehicle.AbstractBoat;

@Mixin(AbstractBoat.class)
//#else
//$$ @Mixin(Boat.class)
//#endif

public abstract class MixinBoatFloating extends Entity {

    public MixinBoatFloating() {
        //noinspection DataFlowIssue
        super(null, null);
    }

    @WrapOperation(method = "getStatus", at = @At(value = "INVOKE",
    //#if MC > 12100
            target = "Lnet/minecraft/world/entity/vehicle/AbstractBoat;isUnderwater()Lnet/minecraft/world/entity/vehicle/AbstractBoat$Status;"))
    private Boat.Status ff$float1(final AbstractBoat instance, final Operation<AbstractBoat.Status> original)
    //#else
    //$$         target = "Lnet/minecraft/world/entity/vehicle/Boat;isUnderwater()Lnet/minecraft/world/entity/vehicle/Boat$Status;"))
    //$$ private Boat.Status ff$float1(final Boat instance, final Operation<Boat.Status> original)
    //#endif
        {
        if (FlowingFluids.config.enableMod && !FlowingFluids.config.waterFlowAffectsBoats) {
            return null; // force it to be null so we do checkInWater() and have floating boats
        }
        return original.call(instance);
    }

    @ModifyExpressionValue(method = "checkInWater", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;ceil(D)I", ordinal = 1))
    private int ff$float2(final int original, @Local(ordinal = 0) AABB aabb) {
        if (FlowingFluids.config.enableMod && !FlowingFluids.config.waterFlowAffectsBoats) {
            return Mth.ceil(aabb.maxY); // use the actual boat height for checking
        }
        return original;
    }
}
