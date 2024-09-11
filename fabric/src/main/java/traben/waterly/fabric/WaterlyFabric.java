package traben.waterly.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import traben.waterly.Waterly;

public final class WaterlyFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        CommandRegistrationCallback.EVENT.register(Waterly::registerCommands);
        Waterly.init();
    }
}
