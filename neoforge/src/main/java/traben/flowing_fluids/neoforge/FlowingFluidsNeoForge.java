package traben.flowing_fluids.neoforge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFCommands;
import traben.flowing_fluids.config.FFConfigData;

@Mod(FlowingFluids.MOD_ID)
public final class FlowingFluidsNeoForge {
    public FlowingFluidsNeoForge() {
        // Run our common setup.
        FlowingFluids.init();
        NeoForge.EVENT_BUS.register(FlowingFluidsNeoForge.class);

    }

    @SubscribeEvent
    public static void onRegisterCommandEvent(RegisterCommandsEvent event) {
        FlowingFluids.info("commands registered");
        FFCommands.registerCommands(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }
}

@EventBusSubscriber(modid = "flowing_fluids", bus = EventBusSubscriber.Bus.MOD)
class ModRegister {
    @SubscribeEvent
    public static void onPayloadRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("flowing_fluids");
        registrar.playToClient(FFConfigData.type, FFConfigDataNeoForge.CODEC, (data, b) -> {
            try {
                if (data.isValid()) {
                    FlowingFluids.config = data.delegate;

                    FlowingFluids.info("- Server Config data received and synced");
                } else {
                    FlowingFluids.error("- Server Config data received and failed to sync, invalid data");
                    throw new RuntimeException("[Flowing Fluids] - Server Config data received and failed to sync, invalid data");
                }
            } catch (Exception e) {
                FlowingFluids.error("- Server Config data received and failed to sync, exception");
                throw new RuntimeException("[Flowing Fluids] - Server Config data received and failed to sync, exception", e);
            }
        });
    }
}