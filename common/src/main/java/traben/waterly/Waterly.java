package traben.waterly;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
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

    public static void init() {
        Waterly.LOG.info("Waterly initialising");

        CARDINALS.add(Direction.NORTH);
        CARDINALS.add(Direction.SOUTH);
        CARDINALS.add(Direction.EAST);
        CARDINALS.add(Direction.WEST);

        CARDINALS_AND_DOWN.add(Direction.DOWN);
        CARDINALS_AND_DOWN.addAll(CARDINALS);
    }


    private static final List<Direction> CARDINALS = new ArrayList<>();
    private static final List<Direction> CARDINALS_AND_DOWN = new ArrayList<>();

    public static List<Direction> getCardinalsShuffle() {
        Collections.shuffle(CARDINALS);
        return CARDINALS;
    }

    public static List<Direction> getCardinalsAndDownShuffle() {
        Collections.shuffle(CARDINALS_AND_DOWN);
        return CARDINALS_AND_DOWN;
    }

    public static ItemStack lastBucketUsed = ItemStack.EMPTY;

    public static boolean fastmode = false;
    public static boolean debugSpread = false;
    public static boolean isDebugSpreadRemoveCaches = false;
    public static BigDecimal totalDebugMilliseconds = BigDecimal.valueOf(0);
    public static long totalDebugTicks = 0;
    public static double getAverageDebugMilliseconds(){
        return totalDebugMilliseconds.divide(BigDecimal.valueOf(totalDebugTicks), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private static void sendCommandFeedback(CommandContext<CommandSourceStack> context, String text) {
        String inputCommand = context.getInput();
        context.getSource().sendSystemMessage(Component.literal("\n§7§o/" + inputCommand + "§r\n" + text + "\n§7_____________________________"));
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext var2, Commands.CommandSelection var3) {
        dispatcher.register(Commands.literal("waterly")
                .requires(source -> source.hasPermission(4)
                        || (source.getServer().isSingleplayer() && source.getPlayer() != null && source.getServer().isSingleplayerOwner(source.getPlayer().getGameProfile()))
                )
//                .executes(SolidMobsCommands::help)
                .then(Commands.literal("toggle_fast_mode").executes(cont->{
                    fastmode = !fastmode;
                    sendCommandFeedback(cont, "Fast mode is now " + (fastmode ? "enabled" : "disabled") + ".\nLiquids will no longer check if they can move further than 1 block away from itself and may pool more in places.\nThis reduces sideways positional checking from 4 - 306 times per update, down to only 4 times.");
                    return 1;
                }))
                .then(Commands.literal("debugSpread").executes(cont-> {
                    debugSpread = !debugSpread;
                    totalDebugMilliseconds = BigDecimal.valueOf(0);
                    totalDebugTicks = 0;
                    sendCommandFeedback(cont, "debugSpread is " + (debugSpread ? "enabled." : "disabled."));
                    return 1;
                }))
                .then(Commands.literal("debugSpread_removeCaches").executes(cont-> {
                    isDebugSpreadRemoveCaches = !isDebugSpreadRemoveCaches;
                    totalDebugMilliseconds = BigDecimal.valueOf(0);
                    totalDebugTicks = 0;
                    sendCommandFeedback(cont, "cache use is " + (isDebugSpreadRemoveCaches ? "disabled." : "enabled."));
                    return 1;
                }))
                        .then(Commands.literal("averageDebugTickLength").executes(cont-> {

                            sendCommandFeedback(cont, "average water tick length is : " + getAverageDebugMilliseconds() + "ms");
                            return 1;
                        }))



//                .then(CommandManager.literal("listRecentCollisions")
//                        .executes(SolidMobsCommands::recentCollisionsQueryAll)
//                        .then(CommandManager.literal("all").executes(SolidMobsCommands::recentCollisionsQueryAll))
//                        .then(CommandManager.literal("involvingPlayers").executes(SolidMobsCommands::recentCollisionsQueryPlayers))
//                        .then(CommandManager.literal("resultCollision").executes(SolidMobsCommands::recentCollisionsQueryYes))
//                        .then(CommandManager.literal("resultNoCollision").executes(SolidMobsCommands::recentCollisionsQueryNo))
//                )
//                .then(CommandManager.literal("entityBlacklist")
//                        .executes(SolidMobsCommands::blacklistList)
//                        .then(CommandManager.literal("list").executes(SolidMobsCommands::blacklistList))
//                        .then(CommandManager.literal("add").then(CommandManager.argument("text", StringArgumentType.string()).executes(SolidMobsCommands::blacklistAdd)))
//                        .then(CommandManager.literal("remove").then(CommandManager.argument("text", StringArgumentType.string()).executes(SolidMobsCommands::blacklistRemove)))
//                        .then(CommandManager.literal("clear").executes(SolidMobsCommands::blacklistClear))
//                )
        );
    }
}
