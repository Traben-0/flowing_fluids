


<span align="center">

![Flowing Fluids Banner](https://i.imgur.com/hBR2oop.png)

[![discord](https://i.imgur.com/vU8pcbf.png)](https://discord.com/invite/rURmwrzUcz)
![-](https://i.imgur.com/yZzzW4Q.pngg)
[![curseforge](https://i.imgur.com/rpoyjVG.png)](https://www.curseforge.com/minecraft/mc-mods/flowing-fluids)
![-](https://i.imgur.com/VgKekoJ.png)
[![modrinth](https://i.imgur.com/7fioiRx.png)](https://modrinth.com/mod/flowing-fluids)
![-](https://i.imgur.com/tiNmDIH.png)
[![github](https://i.imgur.com/EYNx6oL.png)](https://github.com/Traben-0/flowing_fluids)
![-](https://i.imgur.com/yar5K6v.png)
[![kofi](https://i.imgur.com/xdhWKwC.png)](https://ko-fi.com/traben)



---

![example](https://i.imgur.com/cchiXTD.gif)

</span>

---

# About

Flowing fluids is a mod adding more realistic fluid physics to the game, making fluids mostly finite, and adding an 
element of fluid management to the game. With dozens of settings for performance, flowing behaviour, gameplay elements, and visuals

It is a spiritual successor to some of the various other abandoned water physics mods over the years, but built from scratch for modern
Minecraft. 

Implemented in a way that is compatible with *(most)* mod added fluids, and that allows you to 
disable/remove the mod at any time to return to vanilla mechanics with any 'not currently flowing' fluids remaining fully intact for vanilla purposes.

## Server side or Client side?

This mod works best when on both `server` and `client` side. However, it is only **required** on the `server` side, if 
you don't want the visual features and don't mind some minor client-side de-sync when directly interacting with fluids, such as using buckets.

There is no harm in joining a vanilla server with this mod enabled, however you will get the same minor client-side de-sync
as you would when joining a server with the mod without it on the client.

## ⚠️ The mod is currently in early development ⚠️
It is not yet feature complete, and I make no promises of stability at this stage, as there may yet be 
some unknown breaking bugs and further room for performance improvements.

---

# Features

Many of these can be tweaked or even disabled via the `/flowing_fluids settings` command, use `/flowing_fluids help` if you are confused.

## **Flowing Fluids**:

Fluids will flow and spread outwards and down inclines, filling up spaces and creating pools.

This can be tweaked in various ways, such as how far they flow, and how aggressively the fluid tries to level itself in pools.

![bucket](https://i.imgur.com/2hTCIJH.gif)

This means you can create things like aqueducts.

![aquaduct_2](https://i.imgur.com/grBktwe.gif)

or flood areas with water.

![water_2](https://i.imgur.com/NDWqyRH.gif)

or lava.

![lava](https://i.imgur.com/cchiXTD.gif)

or even modded fluids.


## **Finite Fluids**:

Fluids are finite, and will flow until they run out, or are blocked.

![finite_1](https://i.imgur.com/dTs4G4O.gif)

This means you will need to manage your water supply, and can't just place 2 water source blocks and have infinite water.

![aquaduct_1](https://i.imgur.com/S5R0pPM.gif)

Oceans, Rivers, and Swamps however, will act as infinite water sources, but there is a limit to how fast they will refill,
and they can still be drained if fast enough *(controlled in settings)*. 

These biomes will also absorb any non-full water that flows back into it at sea level, also adjustable via settings.

![ocean](https://i.imgur.com/H1GpmCs.gif)

Fluids will also be displaced by blocks wherever possible to avoid losing any fluid.

![fill_and_displace](https://i.imgur.com/kD6eeE7.gif)

## **Piston Pumps**:

Fluids can be moved by pistons to pump them upwards automatically.

![piston_1](https://i.imgur.com/BMVSyJG.gif)

![piston_2](https://i.imgur.com/86PC5cV.gif)

## **Gameplay Elements**:

There are a few new gameplay elements that will drain or use water, to encourage water management, most are configurable.

Such as farmlands having a chance to drain a water level whenever it hydrates.

![farmland](https://i.imgur.com/NoTbe63.gif)

Animals having a chance to require and consume 1 level of nearby water *(within 8 blocks)* when breeding.

Bottles will consume 2-3 water levels when filled.

![finite_2](https://i.imgur.com/DYaWKoX.gif)

Buckets are able to pick up 1 - 8 levels of fluid, and can be used to place down precise amounts.

![buckets](https://i.imgur.com/2fVOcKW.png)

With fluids being displaced by block placement, you will need to find other ways to drain it away, such as using sponges, creating drains, 
or tweaking the evaporation settings mentioned below.

![displacement](https://i.imgur.com/aLXbkdG.gif)

There are also various ways in which water specifically naturally evaporates or refills *(Rates customizable via settings)*

- Small *(level 1)* water puddles will evaporate slowly in the over world.
- Any water will evaporate quickly in the nether.
- Any water blocks that are not full *(levels 1-7)* will refill over time when exposed to rain.
- Any water blocks that are not full *(levels 1-7)* will refill over time when in Ocean, River, or Swamp biomes, below sea level, and exposed to skylight.'

![commands](https://i.imgur.com/8INEzZB.png)

## **Mod and Vanilla backwards compatibility**:

Any mod added fluids should work with the mod, though if the modded fluid has special properties, they may not be fully supported.

You can also blacklist modded fluids that you don't want to have physics. Via the `/flowing_fluids settings ignored_fluids add <fluid_name>` command.

This mod can also be disabled (`/flowing_fluids settings enable_mod on|off`) or removed at any time without breaking the world, as any fluid that is 'full' *(8 levels)* will be saved as a vanilla source block. 
And all 'non-full' fluids *(1-7 levels)* will be saved as a vanilla flowing blocks which will just disappear once updated in vanilla.

## **The Create Mod - (v0.5 Fabric and v0.6 Forge and NeoForge)**

Flowing Fluids has special compatibility settings for use with the Create mod, to modify the behaviour of water wheels and pipes.

Please note some of these interactions only work properly with "full" liquid blocks *(source blocks)*, so factor that in to your design planning.

![create](https://i.imgur.com/fR4fpji.gif)

Create Mod Pipes can move fluids around finitely, as can Hose Pulleys, and can optionally be set to treat all fluids as infinite sources via the `/flowing_fluids create_compat pipes infinite_pipe_fluid_source on|off` command.

![pipes](https://i.imgur.com/hM95cFe.gif)

Create Mod Water Wheels will work normally given a constant flow of fluid.

![wheels](https://i.imgur.com/f7G3L2f.gif)

By default, Water Wheels will also work without flow in river biomes at sea level.

![wheels](https://i.imgur.com/l2TV14C.gif)

![wheels](https://i.imgur.com/E00rDkE.gif)

This behaviour can be modified via the `/flowing_fluids create_compat water_wheel_requirements ...` command, with the following options:
- `flow` - Requires a constant flow of fluid
- `flow_or_river` - Requires a constant flow of fluid, or to be touching water in a river biome near sea level ***(Default)***
- `fluid` - Requires contact with any fluid blocks, regardless of flow
- `full_fluid` - Requires contact with full *(8 levels)* fluid blocks only, regardless of flow
- `always` - All water wheels will always be spinning


For the options that don't require a constant flow of fluid, the wheels will always spin in the same direction, 
the following options exists to swap this arbitrary direction:
- `flow_or_river_opposite_spin`
- `fluid_opposite_spin`
- `full_fluid_opposite_spin`
- `always_opposite_spin`

## **Visuals**:

The mod renders fluids completely normally, using the regular vanilla fluid rendering, this allows it to be compatible with shaders.

![shaders](https://i.imgur.com/BOT5Qvw.png)

The mod allows hiding the flowing texture that normally appears on fluids when there is a height difference. *(enabled by default)*

`/flowing_fluids settings appearance flowing_texture`

![flow](https://i.imgur.com/tzK8HeZ.png)

![still](https://i.imgur.com/tj5ovML.png)

The mod also allows changing the heights at which 'full' fluids render and affect entities.

`/flowing_fluids settings behaviour fluid_height`

![regular](https://i.imgur.com/7vhITCK.png)

![full](https://i.imgur.com/dVohNTb.png)

You can also prevent minimum level fluids from flowing over edges, if you prefer that visual.

`/flowing_fluids settings behaviour flow_over_edges`

![edge](https://i.imgur.com/ItAgKff.png)

## **Performance**:

The mod is designed to be as performant as possible. However, massive amounts of flow affecting hundreds or thousands of blocks,
such as quickly draining an ocean, is going to cause some unavoidable server side lag. As much as any other thing affecting this many blocks would.

![lag](https://i.imgur.com/H1GpmCs.gif)

The mod has various settings to tweak the performance have a look around and see what works for you.

The mod also makes a pass over every chunk after generation and plugs up all exposed fluids so you don't end up with 
hundreds of fluid ticks happening right after the world loads as entire oceans drain into vast cave systems.
This used to be a problem with the early mod but is now quite stable, can be disabled, but I wouldn't recommend it.

# Changelog
- find the changelog [here](CHANGLELOG.MD)


# FAQ
- version for 1.19 or earlier? 
  - *I have no plans to backport this mod earlier than 1.20.1*
- does "mod_name" work with this? 
  - *probably, depends on what they do, Flowing Fluids does have an [API](common/src/main/java/traben/flowing_fluids/api/FlowingFluidsAPI.java) for other mods to interact with it if needed*
- can I use this in my mod-pack? 
  - *go for it*
- it breaks with the Create mod! 
  - *you have to have the specifically supported version of create for things to work, currently these are `v0.5` for `Fabric 1.20.1` and `v0.6` for `Forge 1.20.1` and `NeoForge 1.21.1`*


<span align="center">

![Banner](https://i.imgur.com/j86ZaXm.png)

</span>