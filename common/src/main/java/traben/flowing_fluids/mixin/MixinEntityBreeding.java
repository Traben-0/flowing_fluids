package traben.flowing_fluids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.animal.Animal;
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
            if (this.animal.getRandom().nextFloat() > FlowingFluids.config.farmlandDrainWaterChance) return;

            var world = this.animal.level();
            var pos = this.animal.blockPosition();

            for(BlockPos blockPos2 : BlockPos.betweenClosed(pos.offset(-8, -1, -8), pos.offset(8, 1, 8))) {
                if (world.getFluidState(blockPos2).is(FluidTags.WATER)) {
                    FFFluidUtils.removeAmountFromFluidAtPosWithRemainder(world, blockPos2, Fluids.WATER,1);
                    world.playLocalSound(this.animal,SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 1, 1);
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
            for (int i = 0; i < 8; i++) {
                world.addParticle(new DustParticleOptions(Vec3.fromRGB24(9999746).toVector3f(),1),
                        pos.getX(), pos.getY(), pos.getZ(),
                        0.5f - rand.nextFloat(), rand.nextFloat(), 0.5f - rand.nextFloat());
            }
            world.playLocalSound(this.animal,SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.25f, 1);
        }
    }


}
