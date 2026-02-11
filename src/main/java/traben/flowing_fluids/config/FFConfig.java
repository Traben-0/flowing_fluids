package traben.flowing_fluids.config;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
//#if MC > 12100
import net.minecraft.util.ARGB;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//$$ import net.minecraft.util.FastColor;
//#endif
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import traben.flowing_fluids.FlowingFluids;

public class FFConfig {
    public boolean flowToEdges = true;
    public boolean enableMod = true;
    public boolean enableDisplacement = true;
    public boolean enablePistonPushing = true;
    public float rainRefillChance = 0.3f;
    public float oceanRiverSwampRefillChance = 1f;
    public float evaporationChanceV2 = 1f;
    public float evaporationNetherChance = 1f;
    public boolean printRandomTicks = false;
    public boolean hideFlowingTexture = true;
    public LiquidHeight fullLiquidHeight = LiquidHeight.REGULAR;
    public float farmlandDrainWaterChance = 0.1f;
    public boolean debugWaterLevelColours = false;
    public WaterLogFlowMode waterLogFlowMode = WaterLogFlowMode.IN_FROM_TOP_ELSE_OUT;
    public int waterFlowDistance = 4;
    public int lavaFlowDistance = 2;
    public int lavaNetherFlowDistance = 4;
    public int waterTickDelay = 2;
    public int lavaTickDelay= 15;
    public int lavaNetherTickDelay = 5;
    public int randomTickLevelingDistance = 32;
    public float drinkWaterToBreedAnimalChance = 0.1f;
    public boolean encloseAllFluidOnWorldGen = true;
    public boolean announceWorldGenActions = false;
    public boolean easyPistonPump = true;
    public boolean waterFlowAffectsBoats = false;
    public boolean waterFlowAffectsEntities = true;
    public boolean waterFlowAffectsPlayers = false;
    public boolean waterFlowAffectsItems = true;
    public float infiniteWaterBiomeNonConsumeChance = 0.01f;
    public float infiniteWaterBiomeDrainSurfaceChance = 0.5f;
    public int minWaterLevelForIce = 4;
    public boolean rainFillsWaterHigherV2 = false;
    public int minLavaLevelForObsidian = 6;
    public boolean fastBiomeRefillAtSeaLevelOnly = false;
    public int playerBlockDistanceForFlowing = 0;
    public float concreteDrainsWaterChance = 0.5f;
    public float displacementDepthMultiplier = 1f;
    public DisplacementSounds displacementSounds = DisplacementSounds.BOTH;
    public float flowSoundChance = 0.15f;
    public int tickDelaySpread = 0; // not alterable via command, only for auto performance

    // Auto performance handling
    public AutoPerformance autoPerformanceMode = AutoPerformance.HIGH_QUALITY_DEFAULT;
    public int autoPerformanceUpdateRateSeconds = 10;
    public float autoPerformanceMSPTargetMultiplier = 0.9f;
    public boolean autoPerformanceShowMessages = true;


    // create mod options
    public CreateWaterWheelMode create_waterWheelMode = CreateWaterWheelMode.REQUIRE_FLOW_OR_RIVER;
    public boolean create_infinitePipes = false;
    public int create_waterWheelFlowMaxTickInterval = 80;

    // fluid blacklist
    public ObjectOpenHashSet<String> fluidBlacklist = new ObjectOpenHashSet<>();

    // sea level overrides
    public int defaultSeaLevelOverride = Integer.MIN_VALUE;
    public Int2IntOpenHashMap dimensionSeaLevelOverrides = new Int2IntOpenHashMap();

    public boolean hideStartMessage = false;

    public boolean isFluidAllowed(Fluid fluid){
        if (fluid == null) return false;
        // quick most likely exits to avoid searching the blacklist
        if (fluidBlacklist.isEmpty() || fluid == Fluids.EMPTY) return true;
        return !fluidBlacklist.contains(BuiltInRegistries.FLUID.getKey(fluid).toString());
    }
    public boolean isFluidAllowed(FluidState fluid){
        return isFluidAllowed(fluid.getType());
    }

    public boolean isWaterAllowed(){
        return isFluidAllowed(Fluids.WATER);
    }

