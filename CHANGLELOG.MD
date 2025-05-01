
0.4
- many settings default values have changed please reset your settings
- added create 0.6 support for both Forge 1.20.1 and NeoForge 1.21.1
- added support for create Hose Pulley blocks
- added the `FlowingFluidsApi` interface to allow other mods to interact with the mod safely, use `FlowingFluidsApi.getInstance(modid)` to get an implementation of the API to use
- changed the farmland drains water setting from on|off to a chance setting `farm_land_drains_water_chance` with values between 0 and 1.0, 0 is off, 1.0 is always
- changed the infinite water biomes feature to be more aggressive, water will now drain back into water in oceans and rivers at sea level much quicker, and will also refill more quickly, as well as the new feature below
- added the `water_wet_biome_non_consume_chance` setting to control the chance that water will not be consumed when flowing from a "wet" biome (oceans, rivers, swamps), this enhances the effect of having these biomes act as infinite sources of water
- added the `animal_breeding_drains_water_chance` setting to control the chance animals may require, and consume, nearby water when breeding. this range is within 8 blocks horizontally and 1 block above or below the animals feet
- added the `plug_fluids_during_world_gen` setting to control if fluids should be plugged during world generation, this is an IMMENSE performance boost as previously these exposed water reservoirs would cause thousands of fluid updates on chunk generation, this won't affect lone fluid blocks unless they are on chunk boundaries
- added the `easy_piston_pumps` setting to delay fluids above a piston from falling back down, to improve upwards piston pumps
- added further debug commands and removed some
- buckets will now try to place all of their water, and will spread it to nearby water if required, only leaving a partially filled bucket if there was no space.
- all level 1 water now evaporates regardless of exposure to the sky, but still only during the day when it isn't raining
- results of water and lava colliding will now consume 1 level of the fluid that triggered the conversion, e.g. a cobblestone generator will consume 1 level of water as well as replacing the lava
- added a simple fluid pressure system, fluids with fluid above them will spread faster and further per tick, consuming the block above them for extra flow, this pressure only propagates downwards and sideways
- added settings to control whether fluid height differences cause vanilla water pushing behaviours, this can be set separately for boats, players, items, and other entities. Only items entities are enabled by default
- boats will now always float when in, or under, water, this requires the above setting to be off for boats (which is default)
- partially filled buckets will now tint their fullness meter the default color of the fluid
- fixed flowing water texture hiding with sodium
- fixed water level tinting with sodium
- removed the `fast_mode` setting as it was just jank and easily broken

0.3.1 
- neoforge and forge crash fix

0.3
- updated to 1.21.3
- fixed ice displacing the water it is meant to replace
- fixed a sodium / embeddium mixin crash
- removed localization strings in commands text, due to inconsistencies, english "on" and "off" must be used.
- fluid height command changed to behaviour category as it also has non-visual effects
- bug: the debug water level color setting will not work with sodium / embeddium in 1.21.3 due to some workflow delays

0.2

- fixed the flowing texture hiding option not working with sodium
- fixed the sea level being considered 1 level higher than it actually is
- allowed Oceans, Rivers, and Swamps to also absorb water that flows into it at sea level at the exact same rate as the refill setting allows these biomes to infinitely refill to sea level.
- optimized the flowing algorithm, removed a lot of excessive roaming stray water levels would do, this means fluid settles MUCH faster causing fewer updates, this also removes the custom settling behaviour command
- changed some default settings, delete your config file if you want to get the new defaults, or run `/flowing_fluids settings reset_all`
- these changes include altering the vanilla water and lava tick delay rates so they will tick noticeably faster now
- added settings and debug commands:
  - `/flowing_fluids debug kill_all_current_fluid_updates` - kills all current fluid update ticks, useful for debugging
  - `/flowing_fluids debug water_level_tinting <on|off>` - colours water based on the water level, useful for debugging
  - `/flowing_fluids debug super_sponge_at_me <optional fluid block>` - drains water from your position like a sponge block, drains up to 4000 blocks of fluid, useful for debugging
  - `/flowing_fluids settings behaviour waterlogged_blocks_flow_mode <only_in|only_out|in_from_sides_or_top_out_down|ignore>` - controls how fluids flow into or out of water loggable blocks
  - `/flowing_fluids settings behaviour tick_delays <water|lava|lava_nether> <0-255>` - controls the delay between fluid flowing ticks, useful for performance tuning and desired flow speeds
  - `/flowing_fluids settings behaviour flow_distances <water|lava|lava_nether> <1-8>` - controls the distance fluids will search to flow down slopes, useful for performance tuning
  - `/flowing_fluids settings behaviour random_tick_level_check_distance <0-64>` - controls the distance fluids will search during random ticks to check if they can level out, useful for performance tuning
