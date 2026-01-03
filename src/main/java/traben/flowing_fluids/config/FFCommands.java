package traben.flowing_fluids.config;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.PlugWaterFeature;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FFCommands {
    private static int messageAndSaveConfig(CommandContext<CommandSourceStack> context, String text) {
        FlowingFluids.saveConfig();
        context.getSource().getServer().getPlayerList().getPlayers().forEach(FlowingFluids::sendConfigToClient);
        return message(context, text);
    }

    private static int message(CommandContext<CommandSourceStack> context, String text) {
        //always executed server side
        String inputCommand = context.getInput();
        context.getSource().sendSystemMessage(Component.literal("\n§7§o/" + inputCommand + "§r\n" + text + "\n§7_____________________________"));
        return 1;
    }

    private static  LiteralArgumentBuilder<CommandSourceStack> floatChanceCommand(String name, String description, Consumer<Float> setter, Supplier<Float> getter) {
        return floatCommand(name, description, "chance", 0, 1, setter, getter);
    }

    private static  LiteralArgumentBuilder<CommandSourceStack> floatCommand(String name, String description, String argName, float min, float max, Consumer<Float> setter, Supplier<Float> getter) {
        return Commands.literal(name)
                .executes(cont -> message(cont, description + "\nCurrent value of " + name + " = " + getter.get()))
                .then(Commands.argument(argName, FloatArgumentType.floatArg(min, max))
                        .executes(cont -> {
                            setter.accept(cont.getArgument(argName, Float.class));
                            return messageAndSaveConfig(cont, name + " set to " + getter.get());
                        })
                );
    }

    private static  LiteralArgumentBuilder<CommandSourceStack> intCommand(String name, String description, String argName, int min, int max, Consumer<Integer> setter, Supplier<Integer> getter) {
        return Commands.literal(name)
                .executes(cont -> message(cont, description + "\nCurrent value of " + name + " = " + getter.get()))
                .then(Commands.argument(argName, IntegerArgumentType.integer(min, max))
                        .executes(cont -> {
                            setter.accept(cont.getArgument(argName, Integer.class));
                            return messageAndSaveConfig(cont, name + " set to " + getter.get());
                        })
                );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> booleanCommand(String name, String description, BooleanConsumer setter, BooleanSupplier getter) {
        return booleanCommand(name, description, name + " setting is now: On.", name + " setting is now: Off.", setter, getter);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> booleanCommand(String name, String description, String messageOn, String messageOff, BooleanConsumer setter, BooleanSupplier getter) {
        return Commands.literal(name)
                .executes(cont -> message(cont, description + "\n" + name + " is currently set to: " + (getter.getAsBoolean() ? "on" : "off")))
                .then(Commands.literal("on")
                        .executes(cont -> {
                            setter.accept(true);
                            return messageAndSaveConfig(cont, messageOn);
                        })
                ).then(Commands.literal("off")
                        .executes(cont -> {
                            setter.accept(false);
                            return messageAndSaveConfig(cont, messageOff);
                        })
                );
    }

    @SafeVarargs
    private static <E extends Enum<E>> LiteralArgumentBuilder<CommandSourceStack> enumCommand(String name, String description, Consumer<E> setter, Supplier<E> getter, Pair<E, String>... options) {
        var command = Commands.literal(name)
                .executes(cont -> message(cont, description + "\n" + name + " is currently set to: " + getter.get().toString().toLowerCase()));

        for (var option : options) {
            String message = option.getSecond();
            E enumVal = option.getFirst();
            command.then(Commands.literal(enumVal.toString().toLowerCase())
                    .executes(cont -> {
                        setter.accept(enumVal);
                        return messageAndSaveConfig(cont, message);
                    })
            );
        }
        return command;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> presets() {
        var presets = Commands.literal("settings_presets");
        presets.executes(cont ->
                message(cont, "These are preset configurations for Flowing Fluids, use these to quickly apply a set of settings suited to a particular playstyle or performance needs."));

        for (FFConfigPreset configPreset : FFConfigPreset.values()) {
            presets.then(Commands.literal(configPreset.name().toLowerCase())
                    .executes(cont -> {
                        configPreset.apply();
                        return messageAndSaveConfig(cont, configPreset.explanation+"\n---------\nApplied the preset: " + configPreset.name().toLowerCase());
                    })
                    .then(Commands.literal("explain")
                            .executes(cont -> message(cont, configPreset.explanation))
                    ).then(Commands.literal("apply_preset")
                            .executes(cont -> {
                                configPreset.apply();
                                return messageAndSaveConfig(cont, "Applied the preset: " + configPreset.name().toLowerCase());
                            })
                    )
            );
        }

        return presets;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> seaLevelOverrides() {
        return Commands.literal("sea_level_override")
                .executes(cont -> message(cont, "Allows overriding the current dimensions sea level as seen by the Flowing Fluids mod"))
                .then(Commands.literal("check_vanilla")
                        .executes(cont -> {
                            var level = cont.getSource().getLevel();
                            return message(cont, "The vanilla sea level as seen by Flowing Fluids for this dimension is " + level.getSeaLevel() + ".");
                        })
                ).then(Commands.literal("check_overrides")
                        .executes(cont -> {
                            var level = cont.getSource().getLevel();
                            int override = FlowingFluids.config.dimensionSeaLevelOverrides.getOrDefault(level.dimensionType().hashCode(), Integer.MIN_VALUE);
                            return message(cont, "The sea level override as seen by Flowing Fluids for this dimension is " + (override == Integer.MIN_VALUE ? "NO OVERRIDE SET" : override) + ". And for the default override it is set to: " + FlowingFluids.config.defaultSeaLevelOverride + ".");
                        })
                ).then(Commands.argument("set_dimension_override", IntegerArgumentType.integer(-999999, 999999))
                        .executes(cont -> {
                            int override = cont.getArgument("distance", Integer.class);
                            FlowingFluids.config.dimensionSeaLevelOverrides.put(cont.getSource().getLevel().dimensionType().hashCode(), override);
                            return messageAndSaveConfig(cont, "The sea level override for this dimension is now set to " + override + ". Warning! mod or datapack changes that alter dimension properties will unset this...");
                        })
                ).then(Commands.literal("clear_dimension_override")
                        .executes(cont -> {
                            FlowingFluids.config.dimensionSeaLevelOverrides.remove(cont.getSource().getLevel().dimensionType().hashCode());
                            return messageAndSaveConfig(cont, "The sea level override for this dimension has been removed.");
                        })
                ).then(Commands.argument("set_default_override", IntegerArgumentType.integer(-999999, 999999))
                        .executes(cont -> {
                            int override = cont.getArgument("distance", Integer.class);
                            FlowingFluids.config.defaultSeaLevelOverride = override;
                            return messageAndSaveConfig(cont, "The default sea level override is now set to " + override + ".");
                        })
                ).then(Commands.literal("clear_default_override")
                        .executes(cont -> {
                            FlowingFluids.config.defaultSeaLevelOverride = Integer.MIN_VALUE;
                            return messageAndSaveConfig(cont, "The default sea level override has been removed.");
                        })
                );
    }


    public static LiteralArgumentBuilder<CommandSourceStack> ignoreList(CommandBuildContext commandBuildContext) {
        return Commands.literal("ignored_fluids")
                .executes(cont -> message(cont, "Control which fluids do or do not get affected by this mod."))
                .then(Commands.literal("list")
                        .executes(cont -> message(cont, "The following fluids are currently ignored by Flowing Fluids: " + FlowingFluids.config.fluidBlacklist))
                )
                .then(Commands.literal("list_all_fluid_names")
                        .executes(cont -> message(cont, "This is a list of all registered fluids as Flowing Fluids knows them: " +
                                BuiltInRegistries.FLUID.stream().map(fluid -> BuiltInRegistries.FLUID.getKey(fluid).toString()).collect(Collectors.toCollection(HashSet::new))))
                )
                .then(Commands.literal("add")
                        .then(Commands.argument("fluid", BlockStateArgument.block(commandBuildContext))
                                .executes(cont -> {
                                    var fluidState = BlockStateArgument.getBlock(cont, "fluid").getState().getFluidState();
                                    if (fluidState.isEmpty() || !(fluidState.getType() instanceof FlowingFluid flows)) {
                                        throw notFluidException.create();
                                    }
                                    String source = BuiltInRegistries.FLUID.getKey(flows.getSource()).toString();
                                    FlowingFluids.config.fluidBlacklist.add(source);
                                    String flowing = BuiltInRegistries.FLUID.getKey(flows.getFlowing()).toString();
                                    FlowingFluids.config.fluidBlacklist.add(flowing);
                                    return messageAndSaveConfig(cont, "Added the fluids " + source + " and " + flowing + " to the ignored fluids list. The list is now: " + FlowingFluids.config.fluidBlacklist);
                                })
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("fluid", BlockStateArgument.block(commandBuildContext))
                                .executes(cont -> {
                                    var fluidState = BlockStateArgument.getBlock(cont, "fluid").getState().getFluidState();
                                    if (fluidState.isEmpty() || !(fluidState.getType() instanceof FlowingFluid flows)) {
                                        throw notFluidException.create();
                                    }
                                    String source = BuiltInRegistries.FLUID.getKey(flows.getSource()).toString();
                                    FlowingFluids.config.fluidBlacklist.remove(source);
                                    String flowing = BuiltInRegistries.FLUID.getKey(flows.getFlowing()).toString();
                                    FlowingFluids.config.fluidBlacklist.remove(flowing);
                                    return messageAndSaveConfig(cont, "Removed the fluids " + source + " and " + flowing + " from the ignored fluids list. The list is now: " + FlowingFluids.config.fluidBlacklist);
                                })
                        )
                );
    }

    public static LiteralArgumentBuilder<CommandSourceStack> debug(CommandBuildContext commandBuildContext) {
        return Commands.literal("~debug").executes(cont -> message(cont, "Debug commands you probably don't need these."))
                .then(booleanCommand("random_ticks_printing",
                        "Enables or disables printing of random tick events, this will spam your log with every random tick event that happens.",
                        "Random ticks printing is now enabled.",
                        "Random ticks printing is now disabled.",
                        a -> FlowingFluids.config.printRandomTicks = a, () -> FlowingFluids.config.printRandomTicks)

                ).then(booleanCommand("water_level_tinting",
                        "Enables or disables water level tinting, this will make water change colour based on its level.",
                        "water_level_tinting is now enabled.",
                        "water_level_tinting is now disabled.",
                        a -> FlowingFluids.config.debugWaterLevelColours = a, () -> FlowingFluids.config.debugWaterLevelColours)
                ).then(Commands.literal("kill_all_current_fluid_updates")
                        .executes(cont -> {
                            FlowingFluids.debug_killFluidUpdatesUntilTime = System.currentTimeMillis() + 3000;
                            return message(cont, "All fluid flowing ticks will be ignored and allowed to freeze in place over the next 3 seconds.\nAll fluids that are loaded and ticking during this time will completely stop updating and freeze in place until the next time they get updated.\n You may use the debug command \"plug_fluids_in_nearby_chunks\" to surround all these frozen fluids with appropriate blocks to prevent further flow.");
                        })
                ).then(Commands.literal("how_many_fluids_plugged_in_world_gen_this_session")
                        .executes(cont ->
                                message(cont, FlowingFluids.waterPluggedThisSession + " fluids have been plugged during world gen this session."))
                ).then(Commands.literal("super_sponge_at_me")
                        .executes(cont -> {
                            int drained = superSponge(cont.getSource().getLevel(), BlockPos.containing(cont.getSource().getPosition()), Fluids.WATER);
                            return message(cont, drained + " blocks of water have been drained.");
                        })
                        .then(Commands.argument("fluid", BlockStateArgument.block(commandBuildContext))
                                .executes(cont -> {
                                            var fluidState = BlockStateArgument.getBlock(cont, "fluid").getState().getFluidState();
                                            if (fluidState.isEmpty() || !(fluidState.getType() instanceof FlowingFluid flows)) {
                                                throw notFluidException.create();
                                            }
                                            int drained = superSponge(cont.getSource().getLevel(), BlockPos.containing(cont.getSource().getPosition()), flows);
                                            return message(cont, drained + " blocks of " + flows.getSource().defaultFluidState().createLegacyBlock().getBlock().getName().getString() +" have been drained.");
                                        }
                                )
                        )
                ).then(booleanCommand("announce_world_gen_actions",
                        "Enables or disables world gen action announcements, this will spam your log with every world gen action that happens because of this mod, including the location of this action (E.G. the plug fluids during world gen feature).",
                        "World gen action announcements are now enabled.",
                        "World gen action announcements are now disabled.",
                        a -> FlowingFluids.config.announceWorldGenActions = a, () -> FlowingFluids.config.announceWorldGenActions)
                ).then(Commands.literal("surround_all_fluids_in_nearby_chunks_with_blocks")
                        .executes(cont ->{
                            var level = cont.getSource().getLevel();
                            var pos = cont.getSource().getPosition();
                            var posChunk = new ChunkPos(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));

                            var dist = level.getServer().getPlayerList().getSimulationDistance();

                            int count = FlowingFluids.waterPluggedThisSession;
                            for (int x = posChunk.x-dist; x <= posChunk.x+dist; x++) {
                                for (int z = posChunk.z-dist; z <= posChunk.z+dist; z++) {
                                    if (level.hasChunk(x, z)) {
                                        PlugWaterFeature.processChunk(level, new ChunkPos(x, z), level.getChunk(x, z));
                                    }
                                }
                            }
                            return message(cont, "All fluids, within "+dist+" chunks of you, have had any fluids that are exposed to air plugged up with appropriate blocks.\n" +
                                    "This will not affect any fluids that are not exposed to air, or are already plugged.\n" +
                                    "This has plugged " + (FlowingFluids.waterPluggedThisSession - count) + " fluids in total.");
                        })
                ).then(Commands.literal("force_tick_all_fluids_in_nearby_chunks")
                        .executes(cont ->{
                            var level = cont.getSource().getLevel();
                            var pos = cont.getSource().getPosition();
                            var posChunk = new ChunkPos(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));

                            var dist = level.getServer().getPlayerList().getSimulationDistance();
                            var rand = level.getRandom();
                            final AtomicInteger count = new AtomicInteger();
                            for (int x = posChunk.x-dist; x <= posChunk.x+dist; x++) {
                                for (int z = posChunk.z-dist; z <= posChunk.z+dist; z++) {
                                    if (level.hasChunk(x, z)) {
                                        level.getChunk(x, z).findBlocks(BlockBehaviour.BlockStateBase::liquid,
                                                (blockPos, blockState) -> {
                                                    level.scheduleTick(blockPos, blockState.getFluidState().getType(), 1 + rand.nextInt(200));
                                                    count.incrementAndGet();
                                                });
                                    }
                                }
                            }
                            return message(cont, "All fluids, within "+dist+" chunks of you, have been forcibly added to the tick queue with random intervals over the next 0-10 seconds, EXPECT SOME LAG! Amount force ticked = " + count.get());
                        })
                ).then(Commands.literal("is_infinite_water_biome")
                        .executes(cont ->{
                            var level = cont.getSource().getLevel();
                            var pos = cont.getSource().getPosition();
                            return message(cont, "You are "+
                                    (FFFluidUtils.matchInfiniteBiomes(level.getBiome(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z)))
                                            ? "IN" : "NOT IN") + " an Infinite biome. By default these are: Oceans, Rivers, and Swamps.\n" +
                                    "Mods can add their own via the api but most modded oceans and rivers should be accounted for automatically by this mod.");
                        })
                );
    }

    public static LiteralArgumentBuilder<CommandSourceStack> drainAndFill() {
        return Commands.literal("draining_and_filling")
                .executes(commandContext -> message(commandContext, "Set the chances of certain random tick interactions with fluids."))
                .then(floatChanceCommand("water_puddle_evaporation_chance",
                        "Sets the chance of small minimum level water tiles evaporating during random ticks",
                        a -> FlowingFluids.config.evaporationChanceV2 = a,
                        () -> FlowingFluids.config.evaporationChanceV2)
                ).then(floatChanceCommand("water_nether_evaporation_chance",
                        "Sets the chance of any water losing a level during random ticks in the nether",
                        a -> FlowingFluids.config.evaporationNetherChance = a,
                        () -> FlowingFluids.config.evaporationNetherChance)
                ).then(floatChanceCommand("water_rain_refill_chance",
                        "Sets the chance of non-full water tiles increasing their level while its rains and they are open to the sky, during random ticks. This provides access to renewable water given enough time.\nNOTE: this will always be forcibly limited to 1/3rd of the current water_puddle_evaporation_chance setting otherwise water will endlessly fill the world during rain, this does effectively cap this value to 0.33",
                        a -> FlowingFluids.config.rainRefillChance = a,
                        () -> FlowingFluids.config.rainRefillChance)
                ).then(floatChanceCommand("water_infinite_biome_refill_chance",
                        "Sets the chance of non-full water tiles increasing their level within: Oceans, Rivers, and Swamps, during random ticks. Additionally they must have a sky light level higher than 0, and be between y=0 and sea level. This provides time limited access to infinite water within these biomes, granted they are big enough and not drained too quickly",
                        a -> FlowingFluids.config.oceanRiverSwampRefillChance = a,
                        () -> FlowingFluids.config.oceanRiverSwampRefillChance)
                ).then(floatChanceCommand("water_infinite_biome_non_consume_chance",
                        "Sets the chance of water not being consumed when flowing in: Oceans, Rivers, and Swamps. Additionally they must have a sky light level higher than 0, and be between y=0 and sea level. This allows access to infinite water within these biomes",
                        a -> FlowingFluids.config.infiniteWaterBiomeNonConsumeChance = a,
                        () -> FlowingFluids.config.infiniteWaterBiomeNonConsumeChance)
                ).then(floatChanceCommand("water_infinite_biome_surface_drain_chance",
                        "Sets the chance of water being drained into water at sea level when flowing into: Oceans, Rivers, and Swamps. Additionally they must have a sky light level higher than 0. This allows infinte water drainage within these biomes",
                        a -> FlowingFluids.config.infiniteWaterBiomeDrainSurfaceChance = a,
                        () -> FlowingFluids.config.infiniteWaterBiomeDrainSurfaceChance)
                ).then(floatChanceCommand("farm_land_drains_water_chance",
                        "Sets the chance at which a farmland block will consume 1 level of water each time it hydrates. 0 == OFF, 1 == ALWAYS",
                        a -> FlowingFluids.config.farmlandDrainWaterChance = a,
                        () -> FlowingFluids.config.farmlandDrainWaterChance)
                ).then(floatChanceCommand("animal_breeding_drains_water_chance",
                        "Sets the chance at which an animal will consume 1 level of nearby water each time it tries to breed, range 8 blocks, water can be at same level or 1 lower. 0 == OFF, 1 == ALWAYS",
                        a -> FlowingFluids.config.drinkWaterToBreedAnimalChance = a,
                        () -> FlowingFluids.config.drinkWaterToBreedAnimalChance)
                ).then(floatChanceCommand("concrete_powder_drains_water_chance",
                        "Sets the chance at which concrete powder will consume a water level on hardening. 0 == OFF, 1 == ALWAYS",
                        a -> FlowingFluids.config.concreteDrainsWaterChance = a,
                        () -> FlowingFluids.config.concreteDrainsWaterChance)
                ).then(booleanCommand("rain_fills_block_above",
                        "Controls if rain will place new layers of water higher than the previous block of water was.",
                        a -> FlowingFluids.config.rainFillsWaterHigherV2 = a, () -> FlowingFluids.config.rainFillsWaterHigherV2)
                ).then(booleanCommand("only_infinite_biomes_at_sea_level",
                        "Controls if the infinite biome refilling only happens to water at exactly sea level.",
                        a -> FlowingFluids.config.fastBiomeRefillAtSeaLevelOnly = a, () -> FlowingFluids.config.fastBiomeRefillAtSeaLevelOnly)
                );
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("create_mod_compat")
                .executes(commandContext -> message(commandContext, "Settings for Create Mod compatibility, use these to change how fluids interact with Create water wheels and pipes."))
                .then(Commands.literal("info")
                        .executes(c -> message(c, "The Create mod uses water wheels as it's most primitive power source. Flowing Fluids has settings to change how these water wheels get powered due to the additional challenges of the flowing fluids mod interactions with fluids."))
                ).then(intCommand("water_wheel_max_ticks_between_flows",
                                "Controls how many ticks a water wheel will keep spinning after the last fluid to flow past it, this helps with intermittent or slow flows.\nThe default value is 80, min is 20, and the maximum value is 9999.",
                                "ticks", 20, 9999,
                                a -> FlowingFluids.config.create_waterWheelFlowMaxTickInterval = a, () -> FlowingFluids.config.create_waterWheelFlowMaxTickInterval)
                ).then(Commands.literal("water_wheel_requirements")
                        .executes(cont -> message(cont, "Changes how the Create Mod's water wheels interact with fluids, select an mode to get further information. Default is flow. Water wheel mode is currently set to " + FlowingFluids.config.create_waterWheelMode))
                        .then(Commands.literal("flow")
                                .executes(cont -> {
                                    FlowingFluids.config.create_waterWheelMode = FFConfig.CreateWaterWheelMode.REQUIRE_FLOW;
                                    return messageAndSaveConfig(cont, "Water wheel mode is now set to require flow.\nWater wheels will only spin if the water has a level gradient, which almost always requires the water to be actively flowing.");
                                })
                        ).then(Commands.literal("flow_or_river")
                                .executes(cont -> {
                                    FlowingFluids.config.create_waterWheelMode = FFConfig.CreateWaterWheelMode.REQUIRE_FLOW_OR_RIVER;
                                    return messageAndSaveConfig(cont, "Water wheel mode is now set to require flow or river.\nWater wheels will only spin if the water has a level gradient, which almost always requires the water to be actively flowing, or if the water is in a river biome touching any water, and within 5 blocks of sea level. Will always spin in the same direction when using a river as a source.");
                                })
                        ).then(Commands.literal("flow_or_river_opposite_spin")
                                .executes(cont -> {
                                    FlowingFluids.config.create_waterWheelMode = FFConfig.CreateWaterWheelMode.REQUIRE_FLOW_OR_RIVER_OPPOSITE;
                                    return messageAndSaveConfig(cont, "Water wheel mode is now set to require flow or river with opposite spin.\nWater wheels will only spin if the water has a level gradient, which almost always requires the water to be actively flowing, or if the water is in a river biome touching any water, and within 5 blocks of sea level. Will spin in the opposite direction to the other river mode.");
                                })
                        ).then(Commands.literal("fluid")
                                .executes(cont -> {
                                    FlowingFluids.config.create_waterWheelMode = FFConfig.CreateWaterWheelMode.REQUIRE_FLUID;
                                    return messageAndSaveConfig(cont, "Water wheel mode is now set to only require fluid to be present in the checked spaces. Will always spin in the same direction.");
                                })
                        ).then(Commands.literal("fluid_opposite_spin")
                                .executes(cont -> {
                                    FlowingFluids.config.create_waterWheelMode = FFConfig.CreateWaterWheelMode.REQUIRE_FLUID_OPPOSITE;
                                    return messageAndSaveConfig(cont, "Water wheel mode is now set to only require fluid to be present in the checked spaces. Will spin in the opposite direction to the other fluid mode.");
                                })
                        ).then(Commands.literal("full_fluid")
                                .executes(cont -> {
                                    FlowingFluids.config.create_waterWheelMode = FFConfig.CreateWaterWheelMode.REQUIRE_FULL_FLUID;
                                    return messageAndSaveConfig(cont, "Water wheel mode is now set to only require a full 8 levels of fluid to be present in the checked spaces. Will always spin in the same direction.");
                                })
                        ).then(Commands.literal("full_fluid_opposite_spin")
                                .executes(cont -> {
                                    FlowingFluids.config.create_waterWheelMode = FFConfig.CreateWaterWheelMode.REQUIRE_FULL_FLUID_OPPOSITE;
                                    return messageAndSaveConfig(cont, "Water wheel mode is now set to only require a full 8 levels of fluid to be present in the checked spaces. Will spin in the opposite direction to the other full fluid mode.");
                                })
                        ).then(Commands.literal("always")
                                .executes(cont -> {
                                    FlowingFluids.config.create_waterWheelMode = FFConfig.CreateWaterWheelMode.ALWAYS;
                                    return messageAndSaveConfig(cont, "water wheel mode is now set to always spin.\nWater wheels will always spin with max strength regardless of present fluids.");
                                })
                        ).then(Commands.literal("always_opposite_spin")
                                .executes(cont -> {
                                    FlowingFluids.config.create_waterWheelMode = FFConfig.CreateWaterWheelMode.ALWAYS_OPPOSITE;
                                    return messageAndSaveConfig(cont, "water wheel mode is now set to always spin with opposite spin.\nWater wheels will always spin with max strength regardless of present fluids, and will spin in the opposite direction to the other always mode.");
                                })
                        ).then(Commands.literal("river_only")
                                .executes(cont -> {
                                    FlowingFluids.config.create_waterWheelMode = FFConfig.CreateWaterWheelMode.RIVER_ONLY;
                                    return messageAndSaveConfig(cont, "water wheel mode is now set to spin in rivers only.\nWater wheels will always spin with max strength when in surface river biome water.");
                                })
                        ).then(Commands.literal("river_only_opposite_spin")
                                .executes(cont -> {
                                    FlowingFluids.config.create_waterWheelMode = FFConfig.CreateWaterWheelMode.RIVER_ONLY_OPPOSITE;
                                    return messageAndSaveConfig(cont, "water wheel mode is now set to spin in rivers only.\nWater wheels will always spin with max strength when in surface river biome water.\nWith an opposite spin direction to the other setting");
                                })
                        )
                ).then(Commands.literal("pipes")
                        .then(booleanCommand("infinite_pipe_fluid_source",
                                "Enables or disables infinite pipe fluid source, if disabled pipes will consume the source fluid block.",
                                "Pipes will now not consume the source fluid block.",
                                "Pipes will now consume the source fluid block.",
                                a -> FlowingFluids.config.create_infinitePipes = a, () -> FlowingFluids.config.create_infinitePipes)
                        ).then(Commands.literal("info")
                                .executes(c -> message(c, "Create mod pipes will draw fluids only when the entire input block is full (8 levels of fluid). This is required for fluid levels to remain consistent between bucket and other usages, and for Flowing Fluids to be as unobtrusive as possible to the Create mod's inner workings. That being said if you want an easy time of using pipes without worrying about water usage, then enable the infinite pipes setting. You can also disable Create pipes from outputting water blocks in it's own config settings"))
                        )
                );
    }

    private static SimpleCommandExceptionType notFluidException = new SimpleCommandExceptionType(new LiteralMessage("The block you provided is not a fluid block, or is not a fluid block that can flow."));


    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, @SuppressWarnings("unused") Commands.CommandSelection var3) {

        var commands = Commands.literal("flowing_fluids")
                .requires(source ->
                        //#if MC>= 12111
                        //$$ source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_OWNER)
                        //#else
                        source.hasPermission(4)
                        //#endif
                            || (source.getServer() != null && source.getServer().isSingleplayer() && source.getPlayer() != null && source.getServer().isSingleplayerOwner(
                        //#if MC>= 12109
                        source.getPlayer().nameAndId()
                        //#else
                        //$$ source.getPlayer().getGameProfile()
                        //#endif
                        ))
                ).then(Commands.literal("help")
                        .executes(c -> message(c, "Use any of the commands without adding any of it's arguments, E.G '/flowing_fluids settings', to get a description of what the command does and it's current value."))
                ).then(presets()
                ).then(Commands.literal("hide_start_message")
                        .executes(c -> {
                            FlowingFluids.config.hideStartMessage = true;
                            return messageAndSaveConfig(c, "The Flowing Fluids start message will no longer be shown to the first player joining.");
                        } )
                ).then(Commands.literal("settings")
                        .executes(commandContext -> message(commandContext, "Settings for Flowing Fluids, use these to change how fluids behave."))
                        .then(booleanCommand("plug_fluids_during_world_gen",
                                        "Enables or disables plugging all fluids that are generated with air beside or below them.\nThis is an IMMENSE reduction in lag during world generation.",
                                        "World gen fluid plugging is now enabled.",
                                        "World gen fluid plugging is now disabled.",
                                        a -> FlowingFluids.config.encloseAllFluidOnWorldGen = a, () -> FlowingFluids.config.encloseAllFluidOnWorldGen)
                        ).then(ignoreList(commandBuildContext)
                        ).then(Commands.literal("reset_all_to_defaults")
                                .executes(cont -> {
                                    FlowingFluids.config = new FFConfig();
                                    return messageAndSaveConfig(cont, "All Flowing Fluids settings have been reset to defaults.");
                                })
                        ).then(Commands.literal("appearance")
                                .executes(commandContext -> message(commandContext, "Appearance settings for Flowing Fluids, use these to change how fluids appear."))
                                .then(Commands.literal("flowing_texture")
                                        .executes(cont -> message(cont, "The flowing fluid texture is currently " + (FlowingFluids.config.hideFlowingTexture ? "hidden." : "shown.") + "\n This will make the fluids surface appear more still and less flickery while settling, this might conflict with mods affecting fluid rendering"))
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
                                )
                        ).then(booleanCommand("enable_mod",
                                "Enables or disables the mod, if disabled the mod will not affect any fluids.",
                                "FlowingFluids is now enabled, liquids will now have physics.",
                                "FlowingFluids is now disabled, vanilla liquid behaviour will be restored, Buckets will retain their partial fill amount until used.",
                                a -> FlowingFluids.config.enableMod = a,
                                () -> FlowingFluids.config.enableMod)

                        ).then(Commands.literal("sounds")
                                .then(enumCommand("displacement_sounds",
                                                "Controls whether or not fluid displacements, via pistons or otherwise, make sounds.",
                                                a -> FlowingFluids.config.displacementSounds = a, () -> FlowingFluids.config.displacementSounds,
                                                Pair.of(FFConfig.DisplacementSounds.NONE, "Displaced fluids will now be silent."),
                                                Pair.of(FFConfig.DisplacementSounds.BLOCKS_ONLY, "Displaced fluids will now only make sounds when displaced by placed blocks."),
                                                Pair.of(FFConfig.DisplacementSounds.PISTON_ONLY, "Displaced fluids will now only make sounds when displaced by pistons."),
                                                Pair.of(FFConfig.DisplacementSounds.BOTH, "Displaced fluids will now make sounds when displaced by both pistons and placed blocks.")
                                        )
                                ).then(floatChanceCommand("flow_sound_chance",
                                        "Controls how often flowing fluids make sounds when they flow. Default is 0.05 (5% chance per fluid flow). Set to 0 to disable flowing sounds.",
                                        (a) -> FlowingFluids.config.flowSoundChance = a,() -> FlowingFluids.config.flowSoundChance)
                                )
                        ).then(Commands.literal("behaviour")
                                .executes(commandContext -> message(commandContext, "Behaviour settings for Flowing Fluids, use these to change how fluids behave."))
                                .then(intCommand("min_level_for_ice",
                                        "Controls the minimum level of water that will freeze, this is useful for making ice form in partial height water.\nThe default value is 4, and the maximum value is 8.",
                                        "level", 0, 8,
                                        a -> FlowingFluids.config.minWaterLevelForIce = a, () -> FlowingFluids.config.minWaterLevelForIce)
                                ).then(intCommand("fluid_processing_distance",
                                        "Allows you to set a block distance for fluid processing, works kinda like render distance but for fluid flowing.\n0 means infinite distance (works with chunk loaders far from players).\nThe default value is 0, and the maximum value is 256 (though it is limited by the servers processing chunk distance).\nPlease note this only affects the flowing calculation and refilling behaviours like rain.",
                                        "distance_in_blocks", 0, 256,
                                        a -> FlowingFluids.config.playerBlockDistanceForFlowing = a, () -> FlowingFluids.config.playerBlockDistanceForFlowing)
                                ).then(intCommand("min_level_for_obsidian",
                                        "Controls the minimum level of lava that will convert to obsidian, this is useful for making obsidian form more consistently.\nThe default value is 6, and the maximum value is 8.",
                                        "level", 0, 8,
                                        a -> FlowingFluids.config.minLavaLevelForObsidian = a, () -> FlowingFluids.config.minLavaLevelForObsidian)
                                ).then(seaLevelOverrides()
                                ).then(Commands.literal("random_tick_level_check_distance")
                                        .executes(cont -> message(cont, "Sets the distance fluids will check for other fluids to level with during random ticks, 0 means disabled, currently set to " + FlowingFluids.config.randomTickLevelingDistance))
                                        .then(Commands.argument("distance", IntegerArgumentType.integer(0, 64))
                                                .executes(cont -> {
                                                    FlowingFluids.config.randomTickLevelingDistance = cont.getArgument("distance", Integer.class);
                                                    return messageAndSaveConfig(cont, "Random tick level check distance set to " + FlowingFluids.config.randomTickLevelingDistance);
                                                })
                                        )
                                ).then(Commands.literal("how_liquids_affect_entities")
                                        .then(booleanCommand("flow_pushes_boats",
                                                "Controls if boats are pushed by the flow angle that water visually has at the surface.\nTHIS MUST BE OFF FOR BOATS TO WORK PROPERLY IN PARTIAL HEIGHT FLUIDS!!!",
                                                "Boats will now be affected by water flow, THIS WILL BREAK BOATS!! they will not function correctly in partial height water",
                                                "Boats will no longer be affected by water flow. This will fix boats not working in partial height water.",
                                                (a) -> FlowingFluids.config.waterFlowAffectsBoats = a,() -> FlowingFluids.config.waterFlowAffectsBoats))
                                        .then(booleanCommand("flow_pushes_players",
                                                "Controls if players are pushed by the flow angle that water visually has at the surface.",
                                                (a) -> FlowingFluids.config.waterFlowAffectsPlayers = a,() -> FlowingFluids.config.waterFlowAffectsPlayers))
                                        .then(booleanCommand("flow_pushes_entities",
                                                "Controls if entities are pushed by the flow angle that water visually has at the surface.\nEXCEPT players, boats, and item entities, they have their own settings.",
                                                (a) -> FlowingFluids.config.waterFlowAffectsEntities = a,() -> FlowingFluids.config.waterFlowAffectsEntities))
                                        .then(booleanCommand("flow_pushes_items",
                                                "Controls if item entities are pushed by the flow angle that water visually has at the surface.",
                                                (a) -> FlowingFluids.config.waterFlowAffectsItems = a,() -> FlowingFluids.config.waterFlowAffectsItems))
                                ).then(enumCommand("fluid_height",
                                        "Changes the heights fluids render/affect entities at, currently set to " + FlowingFluids.config.fullLiquidHeight + ".",
                                        a -> FlowingFluids.config.fullLiquidHeight = a, () -> FlowingFluids.config.fullLiquidHeight,
                                        Pair.of(FFConfig.LiquidHeight.REGULAR, "Fluids now render/affect entities up to regular height."),
                                        Pair.of(FFConfig.LiquidHeight.REGULAR_LOWER_BOUND, "Fluids now render/affect entities up to their regular height but will be almost flat at their lowest amount."),
                                        Pair.of(FFConfig.LiquidHeight.BLOCK_LOWER_BOUND, "Fluids now render/affect entities up to block height but will be almost flat at their lowest amount."),
                                        Pair.of(FFConfig.LiquidHeight.BLOCK, "Fluids now render/affect entities up to block height."),
                                        Pair.of(FFConfig.LiquidHeight.SLAB, "Fluids now render/affect entities up to half a block height."),
                                        Pair.of(FFConfig.LiquidHeight.CARPET, "All Fluids now render/affect entities with 1 pixel height."))
                                )
                                .then(Commands.literal("flow_distances")
                                        .executes(cont -> message(cont, "Modifies the distance fluids will search for slopes to flow down.\nThe vanilla value is always 4 for water but lava will vary between 2 and 4 depending on if it is in the Nether.\n§4WARNING: this setting is the biggest source of lag for all fluid flowing, this value is limited to 8 (as any higher will freeze your world) and I strongly suggest you never raise it above the default 4."))
                                        .then(Commands.literal("water")
                                                .executes(cont -> message(cont, "Modifies the distance water will search for slopes to flow down.\nThe vanilla value is always 4 for water.\nWater flow distance modifier is currently set to " + FlowingFluids.config.waterFlowDistance))
                                                .then(Commands.argument("distance", IntegerArgumentType.integer(0, 8))
                                                        .executes(cont -> {
                                                            FlowingFluids.config.waterFlowDistance = cont.getArgument("distance", Integer.class);
                                                            return messageAndSaveConfig(cont, "Water flow distance set to " + FlowingFluids.config.waterFlowDistance);
                                                        })
                                                )
                                        ).then(Commands.literal("lava")
                                                .executes(cont -> message(cont, "Modifies the distance lava will search for slopes to flow down in the overworld.\nThe vanilla value is always 2 for lava in the overworld.\nLava flow distance modifier is currently set to " + FlowingFluids.config.lavaFlowDistance))
                                                .then(Commands.argument("distance", IntegerArgumentType.integer(0, 8))
                                                        .executes(cont -> {
                                                            FlowingFluids.config.lavaFlowDistance = cont.getArgument("distance", Integer.class);
                                                            return messageAndSaveConfig(cont, "Water flow distance set to " + FlowingFluids.config.lavaFlowDistance);
                                                        })
                                                )
                                        ).then(Commands.literal("lava_nether")
                                                .executes(cont -> message(cont, "Modifies the distance lava will search for slopes to flow down in the nether.\nThe vanilla value is always 4 for lava in the nether.\nLava flow distance modifier is currently set to " + FlowingFluids.config.lavaNetherFlowDistance))
                                                .then(Commands.argument("distance", IntegerArgumentType.integer(0, 8))
                                                        .executes(cont -> {
                                                            FlowingFluids.config.lavaNetherFlowDistance = cont.getArgument("distance", Integer.class);
                                                            return messageAndSaveConfig(cont, "Water flow distance set to " + FlowingFluids.config.lavaNetherFlowDistance);
                                                        })
                                                )
                                        )
                                ).then(Commands.literal("tick_delays__aka__flow_speeds")
                                        .executes(cont -> message(cont, "Modifies the tick delay fluids will have between spreading updates\nThe vanilla value is always 5 for water but lava will vary between 10 and 30 depending on if it is in the Nether."))
                                        .then(Commands.literal("water")
                                                .executes(cont -> message(cont, "Modifies the tick delay water will have between spreading updates.\nThe vanilla value is always 5 for water.\nWater tick delay modifier is currently set to " + FlowingFluids.config.waterTickDelay))
                                                .then(Commands.argument("delay", IntegerArgumentType.integer(1, 255))
                                                        .executes(cont -> {
                                                            FlowingFluids.config.waterTickDelay = cont.getArgument("delay", Integer.class);
                                                            return messageAndSaveConfig(cont, "Water tick delay set to " + FlowingFluids.config.waterTickDelay);
                                                        })
                                                )
                                        ).then(Commands.literal("lava")
                                                .executes(cont -> message(cont, "Modifies the tick delay lava will have between spreading updates in the overworld.\nThe vanilla value is always 30 for lava in the overworld.\nLava tick delay modifier is currently set to " + FlowingFluids.config.lavaTickDelay))
                                                .then(Commands.argument("delay", IntegerArgumentType.integer(1, 255))
                                                        .executes(cont -> {
                                                            FlowingFluids.config.lavaTickDelay = cont.getArgument("delay", Integer.class);
                                                            return messageAndSaveConfig(cont, "Lava tick delay set to " + FlowingFluids.config.lavaTickDelay);
                                                        })
                                                )
                                        ).then(Commands.literal("lava_nether")
                                                .executes(cont -> message(cont, "Modifies the tick delay lava will have between spreading updates in the nether.\nThe vanilla value is always 10 for lava in the nether.\nLava tick delay modifier is currently set to " + FlowingFluids.config.lavaNetherTickDelay))
                                                .then(Commands.argument("delay", IntegerArgumentType.integer(1, 255))
                                                        .executes(cont -> {
                                                            FlowingFluids.config.lavaNetherTickDelay = cont.getArgument("delay", Integer.class);
                                                            return messageAndSaveConfig(cont, "Lava_nether tick delay set to " + FlowingFluids.config.lavaNetherTickDelay);
                                                        })
                                                )
                                        )
                                ).then(Commands.literal("pistons_and_displacement")
                                        .then(booleanCommand("pistons_push_fluids",
                                                "Enables or disables piston pushing, if disabled pistons will no longer push fluids but will cause fluid displacements like any placed block would.\nIf enabled pistons can push fluids the full regular piston distance and will be blocked by things like obsidian at the end.",
                                                "Piston pushing is now enabled.\nLiquids will now be pushed by pistons. You can modify the distance liquids are pushed by changing the displacement_distance_multiplier, but this does affect other things and may increase lag.",
                                                "Piston pushing is now disabled.\nLiquids will no longer be pushed by pistons.",
                                                a -> FlowingFluids.config.enablePistonPushing = a, () -> FlowingFluids.config.enablePistonPushing)
                                        ).then(booleanCommand("easy_piston_pumps",
                                                "Makes fluids above pistons delay their falling to make pumping upwards much easier.",
                                                a -> FlowingFluids.config.easyPistonPump = a, () -> FlowingFluids.config.easyPistonPump)
                                        ).then(booleanCommand("displace_fluids",
                                                "Enables or disables placed blocks displacing fluids, if disabled placed blocks will no longer displace fluids.",
                                                "Placed blocks displacing fluids is now enabled.\nLiquids will now be displaced by blocks placed inside them.",
                                                "Placed blocks displacing fluids is now disabled.\nLiquids will no longer be displaced by blocks placed inside them.",
                                                a -> FlowingFluids.config.enableDisplacement = a, () -> FlowingFluids.config.enableDisplacement)
                                        ).then(floatCommand("displacement_distance_multiplier",
                                                        "Sets a multiplier for how far fluids will try to be displaced when a block is placed inside them, or a piston pushes them, higher values mean further displacement.\nThe default value is 1.0, min is 0.1, and a maximum of 3.0.",
                                                        "multiplier", 0.1f, 3.0f,
                                                        a -> FlowingFluids.config.displacementDepthMultiplier = a, () -> FlowingFluids.config.displacementDepthMultiplier
                                                )
                                        )
                                ).then(Commands.literal("waterlogged_blocks_flow_mode")
                                        .executes(cont -> message(cont, "Controls how water flows into or out fo water loggable blocks, due to limitations you cannot have two side by side waterloggable blocks flow into each other as they would flicker endlessly, Sea grass and kelp are excluded from this setting and will always break in waters absence, current setting: " + FlowingFluids.config.waterLogFlowMode))
                                        .then(Commands.literal("only_in")
                                                .executes(cont -> {
                                                    FlowingFluids.config.waterLogFlowMode = FFConfig.WaterLogFlowMode.ONLY_IN;
                                                    return messageAndSaveConfig(cont, "Water will only flow into water loggable blocks, and never out of them.");
                                                })
                                        ).then(Commands.literal("only_out")
                                                .executes(cont -> {
                                                    FlowingFluids.config.waterLogFlowMode = FFConfig.WaterLogFlowMode.ONLY_OUT;
                                                    return messageAndSaveConfig(cont, "Water will only flow out of water loggable blocks, and never into them.");
                                                })
                                        ).then(Commands.literal("in_from_sides_or_top_out_down")
                                                .executes(cont -> {
                                                    FlowingFluids.config.waterLogFlowMode = FFConfig.WaterLogFlowMode.OUT_DOWN_ELSE_IN;
                                                    return messageAndSaveConfig(cont, "Water will flow into water loggable blocks from the sides or top, and out of them from the bottom, if possible.");
                                                })
                                        ).then(Commands.literal("ignore")
                                                .executes(cont -> {
                                                    FlowingFluids.config.waterLogFlowMode = FFConfig.WaterLogFlowMode.IGNORE;
                                                    return messageAndSaveConfig(cont, "Water flowing will ignore water loggable blocks entirely.");
                                                })
                                        )
                                ).then(booleanCommand("flow_over_edges",
                                        "Controls if liquids flow over nearby edges, or will stay at the ledge.",
                                        "Liquids at their minimum height will now flow to and over nearby edges, up to 4 blocks away.",
                                        "Liquids at their minimum height will no longer flow to and over nearby edges.",
                                        a -> FlowingFluids.config.flowToEdges = a, () -> FlowingFluids.config.flowToEdges)
                                )
                        ).then(drainAndFill()
                        )
                ).then(debug(commandBuildContext)
                );

        if (FlowingFluids.isThisModLoaded("create")) {
            commands.then(create());
        }

        dispatcher.register(commands);
    }

    private static int superSponge(Level level, BlockPos pos, Fluid fluid) {

        final var yes =
                //#if MC>=12104
                BlockPos.TraversalNodeStatus.ACCEPT;
                //#else
                //$$ true;
                //#endif
        final var no =
                //#if MC>=12104
                BlockPos.TraversalNodeStatus.SKIP;
                //#else
                //$$ false;
                //#endif

        return BlockPos.breadthFirstTraversal(pos, 32, 10000, (blockPos, consumer) -> {
            for (Direction direction : Direction.values()) {
                consumer.accept(blockPos.relative(direction));
            }
        }, (blockPos2) -> {
            if (blockPos2.equals(pos)) {
                return yes;
            } else {
                BlockState blockState = level.getBlockState(blockPos2);
                FluidState fluidState = level.getFluidState(blockPos2);
                if (!fluidState.getType().isSame(fluid)) {
                    return no;
                } else {
                    Block block = blockState.getBlock();
                    if (block instanceof final BucketPickup bucketPickup) {
                        if (!bucketPickup.pickupBlock(
                                //#if MC >= 12100
                                null,
                                //#endif
                                level, blockPos2, blockState).isEmpty()) {
                            return yes;
                        }
                    }

                    if (blockState.getBlock() instanceof LiquidBlock) {
                        level.setBlock(blockPos2, Blocks.AIR.defaultBlockState(), 3);
                    } else {
                        if (!blockState.is(Blocks.KELP) && !blockState.is(Blocks.KELP_PLANT) && !blockState.is(Blocks.SEAGRASS) && !blockState.is(Blocks.TALL_SEAGRASS)) {
                            return no;
                        }

                        BlockEntity blockEntity = blockState.hasBlockEntity() ? level.getBlockEntity(blockPos2) : null;
                        Block.dropResources(blockState, level, blockPos2, blockEntity);
                        level.setBlock(blockPos2, Blocks.AIR.defaultBlockState(), 3);
                    }

                    return yes;
                }
            }
        });
    }
}
