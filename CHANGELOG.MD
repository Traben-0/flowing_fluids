
[1.0]
- added an automatic performance handling system that will dynamically reduce fluid tick rates based on current server lag
- added various ready-made settings presets for easily applying different performance levels, and gameplay settings
- added support for 1.21.9 and 1.21.11
- massively improved water displacement upon placing blocks or using pistons, fluids can be pushed through vastly more complex "pipes" much more easily
- fluids no longer always make sounds due to the vanilla sound check seeing partial water
- added various sounds for fluid behaviors toggleable via settings
- added various seaLevelOverride commands to set a custom sea level for fluid behaviors in a given dimension or everywhere
- fixed various sea level related bugs
- allowed water in non-infinite biomes to evaporate at sea level if above water so they don't pool there forever
- added an info message to guide users to the presets command
- added the river_only command options for create water wheel behaviors
- massively improved how create water wheels interact with flowing fluids, they should now work much more reliably and consistently and can no longer be tricked into infinitely spinning without a flow (when flow is configured to be required i.e. default)
- fixed create item drains treating partially filled buckets and bottles as if they were full
- fixed kelp growing into partial water and turning it into full water infinitely
- fixed cauldron interactions with partially filled buckets

[0.6.1]
- fixed a forge crash
- fixed the `concrete_powder_drains_water_chance` setting not updating correctly

[0.6]
- fixed errors that caused rain to flood the world
- added the `fluid_processing_distance` setting that acts like a "render distance" but for fluid flowing, filling, and draining, behaviours, this can have IMMENSE performance improvements if enabled
- tweaked the conditions for rain and infinite refilling biomes to not cross trigger each other
- rain refilling chance is now forcibly limited to 1/3 of the evaporation chance setting to prevent excessive rain filling
- evaporation now happens even during rain and night time
- rain filling upwards is now disabled by default, this can be enabled with the `rain_fills_block_above` setting
- added the `only_infinite_biomes_at_sea_level` setting which means the infinite water biomes setting (oceans, rivers, swamps etc) will only refill existing water at sea level exactly, this should make draining them easier after the first layer, defaults to false
- added `river_only` and `river_only_opposite` modes for create water wheels
- added the `concrete_powder_drains_water_chance` setting to control the chance that concrete powder will consume water when hardened, defaults to 0.5 (50%)
- rain no longer fills water in deserts

[0.5.1]
- fixed dedicated servers not correctly sending their config to clients (only the server needs to update)

[0.5]
- fixed a crash with distant horizons
- fixed Create pipes not consuming fluids in the world
- allowed Create mod pipes to consume fluids in more situations, such as taking water from 2 half filled blocks (still requires a total of 1 full block present)
- added checks to warn users when the wrong Create version is present (on forge this is a controlled crash)
- rain can now fill water in a larger area as long as at least 1 of the blocks has access to the sky
- rain will now fill water twice as fast during a thunderstorm
- added `rain_fills_block_above` setting to allow rain to fill water into empty blocks above, defaults to ON, this means your reservoirs can now overflow
- the evaporation chance setting's default value has been increased from 2% to 5% to help clean up overflows with the above change
- added `min_level_for_obsidian` setting to allow obsidian to be created at lower lava levels, defaults to 6
- made it so blocks that displace fluids are now set per fluid type (e.g. obsidian only doesn't displace lava, ice only doesn't displace water)
- updated API version to 2 due to the above change

[0.4]
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
- added the `min_level_for_ice` setting to control the minimum level of water required for ice to form, this is useful for making ice  more consistently.
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

[0.3.1 ]
- neoforge and forge crash fix

[0.3]
- updated to 1.21.3
- fixed ice displacing the water it is meant to replace
- fixed a sodium / embeddium mixin crash
- removed localization strings in commands text, due to inconsistencies, english "on" and "off" must be used.
- fluid height command changed to behaviour category as it also has non-visual effects
- bug: the debug water level color setting will not work with sodium / embeddium in 1.21.3 due to some workflow delays

[0.2]

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
