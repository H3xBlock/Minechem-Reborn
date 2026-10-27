# Minechem Rebord

**Minechem Rebord** is a chemistry mod for **NeoForge 26.1.2** (Minecraft 26.1.2). It is a port of
[Minechem](https://github.com/iopleke/Minechem/tree/master), created by jakimfett.

Items are broken down into compounds and base elements in the decomposer. With elements, some research and the synthesis
machine, you can make materials and compounds. The fission and fusion reactors split or combine elements.

> **Beta version (26.1.2.1-beta.1).** The mod works and is tested, but not every feature has been checked in a client. The open
> points are listed below.

## Features

- **Elements and molecules**: the element table and the molecules, with their colours, formulas and radioactivity.
- **Decomposer**: the decomposition recipes (metadata variants, fluids, the OreDictionary recipes), and the additions for modern
  resources (blocks, alloys, tools).
- **Synthesis**: the synthesis machine and its recipe table, with its energy and its batches.
- **Reactors**: fission (an element splits), fusion (two elements combine), and the blueprint projector.
- **Microscope**: it inspects an item; with a chemist journal in its slot, an item that has a recipe is written into the journal.
- **Chemist journal**: the item and the recipes it has recorded; its active recipe loads into the synthesis machine.
- **Radiation**: element decay, leaded chests, protective suit (hazmat).
- **Fluids**: buckets of molecules and elements, fluids that flow, swim and react, gases that rise.
- **Polytool**: a multi-tool, with alloys and infusion, and its window.
- **API** for other mods: `minechem.api.ChemicalAPI` (fluid reactions) and `minechem.api.RecipeAPI` (decomposition recipes).
- **Compatibility** (loaded only when the mod is present):
  - **CC:Tweaked 1.120**: the chemical turtle (`chemical` peripheral, `onDecay` event).
  - **Railcraft Reborn 1.4.6**: decomposition of the coloured posts, and of the plates, blocks and nuggets of the Railcraft metals.
  - **Ender IO**: powders, End Steel, the alloy smelter recipes.
  - **Applied Energistics 2**: singularity and charged quartz crystal recipes.
  - **JEI**: the decomposer, synthesis, fission and fusion recipes (with the energy of the reactor recipes).

## Data and additions

The recipes and values (elements, molecules, decomposition and synthesis recipes) are stored as data files in
`src/main/resources/assets/minechem/data/`.

Some modern items do not exist in the original Minechem. They have a decomposition, chosen to match a real resource. Each one has
a `note` in `decomposerOreRecipes.json`:
- the metal blocks (nine ingots);
- brass (11 Cu, 5 Zn);
- the Railcraft metals (bronze, invar, steel): nuggets, plates, blocks;
- the Ender IO alloys: ingots, nuggets, blocks;
- the Railcraft gears and tools, calculated from their crafting recipe.

## Requirements

- Minecraft 26.1.2, NeoForge 26.1.2.76 or newer.
- Java 25: the build toolchain of `build.gradle`. Gradle downloads it when it is not installed (foojay resolver in `settings.gradle`).
- Optional: CC:Tweaked 1.120, Railcraft Reborn 1.4.6, Ender IO, Applied Energistics 2 (the matching recipes are added only when the
  mod is present).

## Build and run (development)

```
./gradlew build                          # builds the jar
./gradlew runClient --offline            # starts the test client
./gradlew runGameTestServer --offline    # runs the GameTests
./gradlew test --offline                 # runs the unit tests
```

On Windows, use `gradlew.bat`. Any JDK that Gradle can run is enough; the Java 25 toolchain is downloaded when needed.

## Continuous integration

- `.github/workflows/build.yml` runs on every push and pull request: it builds the jar, runs the unit tests and the GameTests, and
  uploads the jar as an artifact.
- The compile-only dependency CC:Tweaked is not in git. The workflow downloads it from the repository variable
  `CC_TWEAKED_JAR_URL` and checks its SHA-256 from `CC_TWEAKED_JAR_SHA256`. Set both in the GitHub repository settings
  (Settings → Secrets and variables → Actions → Variables). Without them the build stops with a clear message.
- `.github/workflows/publish.yml` publishes to CurseForge and Modrinth. It is disabled (`if: false`) and runs by hand only.

## Open points (beta)

- **Energy**: the machines need Forge Energy (FE), and the mod has no energy source of its own: an energy mod is needed to power
  them. Without one, set `powerUseEnabled = false` in `config/minechem-common.toml`, and the machines run without energy. The
  default (`true`) is still open.
- **ComputerCraft turtle**: reading the chemicals is checked in a client, but not the `onDecay` event nor the full list of methods.
- **Journal, device rendering, microscope screen**: not fully confirmed in a client.
- **Reactions**: some reactions still have to be checked in a client.
- **Uranium ore generation**: reads `generateOre`, `uraniumoredensity` and `uraniumOreClusterSize` from the `[worldgen]` section of
  `config/minechem-common.toml`. Checked by unit tests and GameTests, not yet in a newly generated world in a client.
- **Not ported** (decision): the Ender IO dark steel gear and tools, the Railcraft armour, the End Steel nugget.

To report a bug, attach `run/logs/latest.log` or `run-client-out.log`.

## Licence and credits

- **Licence**: DBaJ "Don't Be a Jerk" non-commercial care-free license (see [`LICENSE.md`](LICENSE.md)). You may use, modify and share
  the mod, without making a profit, keeping the license and crediting the author.
- **Original author**: jakimfett, creator of Minechem ([github.com/iopleke/Minechem](https://github.com/iopleke/Minechem/tree/master)).
- **Port**: this port is maintained by the maintainer of this repository, with the help of AI tools. It is not an official version
  of Minechem.
- **Third-party notices**: see [`NOTICE.md`](NOTICE.md).
