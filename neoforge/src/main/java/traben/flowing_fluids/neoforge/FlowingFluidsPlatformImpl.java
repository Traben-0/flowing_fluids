package traben.flowing_fluids.neoforge;


import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import traben.flowing_fluids.FlowingFluids;

import java.nio.file.Path;

public class FlowingFluidsPlatformImpl {
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static void sendConfigToClient(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new FFConfigDataNeoForge());
//       PacketDistributor.PLAYER.with(player).send(new SMData()
        FlowingFluids.LOG.info("[Flowing Fluids] - Sending server config to [" + player.getName().getString() + "]");
    }
}
