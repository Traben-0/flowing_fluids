package traben.flowing_fluids.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFComands;


@Mod(FlowingFluids.MOD_ID)
public final class FlowingFluidsForge {
    public FlowingFluidsForge() {
        // Run our common setup.
        ForgePacketHandler.init();
        FlowingFluids.init();
        MinecraftForge.EVENT_BUS.register(FlowingFluidsForge.class);
    }

    @SubscribeEvent
    public static void onRegisterCommandEvent(RegisterCommandsEvent event) {
        FlowingFluids.LOG.info("[Flowing Fluids] commands registered");
        FFComands.registerCommands(event.getDispatcher(), null, null);
    }
}
