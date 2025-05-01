package traben.flowing_fluids.api;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfig;

@SuppressWarnings("unused")
public interface FlowingFluidsAPI {

    /**
     * The version of the API, checking this is up to you.
     */
    int VERSION = 1;

    /**
     * Just in case it wasn't obvious.
     */
    int FLUID_LEVELS_PER_BLOCK = 8;

    /**
     * This method is used to get the instance of the API for your mod to use.
     * @param modIdRequestingAPI The mod ID of the mod requesting the API.
     * @return The instance of the API.
     */
    static FlowingFluidsAPI getInstance(@NotNull String modIdRequestingAPI) {
        FlowingFluids.info("API requested by " + modIdRequestingAPI + ", version=" + VERSION);
        return new FlowingFluidsApiImpl(modIdRequestingAPI);
    }


    boolean isModEnabled();

    /**
     * Checks if the mod is currently moving fluids.
     * @return true if the mod is currently moving fluids around by its own logic.
     */
    boolean isModCurrentlyMovingFluids();

    /**
     * Checks if the mod does apply physics to this fluid
     * @param fluid Fluid|FluidState to check
     * @return whether the mod applies physics to this fluid
     */
    boolean doesModifyThisFluid(@NotNull Fluid fluid);

    /**
     * Checks if the mod does apply physics to this fluid
     * @param fluid Fluid|FluidState to check
     * @return whether the mod applies physics to this fluid
     */
    boolean doesModifyThisFluid(@NotNull FluidState fluid);

    /**
     * Disables the mod's physics for this fluid, this will stop the mod from applying any physics to this fluid.
     * @param fluid Fluid|FluidState to disable
     * @param server The server instance this is required if you are in game, this should only ever be null if you are not in game
     */
    void disableThisFluid(@NotNull Fluid fluid, @Nullable MinecraftServer server);

    /**
     * Disables the mod's physics for this fluid, this will stop the mod from applying any physics to this fluid.
     * @param fluid Fluid|FluidState to disable
     * @param server The server instance this is required if you are in game, this should only ever be null if you are not in game
     */
    void disableThisFluid(@NotNull FluidState fluid, @Nullable MinecraftServer server);

    /**
     * Enables the mod's physics for this fluid, this will allow the mod to apply physics to this fluid.
     * @param fluid Fluid|FluidState to enable
     * @param server The server instance this is required if you are in game, this should only ever be null if you are not in game
     */
    void enableThisFluid(@NotNull Fluid fluid, @Nullable MinecraftServer server);

    /**
     * Enables the mod's physics for this fluid, this will allow the mod to apply physics to this fluid.
     * @param fluid Fluid|FluidState to enable
     * @param server The server instance this is required if you are in game, this should only ever be null if you are not in game
     */
    void enableThisFluid(@NotNull FluidState fluid, @Nullable MinecraftServer server);


    /**
     * @return the mod's config
     */
    FFConfig getConfig();

    /**
     * Sets the mod's config with any changes
     * @param config The new config
     * @param server The server instance this is required if you are in game, this should only ever be null if you are not in game
     */
    void setConfig(@NotNull FFConfig config, @Nullable MinecraftServer server);

    /**
     * Removes a certain amount of fluid starting from a position, this will remove the fluid from that block and then spread out and repeat until it has run out
     * of that fluid or has reached the maxAmount.
     * @param level the level, MUST BE SERVER
     * @param startPos the position to start removing from
     * @param fluid the type of fluid to remove
     * @param minAmount the minimum amount of fluid to remove
     * @param maxAmount the maximum amount of fluid to remove
     * @return the total amount of water levels found and removed, this will be between minAmount and maxAmount, or 0 if not enough was found
     */
    int removeFluidAmountFromPos(@NotNull LevelAccessor level, @NotNull BlockPos startPos, @NotNull Fluid fluid, int minAmount, int maxAmount);

