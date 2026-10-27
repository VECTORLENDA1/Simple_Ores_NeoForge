# Simple Ores – developer guide

A map of the code, for anyone who wants to change the mod or make it work with other mods (for example in a modpack).
Every package also has a `package-info.java` that says what is inside it.

All the Java code is in `src/main/java/com/exoticmoss/simpleores/`.

## Where things are

| Package | What is there |
|---|---|
| `SimpleOres.java` | The mod class: registers everything, the network messages (`registerPayloads`) and, on the client, the screens and renderers. |
| `block/` | `ModBlocks` (every block and its properties), the bedrock obscuridium ore. `block/entity/ModBlockEntities` registers the block entities. |
| `item/` | `ModItems` (every item), `ModCreativeModTabs` (the creative tab), `ModToolMaterials`, and special items in `item/custom/`. |
| `worldgen/` | Ore generation: configured features, placed features, biome modifiers, the bedrock ore feature. |
| `recipe/` | `ModRecipes`: the recipe types (the Gravity Core recipe). |
| `gravitycraft/` | The Gravity Cores: blocks that capture thrown items and craft them. `CoreTier` has the 4 tiers. |
| `starstorage/` | The star storage (the Magnetar network). See below. |
| `compat/` | Optional support for other mods: JEI (here), JER (`jer/`), WTHIT (`wthit/`). Only loaded when those mods are installed. |
| `datagen/` | Data generators (block tags, world gen). Their output is in `src/generated/resources/`. |
| `mixin/` | The only mixin (makes the Gravity Cores clickable on their whole sphere). |

### The star storage (`starstorage/`)

| Package | What is there |
|---|---|
| `starstorage/network/` | The network: `StarNetwork` (what is connected, items and liquids in and out), `StarAccess` (moves things between the network and any block), `NetworkHandler` (the network seen by other mods), `StarFluids`, `StarChunks` (chunk loading). |
| `starstorage/magnetar/` | The Magnetar Core and its upgrades (speed, range, Wormhole Links). |
| `starstorage/cell/` | Star Cells and Fluid Cells. **`CellTier` has the capacity and colors of every tier.** |
| `starstorage/drive/` | The Stellar Drive (holds the cells). |
| `starstorage/terminal/` | The Stellar Terminal I and II: menu, messages, layout of the screen. |
| `starstorage/quasar/` | The Quasar: saved recipes (`Pattern`), its menu and messages. |
| `starstorage/autocraft/` | `Autocraft`: the planner and the jobs (crafting table, furnaces, Gravity Cores). |
| `starstorage/binary/` | The Binary Star: rules for the slots and tanks of the blocks around it (`SlotRule`). |
| `starstorage/render/` | How the stars look (`StarLook`, `StarDrawing`) and their renderers. |
| `starstorage/client/` | Everything on the player's side: the screens, tooltips, the "Show area" boxes. |
| `starstorage/` | `ModMenus` (menu registry) and `ModDataComponents` (data kept in items: cell contents, links). |

## I want to…

| I want to… | Go to |
|---|---|
| Change a recipe | `src/main/resources/data/simpleores/recipe/` (Gravity Core recipes are in `recipe/gravity_core/`). |
| Change the texts / translations | `src/main/resources/assets/simpleores/lang/` (`en_us.json`, `pt_pt.json`). |
| Change textures or models | `src/main/resources/assets/simpleores/` (`textures/`, `models/`, `items/`, `blockstates/`). |
| Change what tool breaks a block | `datagen/ModBlockTagProvider` (and run the data generator), output in `src/generated/resources/data/minecraft/tags/block/`. |
| Change what a block drops | `src/main/resources/data/simpleores/loot_table/blocks/`. |
| Change how much a cell keeps | `starstorage/cell/CellTier`. |
| Change the Magnetar upgrades (range, speed, links) | `starstorage/magnetar/MagnetarBlockEntity` (constants at the top). The auto-craft speed per upgrade is in `Autocraft.Job` (`INTERVAL`, `SETS`). |
| Change the range of the Binary Star | `starstorage/binary/BinaryStarBlockEntity` (`RADIUS`, `RANGE_PER_UPGRADE`). |
| Change the Gravity Core tiers | `gravitycraft/CoreTier`. |
| Change the ore generation | `worldgen/` (and `datagen/ModWorldGenProvider`). |
| Add a block or an item | `block/ModBlocks`, `item/ModItems`, then `item/ModCreativeModTabs`, the lang files, the model / texture / loot table. |
| Add a network message | Create the payload, then register it in `SimpleOres.registerPayloads`. |
| Add a screen | The menu in its feature package, register it in `starstorage/ModMenus`, the screen in `client/`, and register it in `SimpleOres` (`registerScreens`). |

## Working with other mods

- **Items and liquids:** the star storage uses the NeoForge transfer API (`ResourceHandler`, capabilities `Capabilities.Item.BLOCK` and `Capabilities.Fluid.BLOCK`).
  - The **Binary Star** works with any block of any mod that exposes those capabilities (chests, machines, tanks).
  - The Binary Star also exposes the whole network to other mods, so their pipes, hoppers and machines can put items and liquids in it and take them out.
  - Most compatibility needs **no code**: put a Binary Star next to the other mod's machine, or connect its pipes to the Binary Star.
- **Liquids** of any mod are kept in the Fluid Cells. They need a bucket only to be taken out by hand in the terminal.
- **JEI, JER, WTHIT:** their plugins are in `compat/`. To support another mod of this kind, add a new folder there, and declare the mod as an optional dependency in `build.gradle` (`compileOnly`) and in `src/main/templates/META-INF/neoforge.mods.toml`.
- **Gravity Core recipes:** other mods (or datapacks) can add them with JSON files of type `simpleores:gravity_craft` (see any file in `data/simpleores/recipe/gravity_core/`).

## Conventions

- Comments explain *why*, in simple English, with `///` before classes and methods.
- Numbers that change with a tier or level are in tables (arrays / enums) at the top of the class, so they are easy to find and balance.
- Build with `./gradlew build`. Run the data generators with `./gradlew runData` after changing `datagen/`.
