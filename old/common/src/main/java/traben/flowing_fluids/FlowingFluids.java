package traben.flowing_fluids;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import traben.flowing_fluids.config.FFConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public final class FlowingFluids {
    public static final String MOD_ID = "flowing_fluids";

    public final static Logger LOG = LoggerFactory.getLogger("FlowingFluids");
    public static boolean isManeuveringFluids = false;
    public static boolean pistonTick = false;
    public static long debug_killFluidUpdatesUntilTime = 0;
    public static int waterPluggedThisSession = 0;

    public static Set<Pair<Fluid,TagKey<Block>>> nonDisplacerTags = new HashSet<>();
    public static Set<Pair<Fluid,Block>> nonDisplacers = new HashSet<>();
    public static Set<TagKey<Biome>> infiniteBiomeTags = new HashSet<>();
    public static Set<ResourceKey<Biome>> infiniteBiomes = new HashSet<>();

    public static FFConfig config = new FFConfig();
    
    public static void info(String str) { LOG.info("[Flowing Fluids] {}", str); }
    public static void warn(String str) { LOG.warn("[Flowing Fluids] {}", str); }
    public static void error(String str) { LOG.error("[Flowing Fluids] {}", str); }

    public static void init() {
        info("initialising");

        infiniteBiomeTags.add(BiomeTags.IS_OCEAN);
        infiniteBiomeTags.add(BiomeTags.IS_RIVER);
        infiniteBiomeTags.add(BiomeTags.IS_BEACH);
        infiniteBiomes.add(Biomes.SWAMP);
        infiniteBiomes.add(Biomes.MANGROVE_SWAMP);

        nonDisplacerTags.add(Pair.of(Fluids.WATER, BlockTags.ICE));
        nonDisplacers.add(Pair.of(Fluids.WATER,Blocks.SPONGE));
        nonDisplacers.add(Pair.of(Fluids.LAVA,Blocks.OBSIDIAN));

        loadConfig();
    }


    public static void loadConfig() {
        File configFile = new File(FlowingFluidsPlatform.getConfigDirectory().toFile(), "flowing_fluids.json");
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
        File configFile = new File(FlowingFluidsPlatform.getConfigDirectory().toFile(), "flowing_fluids.json");
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