    public boolean dontTickAtLocation(BlockPos pos, LevelAccessor level) {
        if (playerBlockDistanceForFlowing == 0) return false;

        int sqrDist = playerBlockDistanceForFlowing * playerBlockDistanceForFlowing;

        // if any player is within distance
        for(Player player2 : level.players()) {
            double i = player2.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
            if (i < sqrDist) return false;
        }
        return true;
    }

    public FFConfig() {
    }

    //color range from red to blue over 8 steps
    public static int[] waterLevelColours ={

            //#if MC > 12100
            ARGB.color(255,0,0,255),
            ARGB.color(255,0,128,255),
            ARGB.color(255,0,255,192),
            ARGB.color(255,0,255,0),
            ARGB.color(255,255,255,0),
            ARGB.color(255,255,128,0),
            ARGB.color(255,255,0,0),
            ARGB.color(255,255,255,255)
            //#else
            //$$ FastColor.ARGB32.color(255,0,0,255),
            //$$ FastColor.ARGB32.color(255,0,128,255),
            //$$ FastColor.ARGB32.color(255,0,255,192),
            //$$ FastColor.ARGB32.color(255,0,255,0),
            //$$ FastColor.ARGB32.color(255,255,255,0),
            //$$ FastColor.ARGB32.color(255,255,128,0),
            //$$ FastColor.ARGB32.color(255,255,0,0),
            //$$ FastColor.ARGB32.color(255,255,255,255)
            //#endif
    };

    public FFConfig(FriendlyByteBuf buffer) {
        setFromBuff(buffer);
    }

    public void setFromBuff(FriendlyByteBuf buffer) {
        FlowingFluids.info("- Decoding server config packet from server.");
        //PRESERVE WRITE ORDER IN READ
        /////////////////////////////////////////
        flowToEdges = buffer.readBoolean();
        enableMod = buffer.readBoolean();
        enableDisplacement = buffer.readBoolean();
        enablePistonPushing = buffer.readBoolean();
        rainRefillChance = buffer.readFloat();
        oceanRiverSwampRefillChance = buffer.readFloat();
        evaporationChanceV2 = buffer.readFloat();
        evaporationNetherChance = buffer.readFloat();
        printRandomTicks = buffer.readBoolean();
        hideFlowingTexture = buffer.readBoolean();
        fullLiquidHeight = buffer.readEnum(LiquidHeight.class);
        farmlandDrainWaterChance = buffer.readFloat();
        debugWaterLevelColours = buffer.readBoolean();
        waterLogFlowMode = buffer.readEnum(WaterLogFlowMode.class);
        waterFlowDistance = buffer.readVarInt();
        lavaFlowDistance = buffer.readVarInt();
        lavaNetherFlowDistance = buffer.readVarInt();
        waterTickDelay = buffer.readVarInt();
        lavaTickDelay = buffer.readVarInt();
        lavaNetherTickDelay = buffer.readVarInt();
        randomTickLevelingDistance = buffer.readVarInt();
        drinkWaterToBreedAnimalChance = buffer.readFloat();
        encloseAllFluidOnWorldGen = buffer.readBoolean();
        announceWorldGenActions = buffer.readBoolean();
        easyPistonPump = buffer.readBoolean();
        waterFlowAffectsBoats = buffer.readBoolean();
        waterFlowAffectsEntities = buffer.readBoolean();
        waterFlowAffectsPlayers = buffer.readBoolean();
        waterFlowAffectsItems = buffer.readBoolean();
        infiniteWaterBiomeNonConsumeChance = buffer.readFloat();
        infiniteWaterBiomeDrainSurfaceChance = buffer.readFloat();
        minWaterLevelForIce = buffer.readVarInt();
        rainFillsWaterHigherV2 = buffer.readBoolean();
        minLavaLevelForObsidian = buffer.readVarInt();
        fastBiomeRefillAtSeaLevelOnly = buffer.readBoolean();
        playerBlockDistanceForFlowing = buffer.readVarInt();
        concreteDrainsWaterChance = buffer.readFloat();
        displacementDepthMultiplier = buffer.readFloat();
        displacementSounds = buffer.readEnum(DisplacementSounds.class);
        flowSoundChance = buffer.readFloat();
        tickDelaySpread = buffer.readVarInt();

        // Auto performance handling
        autoPerformanceMode = buffer.readEnum(AutoPerformance.class);
        autoPerformanceUpdateRateSeconds = buffer.readVarInt();
        autoPerformanceMSPTargetMultiplier = buffer.readFloat();
        autoPerformanceShowMessages = buffer.readBoolean();


        // create mod options
        create_waterWheelMode = buffer.readEnum(CreateWaterWheelMode.class);
        create_infinitePipes = buffer.readBoolean();
        create_waterWheelFlowMaxTickInterval = buffer.readVarInt();

        // blacklist
        fluidBlacklist = buffer.readCollection(ObjectOpenHashSet::new, FriendlyByteBuf::readUtf);

        defaultSeaLevelOverride = buffer.readVarInt();
        dimensionSeaLevelOverrides = buffer.readMap(Int2IntOpenHashMap::new, FriendlyByteBuf::readInt, FriendlyByteBuf::readInt);

        hideStartMessage = buffer.readBoolean();
        ///////////////////////////////////////////////
    }

