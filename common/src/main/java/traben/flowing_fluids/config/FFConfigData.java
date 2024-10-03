package traben.flowing_fluids.config;
#if MC > MC_20_1
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import traben.flowing_fluids.FlowingFluids;

public abstract class FFConfigData implements CustomPacketPayload {
    public static final Type<FFConfigData> type = new Type<>(ResourceLocation.tryParse("flowing_fluids:sync"));
    public FFConfig delegate;

    public FFConfigData() {
        this(FlowingFluids.config);
    }

    public FFConfigData(FFConfig delegate) {
        this.delegate = delegate;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return type;
    }

    public boolean isValid() {
        return delegate != null;
    }


    public void write(FriendlyByteBuf buf) {
        if (delegate != null)
            delegate.encodeToByteBuffer(buf);
    }
}
#endif
