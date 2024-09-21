package traben.flowing_fluids.neoforge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFComands;
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
        FlowingFluids.LOG.info("[Flowing Fluids] commands registered");
        FFComands.registerCommands(event.getDispatcher(), null, null);
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

                    FlowingFluids.LOG.info("[Flowing Fluids] - Server Config data received and synced");
                } else {
                    FlowingFluids.LOG.error("[Flowing Fluids] - Server Config data received and failed to sync, invalid data");
                    throw new RuntimeException("[Flowing Fluids] - Server Config data received and failed to sync, invalid data");
                }
            } catch (Exception e) {
                FlowingFluids.LOG.error("[Flowing Fluids] - Server Config data received and failed to sync, exception");
                throw new RuntimeException("[Flowing Fluids] - Server Config data received and failed to sync, exception", e);
            }
        });
    }
}