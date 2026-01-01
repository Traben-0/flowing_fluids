package traben.flowing_fluids.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfig;

#if MC > MC_20_1
import traben.flowing_fluids.config.FFConfigData;
#endif
public final class FlowingFluidsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        #if MC > MC_20_1
        ClientPlayNetworking.registerGlobalReceiver(FFConfigData.type,
                (data, context) -> {
                    context.client().execute(() -> {
                                //create server config
                                if (!data.isValid()) {
                                    FlowingFluids.error("- Server Config data received and failed to sync, invalid data");
                                    throw new RuntimeException("[Flowing Fluids] - Invalid Server Config data received");
                                }
                                try {
                                    FlowingFluids.config = data.delegate;
                                    FlowingFluids.info("- Server Config data received and synced");
                                } catch (Exception e) {
                                    FlowingFluids.error("- Server Config data received and failed to sync");
                                    throw new RuntimeException("[Flowing Fluids] - Server Config data received and failed to sync", e);
                                }
                            }
                    );
                });
        #else
        ClientPlayNetworking.registerGlobalReceiver(FFConfig.SERVER_CONFIG_PACKET_ID,
                (client, handler, buf, responseSender) -> {

                                try {
                                    FlowingFluids.config = new FFConfig(buf);
                                    FlowingFluids.info("- Server Config data received and synced");
                                } catch (Exception e) {
                                    FlowingFluids.error("- Server Config data received and failed to sync");
                                    throw new RuntimeException("[Flowing Fluids] - Server Config data received and failed to sync", e);
                                }

                });
        #endif
    }
}
