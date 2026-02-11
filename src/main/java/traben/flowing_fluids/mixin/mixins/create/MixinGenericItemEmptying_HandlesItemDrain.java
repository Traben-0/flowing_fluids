package traben.flowing_fluids.mixin.mixins.create;

import org.spongepowered.asm.mixin.Mixin;
//#if !CREATE

import traben.flowing_fluids.mixin.CancelTarget;

@Mixin(CancelTarget.class)
public abstract class MixinGenericItemEmptying_HandlesItemDrain {
}
//#else
//$$
//$$ import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
//$$ import org.spongepowered.asm.mixin.Pseudo;
//$$ import org.spongepowered.asm.mixin.Unique;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#if FABRIC
//$$ import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
//#elseif FORGE
//$$ import net.minecraftforge.fluids.FluidStack;
//#else
//$$ import net.neoforged.neoforge.fluids.FluidStack;
//#endif
//$$ import net.createmod.catnip.data.Pair;
//$$ import net.minecraft.world.item.ItemStack;
//$$ import net.minecraft.world.level.Level;
//$$ import traben.flowing_fluids.FlowingFluids;
//$$ import net.minecraft.world.item.BottleItem;
//$$ import net.minecraft.world.item.BucketItem;
//$$
//$$ @Pseudo
//$$ @Mixin(GenericItemEmptying.class)
//$$ public abstract class MixinGenericItemEmptying_HandlesItemDrain {
//$$
//$$     @Inject(method = "emptyItem", at = @At("RETURN"), remap = false)
//$$     private static void ff$onEmptyItem(Level level, ItemStack stack, boolean simulate, CallbackInfoReturnable<Pair<FluidStack, ItemStack>> cir) {
//$$         if (level == null
//$$                 || !FlowingFluids.config.enableMod
//$$                 || !FlowingFluids.config.create_waterWheelMode.needsFlow()
//$$                 || !FlowingFluids.config.isFluidAllowed(cir.getReturnValue().getFirst().getFluid())
//$$         ) return;
//$$
//$$         if (stack.getItem() instanceof BucketItem) {
//$$             int damage = stack.getDamageValue();
//$$             if (damage == 0) return;
//$$             cir.getReturnValue().getFirst().setAmount(waterModified(8 - damage));
//$$         } else if (stack.getItem() instanceof BottleItem) {
//$$             int damage = stack.getDamageValue();
//$$             if (damage == 0) return;
//$$             cir.getReturnValue().getFirst().setAmount(3 - damage);
//$$         }
//$$     }
//$$
    //#if FABRIC
    //$$ @Unique
    //$$ private static long waterModified(int level) {
    //$$     if (level == 0) return 0;
    //$$     if (level == 8) return 81000L;
    //$$     return 81000L / 8 * level; // new amount
    //$$ }
    //#else
    //$$ @Unique
    //$$ private static int waterModified(int level) {
    //$$     if (level == 0) return 0;
    //$$     if (level == 8) return 1000;
    //$$     return 1000 / 8 * level; // new amount
    //$$ }
    //#endif
//$$ }
//#endif