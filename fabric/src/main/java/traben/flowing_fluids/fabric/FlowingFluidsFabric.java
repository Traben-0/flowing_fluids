package traben.flowing_fluids.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFComands;

public final class FlowingFluidsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        CommandRegistrationCallback.EVENT.register(FFComands::registerCommands);
        PayloadTypeRegistry.playS2C().register(FFConfigDataFabric.type, FFConfigDataFabric.CODEC);
        FlowingFluids.init();
    }
}
