package traben.flowing_fluids.neoforge;


import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import traben.flowing_fluids.config.FFConfig;
import traben.flowing_fluids.config.FFConfigData;

public class FFConfigDataNeoForge extends FFConfigData {
    public static final StreamCodec<FriendlyByteBuf, FFConfigData> CODEC = new StreamCodec<FriendlyByteBuf, FFConfigData>() {
        @Override
        public @NotNull FFConfigData decode(final @NotNull FriendlyByteBuf buf) {
            var data = FFConfigDataNeoForge.read(buf);
            if (data.isValid()) {
                return data;
            }
            throw new RuntimeException("[Flowing Fluids] - Invalid Server Config data received");
        }

        @Override
        public void encode(final FriendlyByteBuf buf, final FFConfigData value) {
            value.write(buf);
        }
    };

    public FFConfigDataNeoForge() {
        super();
    }

    public FFConfigDataNeoForge(FFConfig delegate) {
        super(delegate);
    }

    public static FFConfigData read(final FriendlyByteBuf buffer) {
        FFConfigDataNeoForge packet;
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                System.out.println("[Solid mobs] - Server Config packet received");
                packet = new FFConfigDataNeoForge(new FFConfig(buffer));
            } catch (Exception e) {
                System.out.println("[Solid mobs] - Server Config packet decoding failed because:\n" + e);
                e.printStackTrace();
                packet = new FFConfigDataNeoForge(null);
            }
        } else {
            packet = new FFConfigDataNeoForge(new FFConfig(buffer));
        }
        return packet;
    }
}
