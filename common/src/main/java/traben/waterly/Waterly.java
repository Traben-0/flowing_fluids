package traben.waterly;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Waterly {
    public static final String MOD_ID = "waterly";

    public static void init() {
        // Write common init code here.
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

    public static record CheckedDirFromPosKey(BlockPos pos, Direction dir, int distance) {
        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final CheckedDirFromPosKey that = (CheckedDirFromPosKey) o;
            //larger than is important optimization
            return distance >= that.distance && Objects.equals(pos, that.pos) && dir == that.dir;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos, dir);
        }
    }

    public static boolean fastmode = false;


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
