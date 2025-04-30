package traben.flowing_fluids.fabric;
#if MC > MC_20_1
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfig;
import traben.flowing_fluids.config.FFConfigData;

public class FFConfigDataFabric extends FFConfigData {
    public static final StreamCodec<FriendlyByteBuf, FFConfigData> CODEC = new StreamCodec<FriendlyByteBuf, FFConfigData>() {
        @Override
        public @NotNull FFConfigData decode(final @NotNull FriendlyByteBuf buf) {
            var data = FFConfigDataFabric.read(buf);
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

    public FFConfigDataFabric() {
        super();
    }

    public FFConfigDataFabric(FFConfig delegate) {
        super(delegate);
    }

    public static FFConfigData read(final FriendlyByteBuf buffer) {
        FFConfigDataFabric packet;
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            try {
                FlowingFluids.info("- Server Config packet received");
                packet = new FFConfigDataFabric(new FFConfig(buffer));
            } catch (Exception e) {
                FlowingFluids.error("- Server Config packet decoding failed because:\n" + e);
                throw e;//crash
            }
        } else {
            FlowingFluids.LOG.warn("[Flowing Fluids] - received on server?????");
            packet = new FFConfigDataFabric(null);
        }
        return packet;
    }
}
#endif
