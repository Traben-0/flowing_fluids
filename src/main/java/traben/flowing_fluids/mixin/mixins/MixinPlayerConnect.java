package traben.flowing_fluids.mixin.mixins;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
//#if MC > 12001
import net.minecraft.server.network.CommonListenerCookie;
//#endif
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.flowing_fluids.FlowingFluids;

@Mixin(PlayerList.class)
public class MixinPlayerConnect {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    //#if MC > 12001
    private void ff$disconnect(final Connection connection, final ServerPlayer player, final CommonListenerCookie cookie, final CallbackInfo ci) {
    //#else
    //$$ private void ff$disconnect(final Connection netManager, final ServerPlayer player, final CallbackInfo ci) {
    //#endif
        FlowingFluids.sendConfigToClient(player);
    }
}
