package traben.flowing_fluids.config;

import net.minecraft.network.FriendlyByteBuf;
import traben.flowing_fluids.FlowingFluids;

public class FFConfig {
    public boolean fastmode = false;
    public boolean flowToEdges = true;
    public LevelingBehaviour levelingBehaviour = LevelingBehaviour.LAZY_LEVEL;
    public boolean enableMod = true;
    public boolean debugSpread = false;
    public boolean debugSpreadPrint = false;
    public boolean enableDisplacement = true;
    public boolean enablePistonPushing = true;


    public FFConfig() {
    }

    public FFConfig(FriendlyByteBuf buffer) {

        FlowingFluids.LOG.info("[Flowing Fluids] - Decoding server config packet from server.");
        //PRESERVE WRITE ORDER IN READ
        /////////////////////////////////////////
        fastmode = buffer.readBoolean();
        flowToEdges = buffer.readBoolean();
        levelingBehaviour = buffer.readEnum(LevelingBehaviour.class);
        enableMod = buffer.readBoolean();
        debugSpread = buffer.readBoolean();
        debugSpreadPrint = buffer.readBoolean();
        enableDisplacement = buffer.readBoolean();
        enablePistonPushing = buffer.readBoolean();
        ///////////////////////////////////////////////

    }

    public void encodeToByteBuffer(FriendlyByteBuf buffer) {
        FlowingFluids.LOG.info("[Flowing Fluids] - Encoding server config packet for client.");

        //PRESERVE WRITE ORDER IN READ
        /////////////////////////////////////////
        buffer.writeBoolean(fastmode);
        buffer.writeBoolean(flowToEdges);
        buffer.writeEnum(levelingBehaviour);
        buffer.writeBoolean(enableMod);
        buffer.writeBoolean(debugSpread);
        buffer.writeBoolean(debugSpreadPrint);
        buffer.writeBoolean(enableDisplacement);
        buffer.writeBoolean(enablePistonPushing);
        ///////////////////////////////////////////////
    }

    public enum LevelingBehaviour {
        VANILLA_LIKE,
        LAZY_LEVEL,
        STRONG_LEVEL,
        FORCE_LEVEL

    }
}
