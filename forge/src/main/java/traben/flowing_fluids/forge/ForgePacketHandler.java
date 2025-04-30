package traben.flowing_fluids.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

#if MC > MC_20_1
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
#else
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.config.FFConfig;
import java.util.function.Supplier;
#endif

import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfig;

public class ForgePacketHandler {
    private static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel INSTANCE =
        #if MC > MC_20_1
            ChannelBuilder
            .named(FlowingFluids.MOD_ID + "_channel")
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel();
        #else
            NetworkRegistry.newSimpleChannel(
                FFFluidUtils.res(FlowingFluids.MOD_ID, "channel"),
                () -> String.valueOf(PROTOCOL_VERSION),
                    String.valueOf(PROTOCOL_VERSION)::equals,
                    String.valueOf(PROTOCOL_VERSION)::equals
            );
        #endif


    public static void init() {
        #if MC > MC_20_1
            INSTANCE.messageBuilder(FFConfigPacket.class)
                .encoder(FFConfigPacket::encoder)
                .decoder(FFConfigPacket::decoder)
                .consumerMainThread(FFConfigPacket::messageConsumer)
                .add();
        #else
        INSTANCE.registerMessage(0,
                FFConfigPacket.class,
                FFConfigPacket::encoder,
                FFConfigPacket::decoder,
                FFConfigPacket::messageConsumer);
        #endif
    }



    public static class FFConfigPacket extends FFConfig {
        private boolean is_valid;

        FFConfigPacket() {
        }

        FFConfigPacket(FriendlyByteBuf buffer) {
            super(buffer);
        }

        public static FFConfigPacket decoder(FriendlyByteBuf buffer) {
            // Create packet from buffer data
            //create server config
            FFConfigPacket packet;
            if (FMLEnvironment.dist == Dist.CLIENT) {
                try {
                    FlowingFluids.info("- Server Config packet received");
                    packet = new FFConfigPacket(buffer);
                    packet.is_valid = true;
                } catch (Exception e) {
                    FlowingFluids.error("- Server Config packet decoding failed because:\n" + e);
                    e.printStackTrace();
                    packet = new FFConfigPacket();
                    packet.is_valid = false;
                }
            } else {
                packet = new FFConfigPacket();
                packet.is_valid = false;
            }
            return packet;
        }
#if MC > MC_20_1
        public static void messageConsumer(FFConfigPacket packet, CustomPayloadEvent.Context ctx) {
        // Handle message
            if (packet.is_valid) {
                FlowingFluids.config = packet;
                FlowingFluids.info("- Server Config data received and synced");
            } else {
                FlowingFluids.error("- Server Config data received and failed to sync");
                throw new RuntimeException("[Flowing Fluids] - Server Config data received and failed to sync");
            }
            ctx.setPacketHandled(true);
        }
#else
        public void messageConsumer(Supplier<NetworkEvent.Context> ctx) {
            // Handle message
            if (is_valid) {
                FlowingFluids.config = this;
                FlowingFluids.info("- Server Config data received and synced");
            } else {
                FlowingFluids.error("- Server Config data received and failed to sync");
                throw new RuntimeException("[Flowing Fluids] - Server Config data received and failed to sync");
            }
            ctx.get().setPacketHandled(true);
        }
#endif


        // A class MessagePacket
        public void encoder(FriendlyByteBuf buffer) {
            // Write to buffer
            FlowingFluids.config.encodeToByteBuffer(buffer);
        }
    }

}
