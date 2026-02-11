package traben.flowing_fluids;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.Set;

public interface FFFlowListenerLevel {
    Map<BlockPos, Set<BlockPos>> ff$getFlowListenerPositions();
}
