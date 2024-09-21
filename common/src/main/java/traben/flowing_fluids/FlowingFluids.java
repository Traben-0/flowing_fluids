package traben.flowing_fluids;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import traben.flowing_fluids.config.FFConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FlowingFluids {
    public static final String MOD_ID = "flowing_fluids";

    public final static Logger LOG = LoggerFactory.getLogger("FlowingFluids");
    private static final List<Direction> CARDINALS = new ArrayList<>();
    private static final List<Direction> CARDINALS_AND_DOWN = new ArrayList<>();
    public static BigDecimal totalDebugMilliseconds = BigDecimal.valueOf(0);
    public static long totalDebugTicks = 0;
    public static boolean isManeuveringFluids = false;

    public static FFConfig config = new FFConfig();

    public static void init() {
        FlowingFluids.LOG.info("FlowingFluids initialising");

        CARDINALS.add(Direction.NORTH);
        CARDINALS.add(Direction.SOUTH);
        CARDINALS.add(Direction.EAST);
        CARDINALS.add(Direction.WEST);

        CARDINALS_AND_DOWN.addAll(CARDINALS);
        CARDINALS_AND_DOWN.add(Direction.DOWN);


        loadConfig();

    }

    public static List<Direction> getCardinalsShuffle(RandomSource random) {
        Collections.shuffle(CARDINALS, random::nextInt);
        return CARDINALS;
    }

    public static List<Direction> getCardinalsAndDownShuffle(RandomSource random) {
        Collections.shuffle(CARDINALS_AND_DOWN, random::nextInt);
        return CARDINALS_AND_DOWN;
    }

    public static double getAverageDebugMilliseconds() {
        return totalDebugMilliseconds.divide(BigDecimal.valueOf(totalDebugTicks), 2, RoundingMode.HALF_UP).doubleValue();
    }

    public static void loadConfig() {
        File configFile = new File(FlowingFluidsPlatform.getConfigDirectory().toFile(), "flowing_fluids.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (configFile.exists()) {
            try {
                FileReader fileReader = new FileReader(configFile);
                config = gson.fromJson(fileReader, FFConfig.class);
                fileReader.close();
                //saveConfig();
            } catch (IOException e) {
                //ETFUtils.logMessage("Config could not be loaded, using defaults", false);
            }
        } else {
            config = new FFConfig();
            //only time client side ever calls this
            saveConfig();
        }
    }

    public static void saveConfig() {
        //only trigger on client side when loading defaults
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
        } catch (IOException ignored) {
        }
    }

}
