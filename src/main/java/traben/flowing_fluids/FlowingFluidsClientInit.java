package traben.flowing_fluids;

//#if FABRIC
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import traben.flowing_fluids.config.FFConfig;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class FlowingFluidsClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FlowingFluidsClient.start();

        //#if MC > 12001
                ClientPlayNetworking.registerGlobalReceiver(traben.flowing_fluids.networking.FFConfigData.type,
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
        //#else
        //$$ ClientPlayNetworking.registerGlobalReceiver(FFConfig.SERVER_CONFIG_PACKET_ID,
        //$$         (client, handler, buf, responseSender) -> {
        //$$
        //$$             try {
        //$$                 FlowingFluids.config = new FFConfig(buf);
        //$$                 FlowingFluids.info("- Server Config data received and synced");
        //$$             } catch (Exception e) {
        //$$                 FlowingFluids.error("- Server Config data received and failed to sync");
        //$$                 throw new RuntimeException("[Flowing Fluids] - Server Config data received and failed to sync", e);
        //$$             }
        //$$
        //$$         });
        //#endif
    }



//#elseif FORGE
//$$ @net.minecraftforge.fml.common.Mod("flowing_fluids")
//$$ public class FlowingFluidsClientInit {
//#else
//$$ @net.neoforged.fml.common.Mod("flowing_fluids")
//$$ public class FlowingFluidsClientInit {
//#endif
}
