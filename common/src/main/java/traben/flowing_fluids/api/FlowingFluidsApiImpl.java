package traben.flowing_fluids.api;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.FlowingFluidsPlatform;
import traben.flowing_fluids.config.FFConfig;

public class FlowingFluidsApiImpl implements FlowingFluidsAPI {

    private final String modid;

    FlowingFluidsApiImpl(String modIdRequestingAPI) {
        modid = modIdRequestingAPI;
    }

    @Override
    public boolean isModEnabled() {
        return getConfig().enableMod;
    }

    @Override
    public boolean isModCurrentlyMovingFluids() {
        return FlowingFluids.isManeuveringFluids;
    }

    @Override
    public boolean doesModifyThisFluid(final @NotNull Fluid fluid) {
        return getConfig().isFluidAllowed(fluid);
    }

    @Override
    public boolean doesModifyThisFluid(final @NotNull FluidState fluid) {
        return getConfig().isFluidAllowed(fluid);
    }

    @Override
    public void disableThisFluid(@NotNull final Fluid fluid, @Nullable MinecraftServer server) {
        if (fluid == Fluids.EMPTY) return;

        var flowing = fluidCheck(fluid, "disableThisFluid");
        if (flowing == null) return;

        getConfig().fluidBlacklist.add(BuiltInRegistries.FLUID.getKey(flowing.getSource()).toString());
        getConfig().fluidBlacklist.add(BuiltInRegistries.FLUID.getKey(flowing.getFlowing()).toString());
        setConfig(getConfig(), server);
    }

    @Override
    public void disableThisFluid(@NotNull final FluidState fluid, @Nullable MinecraftServer server) {
        disableThisFluid(fluid.getType(), server);
    }

    @Override
    public void enableThisFluid(@NotNull final Fluid fluid, @Nullable MinecraftServer server) {
        if (fluid == Fluids.EMPTY) return;

        var flowing = fluidCheck(fluid, "enableThisFluid");
        if (flowing == null) return;

        getConfig().fluidBlacklist.remove(BuiltInRegistries.FLUID.getKey(flowing.getSource()).toString());
        getConfig().fluidBlacklist.remove(BuiltInRegistries.FLUID.getKey(flowing.getFlowing()).toString());
        setConfig(getConfig(), server);
    }

    @Override
    public void enableThisFluid(@NotNull final FluidState fluid, @Nullable MinecraftServer server) {
        enableThisFluid(fluid.getType(), server);
    }

    @Override
    public FFConfig getConfig() {
        return FlowingFluids.config;
    }

    @Override
    public void setConfig(final @NotNull FFConfig config, @Nullable MinecraftServer server) {
        FlowingFluids.config = config;
        FlowingFluids.saveConfig();
        if (server != null) {
            FlowingFluids.info("Flowing Fluids config was changed by the "+modid+" mod, and will be applied to the server.");
            server.getPlayerList().getPlayers().forEach(FlowingFluidsPlatform::sendConfigToClient);
        } else {
            FlowingFluids.warn("Flowing Fluids config was changed by the "+modid+" mod, but no server was provided. This will only modify the local config for single-player etc.");
        }
    }

    private boolean clientCheck(LevelAccessor level, String str) {
        if (level.isClientSide()) {
            FlowingFluids.warn("Flowing Fluids API: " + str + " was called on a client side level. This does nothing. Mod that called it: "+modid);
            return true;
        }
        return false;
    }

    private @Nullable FlowingFluid fluidCheck(Fluid fluid, String str) {
        if (fluid instanceof FlowingFluid flowing) {
            return flowing;
        }
        FlowingFluids.warn("Flowing Fluids API: " + str + " was called on a non-flowing fluid. This is not allowed. Mod that called it: "+modid);
        return null;
    }

    @Override
    public int removeFluidAmountFromPos(final @NotNull LevelAccessor level, final @NotNull BlockPos startPos, final @NotNull Fluid fluid, final int minAmount, final int maxAmount) {
        if (clientCheck(level, "removeFluidAmountFromPos")) return 0;

        FlowingFluid flowing = fluidCheck(fluid, "removeFluidAmountFromPos");
        if (flowing == null) return 0;

        return FFFluidUtils.collectConnectedFluidAmountAndRemove(level, startPos, minAmount, maxAmount, flowing);
    }

    @Override
    public int placeFluidAmountFromPos(final @NotNull LevelAccessor level, final @NotNull BlockPos pos, final @NotNull Fluid fluid, final int amount, boolean canSpreadUp, boolean canSpreadDown) {
        if (clientCheck(level, "placeFluidAmountFromPos")) return amount;

        FlowingFluid flowing = fluidCheck(fluid, "placeFluidAmountFromPos");
        if (flowing == null) return 0;

        return FFFluidUtils.addAmountToFluidAtPosWithRemainderAndTrySpreadIfFull(level, pos, flowing, amount, canSpreadUp, canSpreadDown);
    }

    @Override
    public void registerBlockTagThatWontDisplaceFluid(final @NotNull Fluid fluid, final @NotNull TagKey<Block> tag) {
        FlowingFluids.nonDisplacerTags.add(Pair.of(fluid, tag));
    }

    @Override
    public void registerBlockThatWontDisplaceFluid(final @NotNull Fluid fluid, final @NotNull Block block) {
        FlowingFluids.nonDisplacers.add(Pair.of(fluid, block));
    }

    @Override
    public void registerBiomeTagThatHasInfiniteWaterRefilling(final @NotNull TagKey<Biome> tag) {
        FlowingFluids.infiniteBiomeTags.add(tag);
    }

    @Override
    public void registerBiomeThatHasInfiniteWaterRefilling(final @NotNull ResourceKey<Biome> biome) {
        FlowingFluids.infiniteBiomes.add(biome);
    }

    @Override
    public boolean doesBiomeInfiniteWaterRefill(@NotNull final Holder<Biome> biome) {
        return FFFluidUtils.matchInfiniteBiomes(biome);
    }

    @Override
    public boolean modifyFluidAmountAtPos(final @NotNull LevelAccessor level, final @NotNull BlockPos pos, final @NotNull FluidAmountModifier fluidAmountModifier) {
        if (clientCheck(level, "modifyFluidAmountAtPos")) return false;

        var fluid = level.getFluidState(pos);
        if (getConfig().isFluidAllowed(fluid)) {
            var type = fluid.getType();
            var amount = fluid.getAmount();
            var data = fluidAmountModifier.getNewAmount(type, amount);
            if (data.first() == type && data.second() == amount) {
                return false; //no change
            }
            return FFFluidUtils.setFluidStateAtPosToNewAmount(level, pos, data.first(), Mth.clamp(data.second(), 0, 8));
        }

        return false;
    }

    @Override
    public boolean modifyFluidAmountAtPos(@NotNull final LevelAccessor level, @NotNull final BlockPos pos, @NotNull final Fluid type, final int amount) {
        if (clientCheck(level, "modifyFluidAmountAtPos")) return false;

        if (getConfig().isFluidAllowed(type)) {
            return FFFluidUtils.setFluidStateAtPosToNewAmount(level, pos, type, Mth.clamp(amount, 0, 8));
        }
        return false;
    }
}
