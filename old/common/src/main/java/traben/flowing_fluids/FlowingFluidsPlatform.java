package traben.flowing_fluids;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public class FlowingFluidsPlatform {
    @ExpectPlatform
    public static Path getConfigDirectory() {
        return Path.of("");
    }


    @ExpectPlatform
    public static void sendConfigToClient(ServerPlayer player) {
    }

    @ExpectPlatform
    public static boolean isThisModLoaded(String modId) {
        throw new AssertionError();
    }
}
