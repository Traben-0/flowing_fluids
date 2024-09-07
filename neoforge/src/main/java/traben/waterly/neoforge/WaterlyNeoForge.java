package traben.waterly.neoforge;

import traben.waterly.Waterly;
import net.neoforged.fml.common.Mod;

@Mod(Waterly.MOD_ID)
public final class WaterlyNeoForge {
    public WaterlyNeoForge() {
        // Run our common setup.
        Waterly.init();
    }
}
