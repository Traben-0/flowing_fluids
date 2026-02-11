package traben.flowing_fluids;

import traben.flowing_fluids.config.FFCommands;

//#if FABRIC
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class FlowingFluidsInit implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(FFCommands::registerCommands);
        //#if MC > 12001
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
                traben.flowing_fluids.networking.FFConfigDataFabric.type,
                traben.flowing_fluids.networking.FFConfigDataFabric.CODEC);
        //#endif
        FlowingFluids.start();
    }
}
//#elseif FORGE
//$$ import net.minecraft.client.gui.screens.Screen;
//$$ import net.minecraftforge.api.distmarker.Dist;
//$$ import net.minecraftforge.client.ConfigScreenHandler;
//$$ import net.minecraftforge.fml.IExtensionPoint;
//$$ import net.minecraftforge.fml.ModLoadingContext;
//$$ import net.minecraftforge.fml.common.Mod;
//$$ import net.minecraftforge.fml.loading.FMLEnvironment;
//$$ import java.util.function.Function;
//$$ import net.minecraftforge.common.MinecraftForge;
//$$ import net.minecraftforge.event.RegisterCommandsEvent;
//$$ import traben.flowing_fluids.networking.ForgePacketHandler;
    //#if MC >= 12106
    //$$ import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
    //#else
    //$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
    //#endif
//$$
//$$ @net.minecraftforge.fml.common.Mod("flowing_fluids")
//$$ public class FlowingFluidsInit {
//$$     public FlowingFluidsInit() {
//$$         // Run our common setup.
//$$         ForgePacketHandler.init();
//$$         FlowingFluids.start();
//$$         MinecraftForge.EVENT_BUS.register(FlowingFluidsInit.class);
//$$     }
//$$
//$$     @SubscribeEvent
//$$     public static void onRegisterCommandEvent(RegisterCommandsEvent event) {
//$$         FlowingFluids.info("commands registered");
//$$         FFCommands.registerCommands(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
//$$     }
//$$ }
//#else
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.gui.screens.Screen;
//$$ import net.neoforged.fml.ModContainer;
//$$ import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
//$$ import net.neoforged.bus.api.SubscribeEvent;
//$$ import net.neoforged.fml.common.EventBusSubscriber;
//$$ import net.neoforged.neoforge.common.NeoForge;
//$$ import net.neoforged.neoforge.event.RegisterCommandsEvent;
//$$ import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
//$$ import net.neoforged.neoforge.network.registration.PayloadRegistrar;
//$$ import traben.flowing_fluids.networking.FFConfigData;
//$$ import traben.flowing_fluids.networking.FFConfigDataNeoForge;
//$$
//$$ import java.util.List;
//$$
//$$ @net.neoforged.fml.common.Mod("flowing_fluids")
//$$ public class FlowingFluidsInit {
//$$     public FlowingFluidsInit() {
//$$         NeoForge.EVENT_BUS.register(FlowingFluidsInit.class);
//$$     }
//$$
//$$     @SubscribeEvent
//$$     public static void onRegisterCommandEvent(RegisterCommandsEvent event) {
//$$         FlowingFluids.info("commands registered");
//$$         FFCommands.registerCommands(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
//$$     }
//$$ }
//$$
    //#if MC >= 12106
    //$$ @EventBusSubscriber(modid = "flowing_fluids")
    //#else
    //$$ @EventBusSubscriber(modid = "flowing_fluids", bus = EventBusSubscriber.Bus.MOD)
    //#endif
//$$ class ModRegister {
//$$     @SubscribeEvent
//$$     public static void onPayloadRegister(RegisterPayloadHandlersEvent event) {
//$$         PayloadRegistrar registrar = event.registrar("flowing_fluids");
//$$         registrar.playToClient(FFConfigData.type, FFConfigDataNeoForge.CODEC, (data, b) -> {
//$$             try {
//$$                 if (data.isValid()) {
//$$                     FlowingFluids.config = data.delegate;
//$$
//$$                     FlowingFluids.info("- Server Config data received and synced");
//$$                 } else {
//$$                     FlowingFluids.error("- Server Config data received and failed to sync, invalid data");
//$$                     throw new RuntimeException("[Flowing Fluids] - Server Config data received and failed to sync, invalid data");
//$$                 }
//$$             } catch (Exception e) {
//$$                 FlowingFluids.error("- Server Config data received and failed to sync, exception");
//$$                 throw new RuntimeException("[Flowing Fluids] - Server Config data received and failed to sync, exception", e);
//$$             }
//$$         });
//$$     }
//$$ }
//#endif
