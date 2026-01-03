package traben.flowing_fluids.config;

import traben.flowing_fluids.FlowingFluids;

public enum FFConfigPreset {
    DEFAULT(
            "The default configuration for Flowing Fluids.\nThis is equivalent to resetting the config.\nThis can also be considered the highest quality setting with no slowed down settings for performance."
    ) {
        @Override
        public void apply() {
            FlowingFluids.config = new FFConfig();
        }
    },
    PERFORMANCE_QUALITY(
            "The Performance Quality configuration for Flowing Fluids.\nThis will improve performance over the default settings by only lowering fluid ticking speeds while maintaining the exact same fluid behaviour settings.\nThis is equivalent to Default only set to flow a little slower."
    ) {
        @Override
        public void apply() {
            var config = new FFConfig();
            config.waterTickDelay = 4;
            config.lavaNetherTickDelay = 8;
            config.lavaTickDelay = 18;
            config.playerBlockDistanceForFlowing = 0;
            FlowingFluids.config = config;
        }
    },
    PERFORMANCE_MEDIUM(
            "The Performance Medium (reduced quality) configuration for Flowing Fluids.\nThis will improve performance over the default settings by further lowering fluid ticking speeds and slightly reducing fluid behaviour settings.\nThis will result in fluids flowing a bit less far and taking slightly longer to spread.\nFluids will also not flow in non player loaded chunks."
    ) {
        @Override
        public void apply() {
            var config = new FFConfig();
            config.waterTickDelay = 5;
            config.lavaNetherTickDelay = 10;
            config.lavaTickDelay = 20;
            config.playerBlockDistanceForFlowing = 160;
            config.waterFlowDistance = 3;
            config.lavaFlowDistance = 2;
            config.lavaNetherFlowDistance = 3;
            config.randomTickLevelingDistance = 24;
            config.displacementDepthMultiplier = 2/3f;
            FlowingFluids.config = config;
        }
    },
    PERFORMANCE_ULTRA(
            "The Performance Ultra (Low quality) configuration for Flowing Fluids.\nThis will improve performance over the default settings by lowering fluid ticking speeds greatly and greatly reducing fluid behaviour settings.\nThis will result in fluids flowing much less far and taking much longer to spread.\nFluids will also only flow when close to a player.\nNote: this is probably awful to play with, only use if you really can't run the medium or quality performance settings"
    ) {
        @Override
        public void apply() {
            var config = new FFConfig();
            config.waterTickDelay = 6;
            config.lavaNetherTickDelay = 12;
            config.lavaTickDelay = 24;
            config.playerBlockDistanceForFlowing = 80;
            config.waterFlowDistance = 2;
            config.lavaFlowDistance = 1;
            config.lavaNetherFlowDistance = 2;
            config.randomTickLevelingDistance = 16;
            config.displacementDepthMultiplier = 1/3f;
            FlowingFluids.config = config;
        }
    },
    NO_INFINITE_BIOMES(
            "The No Infinite Biomes configuration for Flowing Fluids.\nThis will disable all infinite water settings for biomes such as oceans, rivers and swamps.\nBut this will keep rain refilling settings."
    ) {
        @Override
        public void apply() {
            var config = new FFConfig();
            config.oceanRiverSwampRefillChance = 0f;
            config.infiniteWaterBiomeNonConsumeChance = 0f;
            config.infiniteWaterBiomeDrainSurfaceChance = 0f;
            config.fastBiomeRefillAtSeaLevelOnly = false;
            FlowingFluids.config = config;
        }
    },
    RAIN_CAN_FLOOD(
            "The Rain Can Flood configuration for Flowing Fluids.\nThis will enable rain to fill water blocks higher than just source blocks, allowing rain to be capable of flooding areas."
    ) {
        @Override
        public void apply() {
            var config = new FFConfig();
            config.rainFillsWaterHigherV2 = true;
            FlowingFluids.config = config;
        }
    },
    NO_GAMEPLAY_CONSUMERS(
            "The No Gameplay Consumers configuration for Flowing Fluids.\nThis will disable all gameplay water consumption mechanics such as farmland drying, animals drinking to breed, and concrete drying."
    ) {
        @Override
        public void apply() {
            var config = new FFConfig();
            config.farmlandDrainWaterChance = 0f;
            config.drinkWaterToBreedAnimalChance = 0f;
            config.concreteDrainsWaterChance = 0f;
            FlowingFluids.config = config;
        }
    };

    public final String explanation;

    FFConfigPreset(String explanation) {;
        this.explanation = explanation;
    }

    public abstract void apply();

}
