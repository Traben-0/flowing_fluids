package traben.flowing_fluids;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import traben.flowing_fluids.config.FFConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//#if FABRIC
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
//#elseif FORGE
//$$ import net.minecraftforge.fml.ModList;
//$$ import net.minecraftforge.fml.loading.FMLPaths;
//$$ import net.minecraftforge.fml.loading.LoadingModList;
//$$ import net.minecraftforge.forgespi.language.IModInfo;
//$$ import net.minecraftforge.network.PacketDistributor;
//$$ import traben.flowing_fluids.networking.ForgePacketHandler;
//#else
//$$ import net.neoforged.fml.ModList;
//$$ import net.neoforged.fml.loading.FMLPaths;
//$$ import net.neoforged.fml.loading.LoadingModList;
//$$ import net.neoforged.neoforge.network.PacketDistributor;
//$$ import net.neoforged.neoforgespi.language.IModInfo;
//$$ import traben.flowing_fluids.networking.FFConfigDataNeoForge;
//#endif

public class FlowingFluids {

    public static final String MOD_ID = "flowing_fluids";
    public final static Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public static boolean isManeuveringFluids = false;
    public static boolean pistonTick = false;
    public static @Nullable Direction lastPistonMoveDirection = null;
    public static long debug_killFluidUpdatesUntilTime = 0;
    public static int waterPluggedThisSession = 0;

    public static boolean CREATE = false;
    public static boolean TWILIGHT_FOREST = false;

    public static Map<Fluid, List<TagKey<Block>>> nonDisplacerTags = new Object2ObjectOpenHashMap<>();
    public static Map<Fluid, List<Block>> nonDisplacers = new Object2ObjectOpenHashMap<>();

    public static Map<Fluid, List<String>> nonDisplacerTagIds = new Object2ObjectOpenHashMap<>();
    public static Map<Fluid, List<String>> nonDisplacerIds = new Object2ObjectOpenHashMap<>();


    public static Set<TagKey<Biome>> infiniteBiomeTags = new HashSet<>();
    public static Set<ResourceKey<Biome>> infiniteBiomes = new HashSet<>();

    public static FFConfig config = new FFConfig();

    public static void info(String str) { LOG.info("[Flowing Fluids] {}", str); }
    public static void warn(String str) { LOG.warn("[Flowing Fluids] {}", str); }
    public static void error(String str) { LOG.error("[Flowing Fluids] {}", str); }

    public static void start() {
        info("initialising.");

        infiniteBiomeTags.add(BiomeTags.IS_OCEAN);
        infiniteBiomeTags.add(BiomeTags.IS_RIVER);
        infiniteBiomeTags.add(BiomeTags.IS_BEACH);
        infiniteBiomes.add(Biomes.SWAMP);
        infiniteBiomes.add(Biomes.MANGROVE_SWAMP);

        var waterBlockList = new ArrayList<Block>();
        var lavaBlockList = new ArrayList<Block>();
        var waterBlockTagList = new ArrayList<TagKey<Block>>();
        var waterBlockListIds = new ArrayList<String>();

        waterBlockTagList.add(BlockTags.ICE);
        waterBlockList.add(Blocks.SPONGE);
        lavaBlockList.add(Blocks.OBSIDIAN);

        waterBlockListIds.add("twilightforest:twilight_portal");
        nonDisplacerTags.put(Fluids.WATER, waterBlockTagList);
        nonDisplacers.put(Fluids.WATER, waterBlockList);
        nonDisplacers.put(Fluids.LAVA, lavaBlockList);
        nonDisplacerIds.put(Fluids.WATER, waterBlockListIds);

        loadConfig();

        CREATE = isThisModLoaded("create");
        TWILIGHT_FOREST = isThisModLoaded("twilightforest");
    }

    private static boolean showFirstMessage = true;

