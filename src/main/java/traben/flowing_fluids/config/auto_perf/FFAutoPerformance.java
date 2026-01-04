package traben.flowing_fluids.config.auto_perf;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfig;
import traben.flowing_fluids.config.FFConfigPreset;

import java.util.ArrayList;
import java.util.Map;

@SuppressWarnings("unused")
public abstract class FFAutoPerformance {

    private static final Map<FFConfig.AutoPerformance, FFConfigPreset[]> autoPerformanceMap = new Object2ObjectOpenHashMap<>();

    public static FFConfigPreset getForModeAndLevel(FFConfig.AutoPerformance mode, int level /* 0..3 */) {
        return autoPerformanceMap.get(mode)[level];
    }

    public static void resetAuto() {
        assert FlowingFluids.config.autoPerformanceMode.enabled();
        getForModeAndLevel(FlowingFluids.config.autoPerformanceMode, 0).apply(FlowingFluids.config);
    }

    public static void registerPerformanceTypes() {
        for (PerformanceQuality quality : PerformanceQuality.values()) {
            var list = new ArrayList<FFConfigPreset>();
            for (PerformanceSpeed speed : PerformanceSpeed.values()) {
                list.add(new FFConfigPreset("Auto performance: quality " + quality + ", speed " + speed + "." + quality.explain() + speed.explain() +
                        "\nThis setting will only change some config settings and will leave the rest as is.",
                        "~auto_perf_" + quality.name().toLowerCase() + "_" + speed.name().toLowerCase()) {
                    @Override
                    public void apply(FFConfig config) {
                        resetPerformanceSettings(config);
                        speed.apply(config);
                        quality.apply(config);
                    }
                });
            }
            autoPerformanceMap.put(quality.toConfigType(), list.toArray(new FFConfigPreset[0]));
        }
    }

    private static void resetPerformanceSettings(FFConfig config) {
        var newConfig = new FFConfig();
        config.waterTickDelay = newConfig.waterTickDelay;
        config.lavaNetherTickDelay = newConfig.lavaNetherTickDelay;
        config.lavaTickDelay = newConfig.lavaTickDelay;
        config.playerBlockDistanceForFlowing = newConfig.playerBlockDistanceForFlowing;
        config.waterFlowDistance = newConfig.waterFlowDistance;
        config.lavaFlowDistance = newConfig.lavaFlowDistance;
        config.lavaNetherFlowDistance = newConfig.lavaNetherFlowDistance;
        config.randomTickLevelingDistance = newConfig.randomTickLevelingDistance;
        config.displacementDepthMultiplier = newConfig.displacementDepthMultiplier;
        config.tickDelaySpread = newConfig.tickDelaySpread;
    }

}

