package traben.flowing_fluids.neoforge;


import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import traben.flowing_fluids.FlowingFluids;

import java.nio.file.Path;

public class FlowingFluidsPlatformImpl {
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static void sendConfigToClient(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new FFConfigDataNeoForge());
//       PacketDistributor.PLAYER.with(player).send(new SMData()
        FlowingFluids.info("- Sending server config to [" + player.getName().getString() + "]");
    }

    public static boolean isThisModLoaded(final String modId) {
        try {
            ModList list = ModList.get();
            if (list == null) {
                LoadingModList list2 = FMLLoader.getLoadingModList();
                return list2 != null && checkInitialModList(list2, modId);
            }
            return list.isLoaded(modId);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkInitialModList(@NotNull LoadingModList list, String modId) {
        try {
            for (ModInfo mod : list.getMods()) {
                if (mod.getModId().equals(modId)) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
