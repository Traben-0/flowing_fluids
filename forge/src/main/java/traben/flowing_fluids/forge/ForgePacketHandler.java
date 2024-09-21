package traben.flowing_fluids.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfig;

public class ForgePacketHandler {
    private static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel INSTANCE = ChannelBuilder
            .named(FlowingFluids.MOD_ID + "_channel")
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel();


    public static void init() {

        INSTANCE.messageBuilder(FFConfigPacket.class)
                .encoder(FFConfigPacket::encoder)
                .decoder(FFConfigPacket::decoder)
                .consumerMainThread(FFConfigPacket::messageConsumer)
                .add();

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
                    FlowingFluids.LOG.info("[Flowing Fluids] - Server Config packet received");
                    packet = new FFConfigPacket(buffer);
                    packet.is_valid = true;
                } catch (Exception e) {
                    FlowingFluids.LOG.error("[Flowing Fluids] - Server Config packet decoding failed because:\n" + e);
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

        public static void messageConsumer(FFConfigPacket packet, CustomPayloadEvent.Context ctx) {
            // Handle message
            if (packet.is_valid) {
                FlowingFluids.config = packet;
                FlowingFluids.LOG.info("[Flowing Fluids] - Server Config data received and synced");
            } else {
                FlowingFluids.LOG.error("[Flowing Fluids] - Server Config data received and failed to sync");
                throw new RuntimeException("[Flowing Fluids] - Server Config data received and failed to sync");
            }
            ctx.setPacketHandled(true);
        }

        // A class MessagePacket
        public void encoder(FriendlyByteBuf buffer) {
            // Write to buffer
            FlowingFluids.config.encodeToByteBuffer(buffer);
        }
    }
}
