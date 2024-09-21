package traben.flowing_fluids.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.flowing_fluids.FlowingFluidsPlatform;

@Mixin(PlayerList.class)
public class MixinPlayerConnect {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void ff$disconnect(final Connection connection, final ServerPlayer player, final CommonListenerCookie cookie, final CallbackInfo ci) {
        FlowingFluidsPlatform.sendConfigToClient(player);
    }
}
