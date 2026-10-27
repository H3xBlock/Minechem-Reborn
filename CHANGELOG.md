# Changelog

All notable changes to Minechem Rebord are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
The version is the Minecraft version, then the build number, then the release type (`26.1.2.1-beta.1`).

## [26.1.2.1-beta.1]

### Added

- Initial version of Minechem Rebord for NeoForge 26.1.2 (Minecraft 26.1.2), a chemistry mod:
  - Elements and molecules, with their colours, formulas and radioactivity, and the table of elements.
  - Chemical decomposer, synthesis machine and their recipe tables, including the OreDictionary recipes.
  - Fission and fusion reactors (multiblock), and the blueprint projector.
  - Microscope and chemist's journal.
  - Radiation: element decay, leaded chest, protective suit (hazmat).
  - Fluids: buckets of molecules and elements, fluids that flow and react, gases that rise.
  - Polytool: a multi-tool with alloys and infusion.
  - API for other mods: `minechem.api.ChemicalAPI` and `minechem.api.RecipeAPI`.
  - Compatibility, loaded only when the mod is present: CC:Tweaked (chemical turtle), Railcraft Reborn, Ender IO, Applied Energistics 2, JEI.
- Creative tabs for the elements and the buckets.

### Deviations

- Decomposition recipes for modern resources that did not exist in the original mod: the metal blocks, brass, the Railcraft
  metals, the Ender IO alloys and the Railcraft gears and tools. Each has a `note` in `decomposerOreRecipes.json`, which gives the
  reason. The choice was made by the maintainer of this port.
- Not ported, by decision: the Ender IO dark steel gear and tools, the Railcraft armour, the End Steel nugget.
- Energy: the machines use Forge Energy. Without an energy mod, `powerUseEnabled = false` in `config/minechem-common.toml`
  lets them run without energy.