    /**
     * Places a certain amount of fluid starting from a position, this will place the fluid in that block and then spread out and repeat until
     * it has run out of the amount to place, or has no more space to place.
     * @param level the level to check, MUST BE SERVER
     * @param startPos the position to start placing from
     * @param fluid the type of fluid to place
     * @param amount the amount of fluid to place
     * @param canSpreadUp whether the fluid can spread up, this is used to determine if the fluid can spread placement to blocks above
     * @param canSpreadDown whether the fluid can spread down, this is used to determine if the fluid can spread placement to blocks below
     * @return the remaining amount of fluid that was not placed, this will be equal to the amount passed in it was not able to place any
     */
    int placeFluidAmountFromPos(@NotNull LevelAccessor level, @NotNull BlockPos startPos, @NotNull Fluid fluid, int amount, boolean canSpreadUp, boolean canSpreadDown);

    /**
     * Registers a Block or BlockTag that will not displace fluid, this is used to prevent the mod from displacing fluid when it is placed
     * <p>
     * NOTE: all bucket interactable waterlogging blocks are automatically registered as are sponges and everything in the ICE tag
     * @param tag The tag to register
     */
    void registerBlockTagThatWontDisplaceFluid(@NotNull TagKey<Block> tag);

    /**
     * Registers a Block or BlockTag that will not displace fluid, this is used to prevent the mod from displacing fluid when it is placed
     * <p>
     * NOTE: all bucket interactable waterlogging blocks are automatically registered as are sponges and everything in the ICE tag
     * @param block The block to register
     */
    void registerBlockThatWontDisplaceFluid(@NotNull Block block);


    /**
     * Registers a Biome or BiomeTag that will infinitely refill water over time.
     * <p>
     * NOTE: all biomes in the ocean and river tags are automatically registered, as are both vanilla swamps.
     * @param tag The tag to register
     */
    void registerBiomeTagThatHasInfiniteWaterRefilling(@NotNull TagKey<Biome> tag);

    /**
     * Registers a Biome or BiomeTag that will infinitely refill water over time.
     * <p>
     * NOTE: all biomes in the ocean and river tags are automatically registered, as are both vanilla swamps.
     * @param biome The biome to register
     */
    void registerBiomeThatHasInfiniteWaterRefilling(@NotNull ResourceKey<Biome> biome);

    /**
     * Tests if a biome is an infinite water refill one, this will check the biome against the infinite water biomes tags and biomes
     */
    boolean doesBiomeInfiniteWaterRefill(@NotNull Holder<Biome> biome);

    /**
     * A Simple interaction to directly modify the fluid amount at a position without doing something Flowing Fluids doesn't expect.
     * This will also handle interactions with water loggable blocks in that spot, though it will fail if the amount isn't 0 or 8 (empty or full).
     *
     * @param level the level, MUST BE SERVER
     * @param pos the position of the action
     * @param fluidAmountModifier an action taking in the fluid type and the amount present in that pos and returning a new fluid type and amount to set
     * @return true if the fluid amount was modified, false if it was not
     */
    boolean modifyFluidAmountAtPos(@NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull FluidAmountModifier fluidAmountModifier);

    /**
     * A Simple interaction to directly modify the fluid amount at a position without doing something Flowing Fluids doesn't expect.
     * This will also handle interactions with water loggable blocks in that spot, though it will fail if the amount isn't 0 or 8 (empty or full).
     *
     * @param level the level, MUST BE SERVER
     * @param pos the position of the action
     * @param type the type of fluid to set
     * @param amount the amount of fluid to set
     * @return true if the fluid amount was modified, false if it was not
     */
    boolean modifyFluidAmountAtPos(@NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull Fluid type, int amount);


    interface FluidAmountModifier {
        /**
         * This method is called to modify the fluid amount at a position.
         * @param type The type of fluid at the position
         * @param amount The amount of fluid at the position
         * @return A pair of the new fluid type and amount to set, if there is no difference nothing will be changed
         */
        @NotNull Pair<Fluid, Integer> getNewAmount(@NotNull Fluid type, int amount);
    }

}
