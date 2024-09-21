package traben.flowing_fluids.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.flowing_fluids.FlowingFluids;

@Mixin(Minecraft.class)
public class MixinDisconnect {
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("TAIL"))
    private void ff$disconnect(final Screen nextScreen, final CallbackInfo ci) {
        //reset config to remove possible external server config data
        FlowingFluids.loadConfig();
    }
}
