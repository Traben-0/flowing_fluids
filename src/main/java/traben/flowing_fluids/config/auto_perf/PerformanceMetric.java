package traben.flowing_fluids.config.auto_perf;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Arrays;

public class PerformanceMetric {

    public static final Int2ObjectOpenHashMap<PerformanceMetric> metrics = new Int2ObjectOpenHashMap<>();

    static {
        resetMetrics();
    }

    public static void resetMetrics() {
        metrics.clear();
        for (PerformanceSpeed value : PerformanceSpeed.values()) {
            metrics.put(value.ordinal(), new PerformanceMetric(value.ordinal()));
        }
    }


    final int level;

    int intervalsAtLevel = 0;
    private final float[] msptHistory = new float[100];

    PerformanceMetric(int level) {
        this.level = level;
        Arrays.fill(msptHistory, -1f);
    }

    void tick(float mspt) {
        msptHistory[intervalsAtLevel % 100] = mspt;
        intervalsAtLevel++;
    }

    float averageMspt() {
        float total = 0f;
        int count = 0;
        for (float f : msptHistory) {
            if (f >= 0f) {
                total += f;
                count++;
            }
        }
        return count == 0 ? 0f : total / count;
    }
}