    public void encodeToByteBuffer(FriendlyByteBuf buffer) {

        FlowingFluids.info("- Encoding server config packet for client.");

        //PRESERVE WRITE ORDER IN READ
        /////////////////////////////////////////
        buffer.writeBoolean(flowToEdges);
        buffer.writeBoolean(enableMod);
        buffer.writeBoolean(enableDisplacement);
        buffer.writeBoolean(enablePistonPushing);
        buffer.writeFloat(rainRefillChance);
        buffer.writeFloat(oceanRiverSwampRefillChance);
        buffer.writeFloat(evaporationChanceV2);
        buffer.writeFloat(evaporationNetherChance);
        buffer.writeBoolean(printRandomTicks);
        buffer.writeBoolean(hideFlowingTexture);
        buffer.writeEnum(fullLiquidHeight);
        buffer.writeFloat(farmlandDrainWaterChance);
        buffer.writeBoolean(debugWaterLevelColours);
        buffer.writeEnum(waterLogFlowMode);
        buffer.writeVarInt(waterFlowDistance);
        buffer.writeVarInt(lavaFlowDistance);
        buffer.writeVarInt(lavaNetherFlowDistance);
        buffer.writeVarInt(waterTickDelay);
        buffer.writeVarInt(lavaTickDelay);
        buffer.writeVarInt(lavaNetherTickDelay);
        buffer.writeVarInt(randomTickLevelingDistance);
        buffer.writeFloat(drinkWaterToBreedAnimalChance);
        buffer.writeBoolean(encloseAllFluidOnWorldGen);
        buffer.writeBoolean(announceWorldGenActions);
        buffer.writeBoolean(easyPistonPump);
        buffer.writeBoolean(waterFlowAffectsBoats);
        buffer.writeBoolean(waterFlowAffectsEntities);
        buffer.writeBoolean(waterFlowAffectsPlayers);
        buffer.writeBoolean(waterFlowAffectsItems);
        buffer.writeFloat(infiniteWaterBiomeNonConsumeChance);
        buffer.writeFloat(infiniteWaterBiomeDrainSurfaceChance);
        buffer.writeVarInt(minWaterLevelForIce);
        buffer.writeBoolean(rainFillsWaterHigherV2);
        buffer.writeVarInt(minLavaLevelForObsidian);
        buffer.writeBoolean(fastBiomeRefillAtSeaLevelOnly);
        buffer.writeVarInt(playerBlockDistanceForFlowing);
        buffer.writeFloat(concreteDrainsWaterChance);
        buffer.writeFloat(displacementDepthMultiplier);
        buffer.writeEnum(displacementSounds);
        buffer.writeFloat(flowSoundChance);
        buffer.writeVarInt(tickDelaySpread);

        // Auto performance handling
        buffer.writeEnum(autoPerformanceMode);
        buffer.writeVarInt(autoPerformanceUpdateRateSeconds);
        buffer.writeFloat(autoPerformanceMSPTargetMultiplier);
        buffer.writeBoolean(autoPerformanceShowMessages);

        // create mod options
        buffer.writeEnum(create_waterWheelMode);
        buffer.writeBoolean(create_infinitePipes);
        buffer.writeVarInt(create_waterWheelFlowMaxTickInterval);

        // blacklist
        buffer.writeCollection(fluidBlacklist, FriendlyByteBuf::writeUtf);

        buffer.writeVarInt(defaultSeaLevelOverride);
        buffer.writeMap(dimensionSeaLevelOverrides, FriendlyByteBuf::writeInt, FriendlyByteBuf::writeInt);

        buffer.writeBoolean(hideStartMessage);
        ///////////////////////////////////////////////
    }

