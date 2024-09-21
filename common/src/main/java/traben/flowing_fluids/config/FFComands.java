package traben.flowing_fluids.config;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.FlowingFluidsPlatform;

import java.math.BigDecimal;

public class FFComands {
    private static int messageAndSaveConfig(CommandContext<CommandSourceStack> context, String text) {
        FlowingFluids.saveConfig();
        context.getSource().getServer().getPlayerList().getPlayers().forEach(FlowingFluidsPlatform::sendConfigToClient);
        return message(context, text);
    }

    private static int message(CommandContext<CommandSourceStack> context, String text) {
        //always executed server side
        String inputCommand = context.getInput();
        context.getSource().sendSystemMessage(Component.literal("\n§7§o/" + inputCommand + "§r\n" + text + "\n§7_____________________________"));
        return 1;
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, @SuppressWarnings("unused") CommandBuildContext var2, @SuppressWarnings("unused") Commands.CommandSelection var3) {
        dispatcher.register(Commands.literal("flowing_fluids")
                .requires(source -> source.hasPermission(4) || (source.getServer().isSingleplayer() && source.getPlayer() != null && source.getServer().isSingleplayerOwner(source.getPlayer().getGameProfile()))
                ).then(Commands.literal("help")
                        .executes(c -> message(c, "Use any of the commands without adding any of it's arguments, E.G '/flowing_fluids enable_mod', to get a description of what the command does and it's current value."))
                ).then(Commands.literal("enable_mod")
                        .then(Commands.literal("on")
                                .executes(cont -> {
                                    FlowingFluids.config.enableMod = true;
                                    return messageAndSaveConfig(cont, "FlowingFluids is now enabled, liquids will now have physics using : " + (FlowingFluids.config.fastmode ? "Fast mode." : "Quality mode."));
                                })
                        ).then(Commands.literal("off")
                                .executes(cont -> {
                                    FlowingFluids.config.enableMod = false;
                                    return messageAndSaveConfig(cont, "FlowingFluids is now disabled, vanilla liquid behaviour will be restored, Buckets will retain their partial fill amount until used.");
                                })
                        )
                ).then(Commands.literal("flowing_texture")
                        .executes(cont -> message(cont, "The flowing fluid texture is currently " + (FlowingFluids.config.hideFlowingTexture ? "hidden." : "shown.") + "\n This will make the fluids surface appear more still and less flickery while settling, this might conflict with any mod affecting fluid rendering"))
                        .then(Commands.literal("hidden")
                                .executes(cont -> {
                                    FlowingFluids.config.hideFlowingTexture = true;
                                    return messageAndSaveConfig(cont, "Flowing fluid texture is now hidden.\nLiquids will no longer show the flowing texture on their surface.");
                                })
                        ).then(Commands.literal("shown")
                                .executes(cont -> {
                                    FlowingFluids.config.hideFlowingTexture = false;
                                    return messageAndSaveConfig(cont, "Flowing fluid texture is now visible.\nLiquids will now show the flowing texture on their surface.");
                                })
                        )
                ).then(Commands.literal("fast_mode")
                        .executes(cont -> message(cont, "Fast mode is currently " + (FlowingFluids.config.fastmode ? "enabled." : "disabled.") + "\n Fast mode changes how liquids behave, and can be toggled on or off.\nFast mode will reduce the amount of checks liquids do to spread, changing from looking for edges 4 blocks away, to only 1, and may cause liquids to pool more frequently in places.\nIn a worst case scenario Fast mode improves water spread lag by 40 times, in actual practise this tends to vary around the 2-20 times faster."))
                        .then(Commands.literal("on")
                                .executes(cont -> {
                                    FlowingFluids.config.fastmode = true;
                                    return messageAndSaveConfig(cont, "Fast mode is now enabled.\nLiquids will no longer check if they can move further than 1 block away from itself and may pool more frequently in places.\nThis reduces sideways spread positional checking from 4 - 160 times per update, down to only 4 times.");
                                })
                        ).then(Commands.literal("off")
                                .executes(cont -> {
                                    FlowingFluids.config.fastmode = false;
                                    return messageAndSaveConfig(cont, "Fast mode is now disabled.\nLiquids will now check if they can move up to 4 blocks away from itself and will pool less frequently.\nThis increases sideways spread positional checking from 4 times per update, to up to 160 times.");
                                })
                        )
                ).then(Commands.literal("pistons_push_fluids")
                        .executes(cont -> message(cont, "Piston pushing is currently " + (FlowingFluids.config.enablePistonPushing ? "enabled." : "disabled.")))
                        .then(Commands.literal("on")
                                .executes(cont -> {
                                    FlowingFluids.config.enablePistonPushing = true;
                                    return messageAndSaveConfig(cont, "Piston pushing is now enabled.\nLiquids will now be pushed by pistons.");
                                })
                        ).then(Commands.literal("off")
                                .executes(cont -> {
                                    FlowingFluids.config.enablePistonPushing = false;
                                    return messageAndSaveConfig(cont, "Piston pushing is now disabled.\nLiquids will no longer be pushed by pistons.");
                                })
                        )
                ).then(Commands.literal("placed_blocks_displace_fluids")
                        .executes(cont -> message(cont, "Placed blocks displacing fluids is currently " + (FlowingFluids.config.enableDisplacement ? "enabled." : "disabled.")))
                        .then(Commands.literal("on")
                                .executes(cont -> {
                                    FlowingFluids.config.enableDisplacement = true;
                                    return messageAndSaveConfig(cont, "Placed blocks displacing fluids is now enabled.\nLiquids will now be displaced by blocks placed inside them.");
                                })
                        ).then(Commands.literal("off")
                                .executes(cont -> {
                                    FlowingFluids.config.enableDisplacement = false;
                                    return messageAndSaveConfig(cont, "Placed blocks displacing fluids is now disabled.\nLiquids will no longer be displaced by blocks placed inside them.");
                                })
                        )
                ).then(Commands.literal("leveling_behaviour")
                        .executes(cont -> message(cont, "Controls how liquids split when flowing, if a block with 5 water levels splits its flow to its neighbour it will have 1 remaining, this chooses what to do with it: " + FlowingFluids.config.levelingBehaviour))
                        .then(Commands.literal("vanilla")
                                .executes(cont -> {
                                    FlowingFluids.config.levelingBehaviour = FFConfig.LevelingBehaviour.VANILLA_LIKE;
                                    return messageAndSaveConfig(cont, "Liquids will consider themselves 'level' when their neighbours are the same level or 1 level lower. This means water wont flow more than 8 blocks, like vanilla :).");
                                })
                        ).then(Commands.literal("lazy")
                                .executes(cont -> {
                                    FlowingFluids.config.levelingBehaviour = FFConfig.LevelingBehaviour.LAZY_LEVEL;
                                    return messageAndSaveConfig(cont, "Liquids will lazily try to enforce 'true level' however, if a pool of 5 high water has a single block of level 6, instead of wandering endlessly, it will eventually randomly stop looking for a gap to fit in, with a high random chance to stop.");
                                })
                        ).then(Commands.literal("strong")
                                .executes(cont -> {
                                    FlowingFluids.config.levelingBehaviour = FFConfig.LevelingBehaviour.STRONG_LEVEL;
                                    return messageAndSaveConfig(cont, "Liquids will strongly try to enforce 'true level', if a pool of 5 high water has a single block of level 6, the extra level will wander near endlessly, it will eventually randomly stop looking for a gap to fit in, with a low random chance to stop.");
                                })
                        ).then(Commands.literal("force")
                                .executes(cont -> {
                                    FlowingFluids.config.levelingBehaviour = FFConfig.LevelingBehaviour.FORCE_LEVEL;
                                    return messageAndSaveConfig(cont, "Liquids will always try to enforce 'true level' if a pool of 5 high water has a single block of level 6, it will tirelessly wander the entire pool looking for a chance to flow down and level it self.\nThis is the most realistic but not recommended for lag reasons, consider either strong or lazy leveling instead.");
                                })
                        )
                ).then(Commands.literal("flow_to_edge_behaviour")
                        .executes(cont -> message(cont, "Controls if liquids flow to nearby edges when at their minimum height. Currently they: " + (FlowingFluids.config.flowToEdges ? "flow." : "stay.")))
                        .then(Commands.literal("flow")
                                .executes(cont -> {
                                    FlowingFluids.config.flowToEdges = true;
                                    return messageAndSaveConfig(cont, "Liquids at their minimum height will now flow to and over nearby edges, up to 4 blocks away if fast mode is disabled, or 1 block away if fast mode is enabled.");
                                })
                        ).then(Commands.literal("stay")
                                .executes(cont -> {
                                    FlowingFluids.config.flowToEdges = false;
                                    return messageAndSaveConfig(cont, "Liquids at their minimum height will no longer flow to and over nearby edges.");
                                })
                        )
                ).then(Commands.literal("water_puddle_evaporation_chance")
                        .executes(cont -> message(cont, "Sets the chance of small minimum level water tiles evaporating during random ticks, currently set to " + FlowingFluids.config.evaporationChance))
                        .then(Commands.argument("chance", FloatArgumentType.floatArg(0, 1))
                                .executes(cont -> {
                                    FlowingFluids.config.evaporationChance = cont.getArgument("chance", Float.class);
                                    return messageAndSaveConfig(cont, "Water puddle evaporation chance set to " + FlowingFluids.config.evaporationChance);
                                })
                        )
                ).then(Commands.literal("water_rain_refill_chance")
                        .executes(cont -> message(cont, "Sets the chance of non-full water tiles increasing their level while its rains and they are open to the sky, during random ticks. This provides access to renewable water given enough time. Currently set to " + FlowingFluids.config.rainRefillChance))
                        .then(Commands.argument("chance", FloatArgumentType.floatArg(0, 1))
                                .executes(cont -> {
                                    FlowingFluids.config.rainRefillChance = cont.getArgument("chance", Float.class);
                                    return messageAndSaveConfig(cont, "Water rain refill chance set to " + FlowingFluids.config.rainRefillChance);
                                })
                        )
                ).then(Commands.literal("water_wet_biome_refill_chance")
                        .executes(cont -> message(cont, "Sets the chance of of non-full water tiles increasing their level within: Oceans, Rivers, and Swamps, during random ticks. Additionally they must have a sky light level higher than 0, and be between y=0 and sea level. This provides time limited access to infinite water within these biomes, granted they are big enough and not drained too quickly. Currently set to " + FlowingFluids.config.evaporationChance))
                        .then(Commands.argument("chance", FloatArgumentType.floatArg(0, 1))
                                .executes(cont -> {
                                    FlowingFluids.config.evaporationChance = cont.getArgument("chance", Float.class);
                                    return messageAndSaveConfig(cont, "Water set biome refill chance set to " + FlowingFluids.config.evaporationChance);
                                })
                        )
                ).then(Commands.literal("debug").executes(cont -> message(cont, "Debug commands you probably don't need these."))
                        .then(Commands.literal("spread")
                                .then(Commands.literal("print")
                                        .executes(cont -> {
                                            FlowingFluids.config.debugSpreadPrint = !FlowingFluids.config.debugSpreadPrint;
                                            return messageAndSaveConfig(cont, "debugSpread is " + (FlowingFluids.config.debugSpreadPrint ? "printing." : "not printing."));
                                        })
                                ).then(Commands.literal("toggle")
                                        .executes(cont -> {
                                            FlowingFluids.config.debugSpread = !FlowingFluids.config.debugSpread;
                                            FlowingFluids.totalDebugMilliseconds = BigDecimal.valueOf(0);
                                            FlowingFluids.totalDebugTicks = 0;
                                            return messageAndSaveConfig(cont, "debugSpread is " + (FlowingFluids.config.debugSpread ? "enabled." : "disabled."));
                                        })
                                ).then(Commands.literal("average_tick_length")
                                        .executes(cont -> message(cont, "average water spread tick length is : " + FlowingFluids.getAverageDebugMilliseconds() + "ms"))
                                )
                        ).then(Commands.literal("random_ticks_printing")
                                .executes(cont -> message(cont, "Random ticks printing is currently " + (FlowingFluids.config.printRandomTicks ? "enabled." : "disabled.")))
                                .then(Commands.literal("on")
                                        .executes(cont -> {
                                            FlowingFluids.config.printRandomTicks = true;
                                            return messageAndSaveConfig(cont, "Random ticks printing is now enabled.");
                                        })
                                ).then(Commands.literal("off")
                                        .executes(cont -> {
                                            FlowingFluids.config.printRandomTicks = false;
                                            return messageAndSaveConfig(cont, "Random ticks printing is now disabled.");
                                        })
                                )
                        )
                ).then(Commands.literal("reset_all_settings")
                        .executes(cont -> {
                            FlowingFluids.config = new FFConfig();
                            return messageAndSaveConfig(cont, "Flowing Fluids settings have been reset to default.");
                        })
                )

        );
    }
}
