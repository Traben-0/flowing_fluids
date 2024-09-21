package traben.flowing_fluids.neoforge;

import traben.flowing_fluids.FlowingFluids;
import net.neoforged.fml.common.Mod;

@Mod(FlowingFluids.MOD_ID)
public final class FlowingFluidsNeoForge {
    public FlowingFluidsNeoForge() {
        // Run our common setup.
        FlowingFluids.init();
    }
}
