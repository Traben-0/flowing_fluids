package traben.flowing_fluids.forge;

import net.minecraftforge.fml.common.Mod;
import traben.flowing_fluids.FlowingFluids;


@Mod(FlowingFluids.MOD_ID)
public final class FlowingFluidsForge {
    public FlowingFluidsForge() {
        // Run our common setup.
        FlowingFluids.init();
    }
}
