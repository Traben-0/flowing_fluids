package traben.flowing_fluids;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class PlugWaterFeature {

    private static final Pair<Boolean, Runnable> defTrue = Pair.of(true, null);
    private static final Direction[] dirs = {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST,
            Direction.DOWN
    };

    public static Set<BlockPos> findBlocks(ChunkAccess chunkAccess, int x1, int y1, int z1, int x2, int y2, int z2) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        var set = new HashSet<BlockPos>();
        for (int i = #if MC>MC_21 chunkAccess.getMinSectionY() #else chunkAccess.getMinSection() #endif ;
             i < #if MC>MC_21 chunkAccess.getMaxSectionY() #else chunkAccess.getMaxSection() #endif ;
             ++i) {
            LevelChunkSection levelChunkSection = chunkAccess.getSection(chunkAccess.getSectionIndexFromSectionY(i));
            if (levelChunkSection.maybeHas(PlugWaterFeature::isFluidSource)) {
                BlockPos blockPos = SectionPos.of(chunkAccess.getPos(), i).origin();
                for (int y = y1; y < y2; ++y) {
                    for (int z = z1; z < z2; ++z) {
                        for (int x = x1; x < x2; ++x) {
                            BlockState blockState = levelChunkSection.getBlockState(x, y, z);
                            if (isFluidSource(blockState)) {
                                set.add(mutableBlockPos.setWithOffset(blockPos, x, y, z).immutable());
                            }
                        }
                    }
                }
            }
        }
        return set;

    }

    public static void processChunk(LevelAccessor level, ChunkPos chunk, ChunkAccess chunkAccess) {

        boolean hasXNeg = level.hasChunk(chunk.x - 1, chunk.z);
        boolean hasXPos = level.hasChunk(chunk.x + 1, chunk.z);
        boolean hasZNeg = level.hasChunk(chunk.x, chunk.z - 1);
        boolean hasZPos = level.hasChunk(chunk.x, chunk.z + 1);

        var set = findBlocks(chunkAccess, 0, 0, 0, 16, 16, 16);
        if (hasXNeg) set.addAll(findBlocks(level.getChunk(chunk.x - 1, chunk.z), 15, 0, 0, 16, 16, 16));
        if (hasXPos) set.addAll(findBlocks(level.getChunk(chunk.x + 1, chunk.z), 0, 0, 0, 1, 16, 16));
        if (hasZNeg) set.addAll(findBlocks(level.getChunk(chunk.x, chunk.z - 1), 0, 0, 15, 16, 16, 16));
        if (hasZPos) set.addAll(findBlocks(level.getChunk(chunk.x, chunk.z + 1), 0, 0, 0, 16, 16, 1));


        if (set.isEmpty()) return;

        int minx = chunk.getMinBlockX();
        int minz = chunk.getMinBlockZ();
        int maxx = chunk.getMaxBlockX();
        int maxz = chunk.getMaxBlockZ();


        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        int sea = level.getSeaLevel() - 5;

        var doneSet = new HashSet<BlockPos>();
        Supplier<Pair<Boolean, Runnable>> testDo = () -> {
            if (mutableBlockPos.getX() < minx || mutableBlockPos.getX() > maxx) return defTrue;
            if (mutableBlockPos.getZ() < minz || mutableBlockPos.getZ() > maxz) return defTrue;
            if (set.contains(mutableBlockPos)) return defTrue;
            BlockState blockState = chunkAccess.getBlockState(mutableBlockPos);
            if (blockState.isAir() && blockState.getFluidState().isEmpty()) {
                var immutable = mutableBlockPos.immutable();
                doneSet.add(immutable);
                return Pair.of(false, (Runnable) () -> fillBlock(chunkAccess, immutable, sea));
            }
            return null;
        };

        List<Runnable> runs = new ArrayList<>();
        for (BlockPos blockPos : set) {
            if (doneSet.contains(blockPos)) continue;

            boolean neighbourWater = false;
            runs.clear();
            for (Direction dir : dirs) {
                mutableBlockPos.setWithOffset(blockPos, dir);
                var result = testDo.get();
                if (result == null) continue;
                if (result.first()) {
                    neighbourWater = true; // or is testing from chunk edge, in which case we force the plug even if it's a lone block
                } else {
                    runs.add(result.second());
                }
            }
            if (neighbourWater) {
                for (Runnable run : runs) {
                    run.run();
                }
            }
        }

    }

    private static boolean isFluidSource(BlockState state) {
        var fluid = state.getFluidState();
        if (fluid.isEmpty() || !fluid.isSource()) return false;

        return FlowingFluids.config.isFluidAllowed(fluid);
    }

    private static void fillBlock(final ChunkAccess chunk, BlockPos pos, int seaLevel) {

        BlockState blockState;
        var biome = chunk.getNoiseBiome(pos.getX(), pos.getY(), pos.getZ());
        if (biome.is(BiomeTags.IS_NETHER)) {
            blockState = Blocks.NETHERRACK.defaultBlockState();
        } else if (biome.is(BiomeTags.IS_END)) {
            blockState = Blocks.END_STONE.defaultBlockState();
        } else if (pos.getY() < 0) {
            blockState = Blocks.DEEPSLATE.defaultBlockState();
        } else if (pos.getY() < seaLevel) {
            blockState = Blocks.STONE.defaultBlockState();
        } else {
            if (biome.is(BiomeTags.HAS_VILLAGE_DESERT)
                    || biome.is(BiomeTags.IS_BEACH)
                    || biome.is(BiomeTags.IS_OCEAN)) {
                blockState = Blocks.SAND.defaultBlockState();
            } else {
                blockState = Blocks.MUD.defaultBlockState();
            }
        }
        if (FlowingFluids.config.announceWorldGenActions)
            FlowingFluids.info("placed block during world gen: " + blockState + " at /tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ());

        FlowingFluids.waterPluggedThisSession++;
        chunk.setBlockState(pos, blockState,
                #if MC>=MC_21_5 0 // no updates pls
                #else false #endif
                );
    }
}
