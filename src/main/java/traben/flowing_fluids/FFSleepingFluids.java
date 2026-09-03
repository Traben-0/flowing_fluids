package traben.flowing_fluids;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Fluid;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Opt-in "sleeping fluids" performance mode (config.sleepingFluids, default OFF).
 *
 * <p>Fluid ticks are suppressed unless the position was recently woken by a block update
 * (LiquidBlock neighborChanged/onPlace - i.e. placing/breaking blocks, buckets, pistons, and
 * the mod's own spreading, since every flow step is a block change). Sleeping fluid costs
 * nothing: the scheduled tick is consumed with no reschedule. Woken fluid behaves completely
 * normally, and active cascades keep themselves awake through their own block updates until
 * they physically settle, then lapse back to sleep.
 *
 * <p>Because a genuinely unsettleable body (e.g. a modded-worldgen ocean draining into cave
 * systems - see issues #40 / #49 / #81) keeps itself awake forever, wakefulness alone cannot
 * bound cost. So awake ticks are additionally budgeted per game tick
 * (config.sleepingFluidsMaxTicksPerTick); surplus ticks are deferred ~1.5-2.3 seconds with
 * positional jitter, letting huge cascades progress at a bounded, TPS-safe rate. Fluids within
 * config.sleepingFluidsNearPlayerRadius blocks of a player bypass the budget (while still
 * counting toward it), so interactive fluid always flows smoothly even when a distant cascade
 * saturates the budget - without this, a saturating cascade starves player-placed fluid into
 * total stillness (observed in testing).
 *
 * <p>All state is server-thread confined (wake events and fluid ticks both run there).
 */
public final class FFSleepingFluids {

    private FFSleepingFluids() {}

    /** How long a woken position stays tickable after its last block update. Active flow
     * refreshes this constantly through its own block changes. */
    private static final long WAKE_TTL_TICKS = 60;

    /** Deferred over-budget ticks retry after this many ticks (plus 0-15 positional jitter). */
    private static final int DEFER_DELAY_TICKS = 30;

    /** Opportunistic prune threshold/cadence for the wake map. */
    private static final int PRUNE_SIZE_THRESHOLD = 100_000;
    private static final long PRUNE_MIN_INTERVAL_TICKS = 200;

    /** posLong -> gameTime expiry, per level. Weak keys so closed levels drop naturally. */
    private static final Map<Level, Long2LongOpenHashMap> WAKE = new WeakHashMap<>();

    /** Per-level {lastGameTime, ticksUsedThisGameTick, lastPruneTime}. */
    private static final Map<Level, long[]> STATE = new WeakHashMap<>();

    private static Long2LongOpenHashMap map(final ServerLevel level) {
        return WAKE.computeIfAbsent(level, l -> new Long2LongOpenHashMap());
    }

    /** Marks a fluid position tickable. Called from LiquidBlock block-update hooks. */
    public static void wake(final LevelAccessor level, final BlockPos pos) {
        if (!FlowingFluids.config.sleepingFluids) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        long now = serverLevel.getGameTime();
        Long2LongOpenHashMap m = map(serverLevel);
        m.put(pos.asLong(), now + WAKE_TTL_TICKS);
        if (m.size() > PRUNE_SIZE_THRESHOLD) {
            long[] state = STATE.computeIfAbsent(serverLevel, l -> new long[3]);
            if (now - state[2] >= PRUNE_MIN_INTERVAL_TICKS) {
                state[2] = now;
                m.long2LongEntrySet().removeIf(e -> e.getLongValue() < now);
            }
        }
    }

    /**
     * Gate for scheduled fluid ticks; called from the mod's FlowingFluid tick handler.
     * @return true to process this tick normally. False means the tick was either swallowed
     *         (asleep - no reschedule, zero ongoing cost) or deferred (awake but over budget -
     *         rescheduled shortly, so large cascades progress at a bounded rate).
     */
    public static boolean allowScheduledTick(final Level level, final BlockPos pos, final Fluid fluid) {
        if (!FlowingFluids.config.sleepingFluids) return true;
        if (!(level instanceof ServerLevel serverLevel)) return true;

        long now = serverLevel.getGameTime();
        Long2LongOpenHashMap m = map(serverLevel);
        if (m.get(pos.asLong()) < now) {
            return false; // asleep: swallow outright
        }

        long[] state = STATE.computeIfAbsent(serverLevel, l -> new long[3]);
        if (state[0] != now) {
            state[0] = now;
            state[1] = 0;
        }

        // Interactive guarantee: near-player fluid always runs, and still counts toward the
        // budget so it takes priority over - rather than adding to - a distant cascade.
        int near = FlowingFluids.config.sleepingFluidsNearPlayerRadius;
        if (near > 0) {
            double nearSqr = (double) near * near;
            for (var player : serverLevel.players()) {
                if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < nearSqr) {
                    state[1]++;
                    return true;
                }
            }
        }

        if (state[1] >= FlowingFluids.config.sleepingFluidsMaxTicksPerTick) {
            // Over budget: defer instead of running. Jitter by position so the deferred wave
            // spreads out; extend the wake so the retry passes the gate.
            int jitter = (int) (pos.asLong() & 15);
            m.put(pos.asLong(), now + DEFER_DELAY_TICKS + jitter + WAKE_TTL_TICKS);
            serverLevel.scheduleTick(pos.immutable(), fluid, DEFER_DELAY_TICKS + jitter);
            return false;
        }
        state[1]++;
        return true;
    }

    /** Gate for the mod's ambient random-tick behaviours (leveling/evaporation/refill):
     * they only run for awake fluid while sleeping fluids is enabled. */
    public static boolean allowRandomTick(final Level level, final BlockPos pos) {
        if (!FlowingFluids.config.sleepingFluids) return true;
        if (!(level instanceof ServerLevel serverLevel)) return true;
        return map(serverLevel).get(pos.asLong()) >= serverLevel.getGameTime();
    }
}
