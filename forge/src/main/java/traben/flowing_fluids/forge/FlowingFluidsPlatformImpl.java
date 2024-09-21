package traben.flowing_fluids.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import traben.flowing_fluids.FlowingFluids;

import java.nio.file.Path;

public class FlowingFluidsPlatformImpl {
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static void sendConfigToClient(ServerPlayer player) {
        ForgePacketHandler.INSTANCE.send(new ForgePacketHandler.FFConfigPacket(), PacketDistributor.PLAYER.with(player));
        FlowingFluids.LOG.info("[Flowing Fluids] - Sending server config to [" + player.getName().getString() + "]");
    }
}
