package traben.flowing_fluids.fabric;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import traben.flowing_fluids.FlowingFluids;

import java.nio.file.Path;

public class FlowingFluidsPlatformImpl {
    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static void sendConfigToClient(ServerPlayer player) {

        FriendlyByteBuf buf = PacketByteBufs.create();

        FlowingFluids.config.encodeToByteBuffer(buf);
        FlowingFluids.LOG.info("[Flowing Fluids] - Sending server config to [" + player.getName().getString() + "]");
        ServerPlayNetworking.send(player, FFConfigDataFabric.read(buf));//todo just init and send the object normally in 1.20.6+ ?????
    }
}
