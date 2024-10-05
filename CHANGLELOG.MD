0.2

- fixed the sea level being considered 1 level higher than it actually is
- allowed Oceans, Rivers, and Swamps to also absorb water that flows into it at sea level at the exact same rate as the refill setting allows these biomes to infinitely refill to sea level.
- optimized the flowing algorithm, removed a lot of excessive roaming stray water levels would do, this means fluid settles MUCH faster causing fewer updates, this also removes the custom settling behaviour command
- added settings and debug commands:
  - `/flowing_fluids debug kill_all_current_fluid_updates` - kills all current fluid update ticks, useful for debugging
  - `/flowing_fluids debug water_level_tinting <on|off>` - colours water based on the water level, useful for debugging
  - `/flowing_fluids settings behaviour waterlogged_blocks_flow_mode <only_in|only_out|in_from_sides_or_top_out_down|ignore>` - controls how fluids flow into or out of water loggable blocks
  - `/flowing_fluids settings behaviour tick_delays <water|lava|lava_nether> <0-255>` - controls the delay between fluid flowing ticks, useful for performance tuning and desired flow speeds
  - `/flowing_fluids settings behaviour flow_distances <water|lava|lava_nether> <1-8>` - controls the distance fluids will search to flow down slopes, useful for performance tuning
