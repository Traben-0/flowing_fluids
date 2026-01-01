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
    #if MC > MC_20_1
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("TAIL"))
    #else
    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("TAIL"))
    #endif
    private void ff$disconnect(final Screen screen, final CallbackInfo ci) {
        //reset config to remove possible external server config data
        FlowingFluids.loadConfig();
    }
}