    public enum WaterLogFlowMode {
        ONLY_IN,
        ONLY_OUT,
        IN_FROM_TOP_ELSE_OUT,
        OUT_DOWN_ELSE_IN,
        IGNORE;

        public boolean blocksFlowOutDown(){
            return this == ONLY_IN || this == IGNORE;
        }

        public boolean blocksFlowIn(boolean down){
            if (down) return this == ONLY_OUT || this == IGNORE;
            return this == ONLY_OUT || this == IN_FROM_TOP_ELSE_OUT || this == IGNORE;
        }

        public boolean blocksFlowOutSides(){
            return this == ONLY_IN || this == OUT_DOWN_ELSE_IN || this == IGNORE;
        }
    }

    @SuppressWarnings("unused")
    public enum CreateWaterWheelMode {
        ALWAYS,
        REQUIRE_FLOW,
        REQUIRE_FLOW_OR_RIVER,
        REQUIRE_FLUID,
        REQUIRE_FULL_FLUID,
        RIVER_ONLY,
        REQUIRE_FLOW_OR_RIVER_OPPOSITE,
        REQUIRE_FLUID_OPPOSITE,
        REQUIRE_FULL_FLUID_OPPOSITE,
        ALWAYS_OPPOSITE,
        RIVER_ONLY_OPPOSITE;

        public boolean isCounterSpin() {
            return this.ordinal() > 5;
        }

        public boolean isRiver() {
            return this == REQUIRE_FLOW_OR_RIVER || this == REQUIRE_FLOW_OR_RIVER_OPPOSITE || this == RIVER_ONLY || this == RIVER_ONLY_OPPOSITE;
        }

        public boolean isRiverOnly() {
            return this == RIVER_ONLY || this == RIVER_ONLY_OPPOSITE;
        }

        public boolean needsFullFluid() {
            return this == REQUIRE_FULL_FLUID || this == REQUIRE_FULL_FLUID_OPPOSITE;
        }

        public boolean always(){
            return this == ALWAYS || this == ALWAYS_OPPOSITE;
        }

        public boolean needsFlow() {
            return this == REQUIRE_FLOW || this == REQUIRE_FLOW_OR_RIVER || this == REQUIRE_FLOW_OR_RIVER_OPPOSITE;
        }

    }

    public enum LiquidHeight {
        REGULAR,
        REGULAR_LOWER_BOUND,
        BLOCK,
        BLOCK_LOWER_BOUND,
        SLAB,
        CARPET
    }

    public enum DisplacementSounds {
        NONE,
        PISTON_ONLY,
        BLOCKS_ONLY,
        BOTH;

        public boolean allow(boolean isPiston) {
            return switch (this) {
                case NONE -> false;
                case BOTH -> true;
                case PISTON_ONLY -> isPiston;
                case BLOCKS_ONLY -> !isPiston;
            };
        }
    }

    public enum AutoPerformance {
        OFF,
        HIGH_QUALITY_DEFAULT,
        MEDIUM_QUALITY,
        LOW_QUALITY;

        public boolean enabled() {
            return this != OFF;
        }

        public String pretty() {
            if (this == OFF) return "";
            return " with " + pretty2();
        }

        public String pretty2() {
            return switch (this) {
                case OFF -> "";
                case HIGH_QUALITY_DEFAULT -> "High Quality (Default)";
                case MEDIUM_QUALITY -> "Medium Quality";
                case LOW_QUALITY -> "Low Quality";
            };
        }
    }

    //#if MC <= 12001
    //$$ public static final ResourceLocation SERVER_CONFIG_PACKET_ID = traben.flowing_fluids.FFFluidUtils.res("flowing_fluids:server_config_packet");
    //#endif
}
