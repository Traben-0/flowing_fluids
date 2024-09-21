package traben.flowing_fluids.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfigData;

public final class FlowingFluidsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        ClientPlayNetworking.registerGlobalReceiver(FFConfigData.type,
                (data, context) -> {
                    context.client().execute(() -> {
                                //create server config
                                if (!data.isValid()) {
                                    FlowingFluids.LOG.error("[Flowing Fluids] - Server Config data received and failed to sync, invalid data");
                                    throw new RuntimeException("[Flowing Fluids] - Invalid Server Config data received");
                                }
                                try {
                                    FlowingFluids.config = data.delegate;
                                    FlowingFluids.LOG.info("[Flowing Fluids] - Server Config data received and synced");
                                } catch (Exception e) {
                                    FlowingFluids.LOG.error("[Flowing Fluids] - Server Config data received and failed to sync");
                                    throw new RuntimeException("[Flowing Fluids] - Server Config data received and failed to sync", e);
                                }
                            }
                    );
                });
    }
}