    public static void sendConfigToClient(ServerPlayer player) {
        if (showFirstMessage && !config.hideStartMessage) {
            player.sendSystemMessage(
                    Component.literal("[Flowing Fluids mod loaded]\n")
                            .withStyle(ChatFormatting.GOLD)
                            .append(Component.literal("- Automatic performance handling is ")
                                    .withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(config.autoPerformanceMode.enabled() ? "enabled" : "disabled")
                                    .withStyle(config.autoPerformanceMode.enabled() ? ChatFormatting.GREEN : ChatFormatting.RED))
                            .append(Component.literal(config.autoPerformanceMode.pretty() + ".\n- This will overwrite & handle performance settings automatically for you.\n- See ")
                                    .withStyle(ChatFormatting.WHITE))
                            .append(Component.literal("/flowing_fluids performance_and_presets")
                                    .withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(" to change this or access other ready-made gameplay and performance settings presets.")
                                    .withStyle(ChatFormatting.WHITE))
            );
            showFirstMessage = false;
        }

        //#if FABRIC
        FriendlyByteBuf buf = PacketByteBufs.create();

        FlowingFluids.config.encodeToByteBuffer(buf);
        ServerPlayNetworking.send(player,
                //#if MC > 12001
                traben.flowing_fluids.networking.FFConfigDataFabric.read(buf)
                //#else
                //$$ FFConfig.SERVER_CONFIG_PACKET_ID, buf
                //#endif
        );
        //#elseif FORGE
            //#if MC > 12001
            //$$ ForgePacketHandler.INSTANCE.send(new ForgePacketHandler.FFConfigPacket(), PacketDistributor.PLAYER.with(player));
            //#else
            //$$ ForgePacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(()-> player),new ForgePacketHandler.FFConfigPacket());
            //#endif
        //#else
        //$$ PacketDistributor.sendToPlayer(player, new FFConfigDataNeoForge());
        //#endif

        FlowingFluids.info("- Sending server config to [" + player.getName().getString() + "]");
    }

    public static Path getConfigDirectory() {
        //#if FABRIC
        return FabricLoader.getInstance().getConfigDir();
        //#else
        //$$ return FMLPaths.GAMEDIR.get().resolve(FMLPaths.CONFIGDIR.get());
        //#endif
    }

    public static boolean isThisModLoaded(String modId) {
        //#if FABRIC
        return FabricLoader.getInstance().isModLoaded(modId);
        //#else
        //$$ try {
        //$$     ModList list = ModList.get();
        //$$     if (list != null) {
        //$$         return list.isLoaded(modId);
        //$$     } else {
        //$$         LoadingModList list2 = LoadingModList.get();
        //$$         if (list2 != null) {
        //$$             return list2.getModFileById(modId) != null;
        //$$         } else {
        //$$             error("Forge ModList checking failed!");
        //$$         }
        //$$     }
        //$$ } catch (Exception e) {
        //$$     error("Forge ModList checking failed, via exception!");
        //$$ }
        //$$ return false;
        //#endif
    }

    public static List<String> modsLoaded() {
        //#if FABRIC
        return FabricLoader.getInstance().getAllMods().stream().map(modContainer -> modContainer.getMetadata().getId()).toList();
        //#else
        //$$ try {
        //$$     ModList list = ModList.get();
        //$$     if (list != null) {
        //$$         return list.getMods().stream().map(IModInfo::getModId).toList();
        //$$     } else {
        //$$         LoadingModList list2 = LoadingModList.get();
        //$$         if (list2 != null) {
        //$$             return list2.getModFiles().stream()
        //$$                     .flatMap(it -> it.getMods().stream())
        //$$                     .map(IModInfo::getModId)
        //$$                     .toList();
        //$$         } else {
        //$$             error("Forge ModList checking failed!");
        //$$         }
        //$$     }
        //$$ } catch (Exception e) {
        //$$     error("Forge ModList checking failed, via exception!");
        //$$ }
        //$$ return List.of();
        //$$
        //#endif
    }

    @SuppressWarnings("unused")
    public static boolean isForge() {
        return !isFabric();
    }

    public static boolean isFabric() {
        //#if FABRIC
        return true;
        //#else
        //$$ return false;
        //#endif
    }

    public static void loadConfig() {
        File configFile = new File(getConfigDirectory().toFile(), "flowing_fluids.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();

        if (configFile.exists()) {
            try {
                FileReader fileReader = new FileReader(configFile);
                config = gson.fromJson(fileReader, FFConfig.class);
                fileReader.close();
                //saveConfig();
            } catch (IOException e) {
                // ETFUtils.logMessage("Config could not be loaded, using defaults", false);
            }
        } else {
            config = new FFConfig();
            // only time client side ever calls this
            saveConfig();
        }
    }

    public static void saveConfig() {
        // only trigger on client side when loading defaults
        File configFile = new File(getConfigDirectory().toFile(), "flowing_fluids.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (!configFile.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            configFile.getParentFile().mkdirs();
        }
        try {
            FileWriter fileWriter = new FileWriter(configFile);
            fileWriter.write(gson.toJson(config));
            fileWriter.close();
        } catch (IOException ignored) {}
    }
}
