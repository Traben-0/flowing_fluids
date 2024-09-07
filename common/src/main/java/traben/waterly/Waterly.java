package traben.waterly;

import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}
