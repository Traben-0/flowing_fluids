package traben.waterly;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Objects;

public record CheckedDirFromPosKey(BlockPos pos, Direction dir, int distance) {

    public static boolean puttingInCache = false;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CheckedDirFromPosKey that = (CheckedDirFromPosKey) o;
        //custom cache equality check
        boolean distanceCheck = puttingInCache ? distance < that.distance : distance == that.distance;

        return distanceCheck && Objects.equals(pos, that.pos) && dir == that.dir;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, dir);
    }
}
