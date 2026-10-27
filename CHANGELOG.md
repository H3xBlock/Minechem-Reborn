# Changelog

All notable changes to Minechem Rebord are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
The version is the Minecraft version, then the build number, then the release type (`26.1.2.1-beta.1`).

## [Unreleased]

### Added

- Creative tabs: "Minechem Buckets" (the bucket of every chemical that has one) and "Minechem Elements" (every chemical, the former single tab).
- JEI: the chemical buckets are shown as a crafting grid, as in 5.0.5: eight vials around an empty bucket give the chemical's bucket, and the reverse recipe gives the eight vials. The empty bucket is catalysed on the bucket.
- JEI: the decomposer's fluid recipes (water, lava), shown with the fluid's bucket as the input.
- Decomposer: the fluid gauge is drawn with its frame (the 5.0.5 `fluidtank.png`), the liquid and the glass; hovering it shows the fluid's localised name and the amount in mB.
- Table of elements: each element is a cell of the table. Hovering shows its name, its form, its radioactivity and whether it is not yet discovered; clicking a discovered element opens its page in the chemist's journal. Undiscovered elements are dimmed.
- Microscope: inspecting an item with a journal also discovers what the item decomposes into (for example a fermented spider eye finds polonium and ethyl alcohol).
- Development only, not in the release: the GameTest name filter (`-PgametestOnly=<words>`), which runs only the tests whose name contains one of the words.

### Changed

- The microscope and blueprint projector titles are centred as in 5.0.5, and the "Inventory" label is no longer drawn.
- The projector shows the blueprint's name and the count of each wall (`<count>x <wall>`), as 5.0.5.
- Tungsten plating (`fusion_wall_2`) is named "Tungsten Plating" (en) and "Plaque de tungstène" (fr).
- Development only: `AGENTS.md` (the agent instructions) is no longer in the repository. It now lives outside the mod, in `knowledge/AGENTS.md`. The comments, the JSON documentation fields, the config strings, the test messages and the test names were cleaned of references to the history of the project and to the previous version; no value or behaviour changed.

### Fixed

- Leaded chest: the nuclear logo is on the top of the lid again. 26.1 maps a box's top face to the texture area that 1.7.10 used for the bottom, so the port's copy of `models/leaded_chest.png` has its two top-row panels swapped (the top and the underside). Checked by reading the pixels and the 26.1 model code; not yet seen in a client after the change.

### Deviations

