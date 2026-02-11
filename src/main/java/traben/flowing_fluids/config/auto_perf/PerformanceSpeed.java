package traben.flowing_fluids.config.auto_perf;

import traben.flowing_fluids.config.FFConfig;

public enum PerformanceSpeed {
    NORMAL,
    SLOW_1,
    SLOW_2,
    SLOW_3,
    SLOW_4,
    SLOW_5,
    SLOW_6,
    SLOW_7,
    SLOW_8,
    SLOW_9;


    public static int max() {
        return values().length - 1;
    }

    public String explain() {
        return "\nThis has fluid flow/update rates " + switch (this) {
            case NORMAL -> "at the default/normal speeds.";
            case SLOW_1 -> "a little slower than default speeds.";
            case SLOW_2 -> "slower than default speeds.";
            case SLOW_3 -> "much slower than default speeds.";
            case SLOW_4 -> "extremely slower than default speeds.";
            case SLOW_5 -> "ridiculously slower than default speeds.";
            case SLOW_6 -> "unimaginably slower than default speeds.";
            case SLOW_7 -> "staggeringly slower than default speeds.";
            case SLOW_8 -> "unbelievably slower than default speeds.";
            case SLOW_9 -> "apocalyptically slower than default speeds.";
        };
    }

    public void apply(FFConfig config) {
        switch (this) {
            case NORMAL -> {}
            case SLOW_1 -> {
                config.waterTickDelay = 4;
                config.lavaNetherTickDelay = 8;
                config.lavaTickDelay = 18;
            }
            case SLOW_2 -> {
                config.waterTickDelay = 6;
                config.lavaNetherTickDelay = 10;
                config.lavaTickDelay = 20;
                // from here on out the tickDelaySpread will affect fluid behaviors but is desperately needed to prevent
                // fluid ticks from syncing up into lag spikes
                config.tickDelaySpread = 1;
            }
            case SLOW_3 -> {
                config.waterTickDelay = 8;
                config.lavaNetherTickDelay = 12;
                config.lavaTickDelay = 24;
                config.tickDelaySpread = 1;
            }
            case SLOW_4 -> {
                config.waterTickDelay = 10;
                config.lavaNetherTickDelay = 14;
                config.lavaTickDelay = 28;
                config.tickDelaySpread = 2;
            }
            case SLOW_5 -> {
                config.waterTickDelay = 12;
                config.lavaNetherTickDelay = 16;
                config.lavaTickDelay = 32;
                config.tickDelaySpread = 2;
            }
            case SLOW_6 -> {
                config.waterTickDelay = 14;
                config.lavaNetherTickDelay = 18;
                config.lavaTickDelay = 36;
                config.tickDelaySpread = 3;
            }
            case SLOW_7 -> {
                config.waterTickDelay = 16;
                config.lavaNetherTickDelay = 20;
                config.lavaTickDelay = 40;
                config.tickDelaySpread = 3;
            }
            case SLOW_8 -> {
                config.waterTickDelay = 18;
                config.lavaNetherTickDelay = 22;
                config.lavaTickDelay = 44;
                config.tickDelaySpread = 4;
            }
            case SLOW_9 -> {
                config.waterTickDelay = 20; // 1 tick per second seems like a decent enough cutoff here
                config.lavaNetherTickDelay = 24;
                config.lavaTickDelay = 48;
                config.tickDelaySpread = 4;
            }
        }
    }
}
