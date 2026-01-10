package traben.flowing_fluids.mixin.mixins;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.auto_perf.FFAutoPerformance;
import traben.flowing_fluids.config.auto_perf.PerformanceSpeed;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MixinServer_AutoPerformance {

    //#if MC >= 12100
    @Shadow public abstract net.minecraft.server.ServerTickRateManager tickRateManager();
    @Shadow public abstract float getCurrentSmoothedTickTime();
    //#else
    //$$ @Shadow public abstract float getAverageTickTime();
    //#endif
    @Unique
    private long lastSysTimeAdjusted = 0;

    @Inject(method = "stopServer", at = @At(value = "HEAD"))
    private void ff$resetAutoPerformance(CallbackInfo ci) {
        if (!FlowingFluids.config.enableMod || !FlowingFluids.config.autoPerformanceMode.enabled())
            return;

        // reset auto to default on server close
        FFAutoPerformance.resetAuto();
        FlowingFluids.saveConfig();

        lastSysTimeAdjusted = 0;
    }

    @Inject(method = "tickServer", at = @At(value = "TAIL"))
    private void ff$auto_adjust(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        if (!FlowingFluids.config.enableMod || !FlowingFluids.config.autoPerformanceMode.enabled())
            return;

        long time = System.currentTimeMillis();
        if (time - lastSysTimeAdjusted < FlowingFluids.config.autoPerformanceUpdateRateSeconds * 1000L) return;

        if (lastSysTimeAdjusted == 0) {
            // first run, old config may have been retained from last session via crash or who knows what, it's easier to reset always than try to parse
            FFAutoPerformance.resetAuto();
            FlowingFluids.saveConfig();
        }

        lastSysTimeAdjusted = time;

        //#if MC >= 12100
        var tickManager = tickRateManager();
        var msptGoal = tickManager.millisecondsPerTick() * FlowingFluids.config.autoPerformanceMSPTargetMultiplier;
        var mspt = getCurrentSmoothedTickTime();
        //#else
        //$$ var msptGoal = 50f * FlowingFluids.config.autoPerformanceMSPTargetMultiplier;
        //$$ var mspt = getAverageTickTime();
        //#endif

        int initialLevel = FFAutoPerformance.level();
        int level = initialLevel;

        if (mspt <= msptGoal * 0.5) { // only lower if we can maybe handle it
            if (level <= 0) {
                FFAutoPerformance.tickUnchanged(mspt);
                return;
            }
            level--;

            // extra decrements for great performance
            if (mspt <= msptGoal * 0.25) level--;
            if (mspt <= msptGoal * 0.125) level--;
            if (mspt <= msptGoal * 0.0625) level--; // doubt (tm)
            if (level < 0) level = 0;
        } else if (mspt >= msptGoal) {
            if (level >= PerformanceSpeed.max()) {
                FFAutoPerformance.tickUnchanged(mspt);
                return;
            }
            level++;

            // extra increments for terrible performance
            for (int multiplier = 2; multiplier <= 6; multiplier++) {
                if (mspt >= msptGoal * multiplier) level++;
            }
            if (level > PerformanceSpeed.max()) level = PerformanceSpeed.max();

        } else {
            FFAutoPerformance.tickUnchanged(mspt);
            return;
        }

        FFAutoPerformance.setLevel(level, mspt);
        var preset = FFAutoPerformance.current();

        preset.apply(FlowingFluids.config);

        if (FlowingFluids.config.autoPerformanceShowMessages) {
            boolean isMaxed = level >= PerformanceSpeed.max();
            logSeverity(isMaxed ,
                    "[Flowing Fluids] Auto Performance Handling: {} mode's fluid tick rate {} to {} from {} | mspt {} / {} | {} (You can disable this logging in the settings)",
                    FlowingFluids.config.autoPerformanceMode.pretty2(),
                    (level < initialLevel) ? "increased" : "decreased",
                    preset.commandName.toLowerCase(),
                    FFAutoPerformance.getForModeAndLevel(FlowingFluids.config.autoPerformanceMode, initialLevel).commandName.toLowerCase(),
                    mspt,
                    //#if MC >= 12100
                    tickManager.millisecondsPerTick(),
                    //#else
                    //$$ 50,
                    //#endif
                    isMaxed ? "Auto Performance cannot set the tick rate any slower, consider lowering your auto performance quality mode if this is still not enough"
                            : level <= 0
                                ? "Auto Performance is running at maximum tick rate, Nice :)"
                                : "Auto Performance still has wiggle room to speed up or slow down the tick rate of fluids"
                    );
        }
    }

    @Unique
    private void logSeverity(boolean warn, String format, Object... arguments) {
        if (warn) {
            FlowingFluids.LOG.warn(format, arguments);
        } else {
            FlowingFluids.LOG.info(format, arguments);
        }
    }

}