- The fluid gauge's liquid is a flat colour, not the fluid's own sprite.
- A non-empty fluid tank shows at least one row of liquid; 5.0.5 shows nothing below 1/38 of the capacity.
- JEI shows a fluid as its bucket, not as the fluid block.
- Discovery by decomposition: the microscope discovers the products of an inspected item's decomposition, not only the item (5.0.5 adds only the inspected item). Decided by the user.
- The table of elements draws elements 114 to 118 as extra grey cells: the table's texture stops at element 113.
- Delirium at level II: the screen spins as with nausea (vanilla's spin, through a mixin on `GameRenderer`), on top of the dark overlay. The original had only the overlay. Asked by the user.
- Synthesis machine: the arm moves while the machine is active (a recipe whose ingredients are in the storage, with enough power), not only while it waits for power (5.0.5). Each craft also plays a full arm cycle, whatever the craft's time, so hopper-fed crafts show it too. Asked by the user.

## [26.1.2.3-beta.1] - 2026-10-26

### Added

- JEI: the fission and fusion reactor recipes, catalogued under their blueprints (the reactors have no item). Fission lists every element that splits; fusion lists every pair of elements that fuses, each pair once. Each recipe shows its energy (the reactor, the decomposer and the synthesis machine).
- Config section `[worldgen]` with the 5.0.5 settings: `generateOre` (default `true`), `uraniumoredensity` (default `5`, 1 to 64) and `uraniumOreClusterSize` (default `3`, 1 to 10).
- Development only, not in the release: the dev pickaxe (mines a 5 x 5 square and keeps the uranium ore) and the command `/minechem uranium [radius]`, which lists the nearest uranium ore. They exist only in the dev client run.

### Changed

- Uranium ore is mined like the diamond: it needs an iron pickaxe (vanilla `needs_iron_tool` tag), and its hardness and blast resistance are those of the diamond ore. It generates between y -64 and 16, as the diamond, instead of 0 to 49 (5.0.5). Not checked in a client yet.
- Uranium ore generation now follows 5.0.5 and reads the config: per overworld chunk, `uraniumoredensity + 1` veins (6 by default instead of 5), each of a random size from 0 to `uraniumOreClusterSize + 9` (0 to 12 by default instead of always 12), replacing plain stone only (no longer granite, diorite or andesite). `generateOre = false` turns it off. The datapack files `worldgen/configured_feature/uranium_ore.json` and `worldgen/placed_feature/uranium_ore.json` are removed; the biome modifier `minechem:uranium_ore` now adds a feature built in code. Not checked in a client yet.
- The first fusion wall is crafted with tungsten instead of lead (iron, tungsten, iron in each column).

### Fixed

- JEI shows the lead of the hazmat suit, leaded chest and fusion wall as lead, not as a generic chemical item.

### Removed

- The `minechem:energy_source` block and item (a test source, not in 5.0.5). Machines now need an energy mod, or `powerUseEnabled = false`.

## [26.1.2.2-beta.1] - 2026-10-25

### Added

- Config `powerUseEnabled` (default `true`, 5.0.5 `power.enable`): set it to `false` to play without an energy mod. The decomposer,
  the synthesis machine and the fission reactor then work without energy, as 5.0.5 did when no energy API was installed. The
  fusion reactor still needs stored energy to start (5.0.5 behaviour).
- Config `recreationalChemicalEffects` (default `true`, as in 5.0.5): turns off the recreational drug effects and the
  decompositions that give those drugs (brown mushroom, nether wart, duskberry).

### Changed

- Config `debugMode` now defaults to `false`, as in 5.0.5 (an existing config file keeps its value).

### Removed

- Removed the leftovers of the v6 port: the augments (the `minechem:augmented` item, its augment and wrapper recipes and its
  overlay icons), the `minechem:light` and `minechem:redstone` blocks, the player research data (`minechem:research`), the
  118 element advancements and the journal advancement, and the neutron count of the elements (the "Neutrons" tooltip line).
  None of them exists in 5.0.5. Worlds saved with an earlier build lose these items, blocks and data.

### Fixed

- Synthesis machine: the recipe grid holds ghost copies again, as in 5.0.5. Items can no longer be taken out of it (this was a
  duplication), and breaking the machine drops only the storage and the journal, not the grid or the output preview.
- ComputerCraft: the chemical turtle's methods now run on the server thread, so they no longer change the world from the
  computer thread.
- Synthesis machine automation: hoppers and pipes now really reach the machine's slots (they worked on a copy before), and
  they put chemicals into the storage only, never into the recipe grid, where they were deleted.
- Synthesis machine and vanilla hoppers: the 5.0.5 side rules now apply to hoppers too. A hopper below no longer pulls the
  recipe grid's ghosts (a duplication) or the storage; it takes the output, a whole batch at a time, as in 5.0.5.
- Tag-based (OreDictionary) recipes: they are rebuilt on each tag reload instead of piling up, and in single player only the
  server rebuilds them.
- Reactor walls now follow the `allowAutomation` config, as in 5.0.5.
- Chemist journal held in the off hand: choosing an item in its list now sets that journal's active item.
- Blueprint projector: opening its screen should no longer hide the projected ghost blocks on the client (not checked in a
  client).
- The mod's GameTests are no longer registered in a published jar (only in the development runs).
- Radioactive fluids: the saved radiation of a fluid block is removed when the block goes away, so the level data no longer
  grows with stale entries.
- A malformed element or molecule file in `config/minechem/data` (with `useDefaultElements` or `useDefaultMolecules` off) no
  longer stops the game from starting: the error is logged, and recipes that need a missing chemical are skipped.
- `ChemicalAPI.registerFluidChemicalReaction` called too early no longer empties the reaction table for the whole game; it
  returns false.
- Radioactive items and buckets in hand should no longer replay the equip animation on every tick (not checked in a client).
- Radioactive element tooltip: it no longer uses a client-only class, which could crash a dedicated server that builds a
  tooltip.
- Polytool: it no longer loses durability when it hits or mines (5.0.5 behaviour), so it can no longer break and lose its
  infused elements.

## [26.1.2.1-beta.1] - 2026-10-25

First beta: Minechem 5.0.5 ported to NeoForge 26.1.2 (Minecraft 26.1.2). The original mod is by jakimfett, under the DBaJ
non-commercial licence. This port is by h3xblock and is not an official version of Minechem.

### Added

- Elements, molecules, the decomposer, the synthesis machine, the fission and fusion reactors, the blueprint projector, the optical
  microscope, the chemist journal, radiation (decay, leaded chests, hazmat suit), fluids, the polytool, and the `ChemicalAPI` and
  `RecipeAPI` for other mods.
- ComputerCraft: CC:Tweaked 1.120 turtle chemical peripheral (`chemical`), with the `onDecay` event (loaded only with CC:Tweaked).
- Railcraft Reborn: decomposition of the coloured posts, and of the plates, blocks and nuggets of the Railcraft metals (loaded only
  with Railcraft Reborn).
- Ender IO: powders, End Steel ingot, and the alloy smelter recipes (loaded only with Ender IO).
- Applied Energistics 2: singularity and charged quartz crystal recipes (loaded only with Applied Energistics 2).
- OreDictionary decomposition for the vanilla and modded items that have a tag in the game: the metal blocks, brass, the Railcraft
  metals, the Ender IO alloys, the Railcraft gears and the steel tools. The rules are in `decomposerOreRecipes.json`.
- Uranium ore: texture and decomposition.

### Changed

- Gaseous elements and molecules rise, spread a short distance, and do not flow downward. This is the 5.0.5 lava behaviour in reverse.
- The creative tab is named "Minechem Rebord".

### Not ported

- Ender IO dark steel gear, tools and armour.
- Railcraft armour.
- End Steel nugget.

### Known issues

- The ComputerCraft `onDecay` event and the full list of turtle methods are not checked in a client yet.
- The journal, the device rendering and the microscope screen are not fully checked in a client.
- Some reactions are not checked in a client yet.
- Uranium ore generation: the density and cluster settings are not read by the datapack yet.
