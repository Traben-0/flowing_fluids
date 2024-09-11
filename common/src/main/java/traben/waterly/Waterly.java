package traben.waterly;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Waterly {
    public static final String MOD_ID = "waterly";

    public final static Logger LOG = LoggerFactory.getLogger("Waterly");
    private static final List<Direction> CARDINALS = new ArrayList<>();
    private static final List<Direction> CARDINALS_AND_DOWN = new ArrayList<>();
    public static boolean fastmode = false;
    public static boolean edges = false;
    public static CarrySplitBehaviour levelBehaviour = CarrySplitBehaviour.LAZY_LEVEL;
    public static boolean enable = true;
    public static boolean debugSpread = false;
    public static boolean debugSpreadPrint = false;
    public static BigDecimal totalDebugMilliseconds = BigDecimal.valueOf(0);
    public static long totalDebugTicks = 0;

    public static void init() {
        Waterly.LOG.info("Waterly initialising");

        CARDINALS.add(Direction.NORTH);
        CARDINALS.add(Direction.SOUTH);
        CARDINALS.add(Direction.EAST);
        CARDINALS.add(Direction.WEST);

        CARDINALS_AND_DOWN.add(Direction.DOWN);
        CARDINALS_AND_DOWN.addAll(CARDINALS);
    }

    public static List<Direction> getCardinalsShuffle() {
        Collections.shuffle(CARDINALS);
        return CARDINALS;
    }

    public static List<Direction> getCardinalsAndDownShuffle() {
        Collections.shuffle(CARDINALS_AND_DOWN);
        return CARDINALS_AND_DOWN;
    }

    public static double getAverageDebugMilliseconds() {
        return totalDebugMilliseconds.divide(BigDecimal.valueOf(totalDebugTicks), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private static int sendCommandFeedback(CommandContext<CommandSourceStack> context, String text) {
        String inputCommand = context.getInput();
        context.getSource().sendSystemMessage(Component.literal("\n§7§o/" + inputCommand + "§r\n" + text + "\n§7_____________________________"));
        return 1;
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext var2, Commands.CommandSelection var3) {
        dispatcher.register(Commands.literal("waterly")
                        .requires(source -> source.hasPermission(4)
                                || (source.getServer().isSingleplayer() && source.getPlayer() != null && source.getServer().isSingleplayerOwner(source.getPlayer().getGameProfile()))
                        ).then(Commands.literal("enable")
                                .then(Commands.literal("on")
                                        .executes(cont -> {
                                            enable = true;
                                            return sendCommandFeedback(cont, "Waterly is now enabled, liquids will now have physics using : " + (fastmode ? "Fast mode." : "Quality mode."));
                                        })
                                ).then(Commands.literal("off")
                                        .executes(cont -> {
                                            enable = false;
                                            return sendCommandFeedback(cont, "Waterly is now disabled, vanilla liquid behaviour will be restored, Buckets will retain their partial fill amount until used.");
                                        })
                                )
                        ).then(Commands.literal("fast_mode")
                                .executes(cont->sendCommandFeedback(cont, "Fast mode is currently " + (fastmode ? "enabled." : "disabled.") +"\n Fast mode changes how liquids behave, and can be toggled on or off.\nFast mode will reduce the amount of checks liquids do to spread, changing from looking for edges 4 blocks away, to only 1, and may cause liquids to pool more frequently in places.\nIn a worst case scenario Fast mode improves water spread lag by 40 times, in actual practise this tends to vary around the 2-20 times faster."))
                                .then(Commands.literal("on")
                                        .executes(cont -> {
                                            fastmode = true;
                                            return sendCommandFeedback(cont, "Fast mode is now enabled.\nLiquids will no longer check if they can move further than 1 block away from itself and may pool more frequently in places.\nThis reduces sideways spread positional checking from 4 - 160 times per update, down to only 4 times.");
                                        })
                                ).then(Commands.literal("off")
                                        .executes(cont -> {
                                            fastmode = false;
                                            return sendCommandFeedback(cont, "Fast mode is now disabled.\nLiquids will now check if they can move up to 4 blocks away from itself and will pool less frequently.\nThis increases sideways spread positional checking from 4 times per update, to up to 160 times.");
                                        })
                                )
                        ).then(Commands.literal("levelBehaviour")
                                .executes(cont->sendCommandFeedback(cont, "Controls how liquids split when flowing, if a block with 5 water levels splits its flow to its neighbour it will have 1 remaining, this chooses what to do with it: " + levelBehaviour))
                                .then(Commands.literal("vanilla")
                                        .executes(cont -> {
                                            levelBehaviour = CarrySplitBehaviour.VANILLA_LIKE;
                                            return sendCommandFeedback(cont, "Liquids will consider themselves 'level' when their neighbours are the same level or 1 level lower. This means water CANNOT flow more than 8 blocks, like vanilla :).");
                                        })
                                ).then(Commands.literal("lazy")
                                        .executes(cont -> {
                                            levelBehaviour = CarrySplitBehaviour.LAZY_LEVEL;
                                            return sendCommandFeedback(cont, "Liquids will lazily try to enforce 'true level' however, if a pool of 5 high water has a single block of level 6, instead of wandering endlessly, it will eventually randomly stop looking for a gap to fit in, with a high random chance to stop.");
                                        })
                                ).then(Commands.literal("strong")
                                .executes(cont -> {
                                    levelBehaviour = CarrySplitBehaviour.STRONG_LEVEL;
                                    return sendCommandFeedback(cont, "Liquids will strongly try to enforce 'true level', if a pool of 5 high water has a single block of level 6, the extra level will wander near endlessly, it will eventually randomly stop looking for a gap to fit in, with a low random chance to stop.");
                                })
                                ).then(Commands.literal("force")
                                        .executes(cont -> {
                                            levelBehaviour = CarrySplitBehaviour.FORCE_LEVEL;
                                            return sendCommandFeedback(cont, "Liquids will always try to enforce 'true level' if a pool of 5 high water has a single block of level 6, it will tirelessly wander the entire pool looking for a chance to flow down and level it self.\nThis is the most realistic but not recommended for lag reasons, consider either strong or lazy leveling instead.");
                                        })
                                )
                ).then(Commands.literal("minHeightFluidEdgeBehaviour")
                        .executes(cont->sendCommandFeedback(cont, "Controls if liquids flow to nearby edges when at their minimum height. Currently they: " + (edges ? "flow." : "stay.")))
                        .then(Commands.literal("flow")
                                .executes(cont -> {
                                    edges = true;
                                    return sendCommandFeedback(cont, "Liquids at their minimum height will now flow to and over nearby edges, up to 4 blocks away if fast mode is disabled, or 1 block away if fast mode is enabled.");
                                })
                        ).then(Commands.literal("stay")
                                .executes(cont -> {
                                    edges = false;
                                    return sendCommandFeedback(cont, "Liquids at their minimum height will no longer flow to and over nearby edges.");
                                })
                        )
                ).then(Commands.literal("debug")
                                .then(Commands.literal("spread")
                                        .then(Commands.literal("print")
                                                .executes(cont -> {
                                                    debugSpreadPrint = !debugSpreadPrint;
                                                    return sendCommandFeedback(cont, "debugSpread is " + (debugSpreadPrint ? "printing." : "not printing."));
                                                })
                                        ).then(Commands.literal("toggle")
                                                .executes(cont -> {
                                                    debugSpread = !debugSpread;
                                                    totalDebugMilliseconds = BigDecimal.valueOf(0);
                                                    totalDebugTicks = 0;
                                                    return sendCommandFeedback(cont, "debugSpread is " + (debugSpread ? "enabled." : "disabled."));
                                                })
                                        ).then(Commands.literal("averageTickLength")
                                                .executes(cont -> sendCommandFeedback(cont, "average water spread tick length is : " + getAverageDebugMilliseconds() + "ms"))
                                        )
                                )
                        )
        );
    }

    public enum CarrySplitBehaviour {
        VANILLA_LIKE,
        LAZY_LEVEL,
        STRONG_LEVEL,
        FORCE_LEVEL

    }
}
