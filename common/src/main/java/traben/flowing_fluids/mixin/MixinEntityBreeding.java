package traben.flowing_fluids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

@Mixin(BreedGoal.class)
public class MixinEntityBreeding {

    @Shadow @Final protected Animal animal;

    @Shadow @Nullable protected Animal partner;

    @Inject(method = "breed", at = @At(value = "HEAD"), cancellable = true)
    private void ff$drinkToBreed(final CallbackInfo ci) {
        if (FlowingFluids.config.enableMod
                && FlowingFluids.config.drinkWaterToBreedAnimalChance > 0
                && FlowingFluids.config.isWaterAllowed()) {

            // allow breed without water
            if (this.animal.getRandom().nextFloat() < FlowingFluids.config.drinkWaterToBreedAnimalChance) return;

            var world = this.animal.level();
            var pos = this.animal.blockPosition();

            for(BlockPos blockPos2 : BlockPos.betweenClosed(pos.offset(-8, -1, -8), pos.offset(8, 1, 8))) {
                var state = world.getBlockState(blockPos2);
                if (world.getFluidState(blockPos2).is(FluidTags.WATER)) {
                    FFFluidUtils.removeAmountFromFluidAtPosWithRemainder(world, blockPos2, Fluids.WATER,1);
                    world.playSound(null,this.animal.getX(), this.animal.getY(), this.animal.getZ(), SoundEvents.GENERIC_DRINK #if MC>=MC_21_5 .value() #endif , SoundSource.NEUTRAL, 1, 1);
                    return;
                }
                if (state.is(Blocks.WATER_CAULDRON)) {
                    LayeredCauldronBlock.lowerFillLevel(state, world, blockPos2);
                    world.playSound(null,this.animal.getX(), this.animal.getY(), this.animal.getZ(), SoundEvents.GENERIC_DRINK #if MC>=MC_21_5 .value() #endif , SoundSource.NEUTRAL, 1, 1);
                    return;
                }
            }

            // no water found, cancel breeding
            ci.cancel();
            this.animal.setAge(6000);
            this.animal.resetLove();

            assert this.partner != null;
            this.partner.setAge(6000);
            this.partner.resetLove();

            // some feedback
            var rand = this.animal.getRandom();

            if (world instanceof ServerLevel server) {
                for (int i = 0; i < 8; i++) {
                    server.sendParticles(new DustParticleOptions(#if MC> MC_21 9999746 #else Vec3.fromRGB24(9999746).toVector3f() #endif , 1),
                            pos.getX(), pos.getY(), pos.getZ(),
                            1,(0.5f - rand.nextFloat())*3, rand.nextFloat()*2, (0.5f - rand.nextFloat())*3, 1);
                }
                server.playSound(null,this.animal.getX(), this.animal.getY(), this.animal.getZ(),SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.25f, 1);
            }
         }
    }


}
