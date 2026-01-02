package traben.flowing_fluids.mixin.mixins.create;

import org.spongepowered.asm.mixin.Mixin;
//#if !CREATE

import traben.flowing_fluids.mixin.CancelTarget;

@Mixin(CancelTarget.class)
public abstract class MixinWaterWheel{
}
//#else
//$$
//$$ import net.minecraft.world.level.block.Blocks;
//$$ import net.minecraft.world.level.block.BubbleColumnBlock;
//$$ import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
//$$ import net.minecraft.core.Direction;
//$$ import net.minecraft.world.level.material.Fluid;
//$$ import net.minecraft.world.phys.Vec3;
//$$ import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
//$$ import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity;
//$$ import com.simibubi.create.foundation.advancement.AllAdvancements;
//$$ import net.minecraft.core.BlockPos;
//$$ import net.minecraft.tags.BiomeTags;
//$$ import net.minecraft.world.level.block.entity.BlockEntityType;
//$$ import net.minecraft.world.level.block.state.BlockState;
//$$ import net.minecraft.world.level.material.Fluids;
//$$ import org.spongepowered.asm.mixin.Pseudo;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$ import traben.flowing_fluids.FFFluidUtils;
//$$ import traben.flowing_fluids.FlowingFluids;
//$$ import traben.flowing_fluids.IFFFlowListener;
//$$ import traben.flowing_fluids.config.FFConfig;
//$$ import it.unimi.dsi.fastutil.Pair;
//$$
//$$ import java.util.LinkedList;
//$$ import java.util.List;
//$$ import java.util.Map;
//$$ import java.util.Set;
//$$
//$$ @Pseudo
//$$ @Mixin(WaterWheelBlockEntity.class)
//$$ public abstract class MixinWaterWheel extends GeneratingKineticBlockEntity implements IFFFlowListener {
//$$
//$$
//$$     @Shadow(remap = false) protected abstract Set<BlockPos> getOffsetsToCheck();
//$$
//$$     @Shadow(remap = false) public abstract void setFlowScoreAndUpdate(final int score);
//$$
//$$     @Shadow
//$$     protected abstract Direction.Axis getAxis();
//$$
//$$     public MixinWaterWheel(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
//$$         super(type, pos, state);
//$$     }
//$$
//$$     @Inject(method = "determineAndApplyFlowScore",
//$$             at = @At(value = "HEAD"),
//$$             cancellable = true, remap = false)
//$$     private void ff$modifyWaterCheck(final CallbackInfo ci) {
//$$         try {
//$$             //leave if no change or level is null
//$$             if (!FlowingFluids.config.enableMod
//$$                     || FlowingFluids.config.create_waterWheelMode == FFConfig.CreateWaterWheelMode.REQUIRE_FLOW
//$$                     || level == null
//$$             ) return;
//$$
//$$             //if REQUIRE_FLOW_OR_RIVER, check for river else fallback to regular flow check
//$$             if (FlowingFluids.config.create_waterWheelMode.isRiver()
//$$                     && !(level.getBiome(worldPosition).is(BiomeTags.IS_RIVER)
//$$                     && Math.abs(worldPosition.getY() - FFFluidUtils.seaLevel(level)) <= 5)
//$$             ) {
//$$                 if (FlowingFluids.config.create_waterWheelMode.isRiverOnly()) {
//$$                     ci.cancel();
//$$                     this.setFlowScoreAndUpdate(0);
//$$                 }
//$$                 return;
//$$             }
//$$
//$$             //from here onwards the only possibilities are
//$$             // - REQUIRE_FULL_FLUID
//$$             // - REQUIRE_FLUID
//$$             // - REQUIRE_FLOW_OR_RIVER and are in a river biome near sea level
//$$             // - RIVER_ONLY and are in a river biome near sea level
//$$             //all of these only require simple water count checks and don't need complex flow checks
//$$
//$$
//$$             //the mixin will now always cancel the default method
//$$             ci.cancel();
//$$
//$$             //search for valid fluids
//$$             boolean lava = false;
//$$             int score = 0;
//$$
//$$             //settings for alternative checks
//$$             boolean fluidCanBeAnyHeight = !FlowingFluids.config.create_waterWheelMode.needsFullFluid();
//$$             boolean oppositeSpin = FlowingFluids.config.create_waterWheelMode.isCounterSpin();
//$$             boolean alwaysSpin = FlowingFluids.config.create_waterWheelMode.always();
//$$
//$$             for (final BlockPos blockPos : this.getOffsetsToCheck()) {
//$$                 BlockPos checkPos = blockPos.offset(this.worldPosition);
//$$                 var fState = level.getFluidState(checkPos);
//$$                 lava |= fState.getType().isSame(Fluids.LAVA);
//$$
//$$                 if (alwaysSpin || (!fState.isEmpty() && (fluidCanBeAnyHeight || fState.getAmount() == 8))) {
//$$                     score += oppositeSpin ? -1 : 1;
//$$                 }
//$$             }
//$$
//$$
//$$             //end setters from super method
//$$             if (score != 0 && !this.level.isClientSide()) {
//$$                 this.award(lava ? AllAdvancements.LAVA_WHEEL : AllAdvancements.WATER_WHEEL);
//$$             }
//$$
//$$             this.setFlowScoreAndUpdate(score);
//$$
//$$         }catch (final Exception ignored){}
//$$     }
//$$
//$$     private final Map<BlockPos, Pair<Vec3, Long>> recentDirections = new Object2ObjectOpenHashMap<>();
//$$
//$$     @Override
//$$     public void ff$acceptRecentFlow(BlockPos pos, Direction direction, Fluid fluid, boolean downAlso) {
//$$         if (level == null || !getOffsetsToCheck().contains(pos.subtract(worldPosition))) return;
//$$         if (fluid.isSame(Fluids.EMPTY)) {
//$$             recentDirections.remove(pos);
//$$             return;
//$$         }
//$$         recentDirections.put(pos, Pair.of(
//$$                         new Vec3(direction.getStepX(), direction == Direction.DOWN || downAlso ? -1 : 0, direction.getStepZ()),
//$$                         level.getGameTime())
//$$         );
//$$
//$$         if (!this.level.isClientSide()) {
//$$             this.award(fluid.isSame(Fluids.LAVA) ? AllAdvancements.LAVA_WHEEL : AllAdvancements.WATER_WHEEL);
//$$         }
//$$     }
//$$
//$$     @Inject(method = "getFlowVectorAtPosition", at = @At("HEAD"), cancellable = true, remap = false)
//$$     private void ff$modifyGetFlowVectorAtPosition(BlockPos pos, CallbackInfoReturnable<Vec3> cir) {
//$$         if (level == null || !FlowingFluids.config.enableMod || !FlowingFluids.config.create_waterWheelMode.needsFlow())
//$$             return;
//$$
//$$         BlockState blockState = level.getBlockState(pos);
//$$         if (blockState.getBlock() == Blocks.BUBBLE_COLUMN){
//$$             cir.setReturnValue(new Vec3(0, blockState.getValue(BubbleColumnBlock.DRAG_DOWN) ? -1 : 1, 0));
//$$             return;
//$$         }
//$$
//$$         var dir = recentDirections.get(pos);
//$$         if (dir == null) {
//$$             cir.setReturnValue(Vec3.ZERO);
//$$             return;
//$$         }
//$$
//$$         // ignore expiry if fluids won't tick here
//$$         boolean ignoreExpiry = FlowingFluids.config.dontTickAtLocation(worldPosition, level);
//$$
//$$         // can linger for 5 seconds
//$$         if (!ignoreExpiry && dir.second() + FlowingFluids.config.create_waterWheelFlowMaxTickInterval < level.getGameTime()) {
//$$             recentDirections.remove(pos);
//$$             cir.setReturnValue(Vec3.ZERO);
//$$             return;
//$$         }
//$$
//$$         cir.setReturnValue(dir.first());
//$$     }
//$$ }
//#endif