# Asteroid Belts

Asteroid Belts is a NeoForge 1.21.1 mod that raises the overworld build limit and adds a configurable asteroid belt above the normal terrain.

## Requirements

- Minecraft 1.21.1
- NeoForge for Minecraft 1.21.1
- Asteroid Belts installed in the `mods` directory

The mod registers a server configuration, so it should be installed wherever the world is generated. In single player, place the mod in the client instance. On a dedicated server, place it in the server's `mods` directory and install the same mod on clients that join the world.

## Installing and starting a world

1. Install the matching NeoForge 1.21.1 profile.
2. Copy the Asteroid Belts jar into the instance or server `mods` directory.
3. Start the game or server once.
4. Open or create an overworld.
5. Stop the game or server before editing the configuration.

The server configuration is created after the first launch at:

```text
config/asteroidbelts-server.toml
```

Edit the values in the `[asteroid_belt]` section. Restart the game or server after changing the file. World generation settings affect chunks as they generate, so changing the config does not regenerate chunks that already exist. Create a new world, or travel into unexplored chunks, to see the result of a configuration change.

## How the asteroid belt works

Asteroids generate only in the overworld. The feature is added to all overworld biomes during world generation, subject to the configured exclusion list.

The default overworld height is from Y=-64 through Y=2031, with an exclusive upper build limit of Y=2032. Asteroids are centered around Y=1024. Their default belt range is approximately Y=824 through Y=1224, and asteroid radii can extend beyond those center positions.

Asteroid centers are distributed across X and Z without visible radial rings. Their vertical distribution is densest at the belt center and becomes less likely toward the top and bottom of the belt. Generation is deterministic from the world seed, so an asteroid that crosses a chunk boundary keeps the same position and shape as neighboring chunks generate.

Asteroids are placed into air and snow. Existing non-air blocks are not replaced. The world spawn column is protected by default so the initial spawn position is not inside an asteroid. This protection can be changed with `void_below_asteroids`.

In amplified worlds, asteroids whose material comes from a biome override are four times the configured diameter range. This makes the default range 40 to 400 blocks for those asteroids. Fallback, additional, and ice asteroids use the configured range without this amplified biome-material scaling.

## Configuration reference

All values below are inside the `[asteroid_belt]` section.

| Option | Default | Valid range | Description |
| --- | ---: | --- | --- |
| `asteroids_per_chunk` | `0.02` | `0.0` to `4.0` | Average number of asteroid centers selected per source chunk. Set to `0` to disable generation. |
| `belt_center_y` | `1024` | `-2032` to `2032` | Y coordinate around which asteroid centers are distributed. |
| `belt_half_height` | `200` | `0` to `1024` | Distance above and below `belt_center_y` used by the belt distribution. |
| `belt_concentration` | `2.0` | `1.0` to `8.0` | Controls how strongly centers favor the belt center. `1.0` spreads them more evenly; higher values make the belt more concentrated. |
| `minimum_diameter` | `10` | `2` to `100` | Smallest asteroid diameter in blocks. |
| `maximum_diameter` | `100` | `2` to `100` | Largest asteroid diameter in blocks. If the values are reversed, the mod uses the smaller value as the minimum and the larger value as the maximum. |
| `asteroid_shape` | `"irregular"` | `irregular`, `ellipsoid`, `sphere` | Selects the surface shape. |
| `ice_asteroid_chance` | `0.025` | `0.0` to `1.0` | Chance that an ordinary asteroid is made entirely from one ice material. |
| `cave_asteroid_chance` | `0.20` | `0.0` to `1.0` | Chance that an asteroid contains one or more tunnel-like caves. |
| `void_below_asteroids` | `false` | `true` or `false` | Clears all non-air blocks below Y=300 as chunks load. It also allows asteroids to occupy the world spawn column. |
| `asteroid_materials` | See below | block and weight entries | Weighted fallback materials used when no biome material override applies. |
| `additional_asteroid_types` | Empty | block and chance entries | Additional material types that can replace ordinary asteroids in every overworld biome. |
| `biome_materials` | See below | biome and block entries | Material overrides selected from the biome at the asteroid center. |
| `asteroid_excluded_biomes` | Empty | biome IDs | Biomes where ordinary asteroids should not generate. Additional asteroid types still generate there. |
| `asteroid_resources` | See below | block, chance, and Y ranges | Blocks that can replace asteroid material at individual positions. |
| `ice_materials` | See below | block and weight entries | Weighted materials used by ice asteroids. |

### Asteroid shapes

Set `asteroid_shape` to one of these values:

- `irregular`: the default. Uses skew, noise, uneven radii, surface craters, and optional tunnels.
- `ellipsoid`: a smooth ellipsoid with independently varied X, Y, and Z radii. Tunnels can still be present if `cave_asteroid_chance` is enabled.
- `sphere`: a smooth sphere with the same radius on all three axes. Tunnels can still be present if enabled.

Shape names are case-insensitive. Invalid values fall back to `irregular`.

### Weighted material lists

`asteroid_materials` and `ice_materials` use entries in this form:

```text
block_id=weight
```

Weights are relative values, not percentages. For example:

```toml
asteroid_materials = [
    "minecraft:stone=60",
    "minecraft:andesite=15",
    "minecraft:diorite=15",
    "minecraft:granite=10"
]
```

These values give stone a 60% selection share, andesite and diorite a 15% share each, and granite a 10% share. The weights do not need to add up to 100.

The fallback list is only used when no biome override matches. The default `biome_materials` list includes `*=minecraft:stone`, so that wildcard supplies the fallback unless it is removed or changed.

Block IDs may refer to blocks from other mods, for example `examplemod:moon_stone=10`. The block must exist in the installed registry when the world generates.

### Additional asteroid types

`additional_asteroid_types` uses this format:

```text
block_id=chance
```

The chance is checked for each generated asteroid. Entries are checked from top to bottom; the first entry whose chance succeeds supplies the asteroid material. Each chance must be greater than `0.0` and no greater than `1.0`.

These entries are useful for rare special asteroids:

```toml
additional_asteroid_types = [
    "minecraft:obsidian=0.05",
    "minecraft:amethyst_block=0.01",
    "minecraft:crying_obsidian=0.005"
]
```

In this example, an asteroid has a 5% chance to become obsidian. If it does not, it has a 1% chance to become amethyst. If neither entry succeeds, normal biome and fallback material selection continues. An additional asteroid uses its configured block regardless of biome material settings and can generate in an excluded biome. It still uses the normal size, shape, cave, and resource rules.

The example below leaves one template commented:

```toml
additional_asteroid_types = [
    # "minecraft:obsidian=0.05",
]
```

### Biome materials

`biome_materials` uses entries in this form:

```text
biome_id=block_id
```

The biome is sampled at the asteroid center. An exact biome entry takes priority over the wildcard entry:

```toml
biome_materials = [
    "minecraft:desert=minecraft:stone",
    "minecraft:forest=minecraft:diorite",
    "minecraft:stony_peaks=minecraft:granite",
    "*=minecraft:stone"
]
```

The wildcard `*` is the fallback for every biome without an exact entry, including biomes added by other mods. Remove the wildcard if you want `asteroid_materials` to be used for unlisted biomes.

Biome material selection makes an asteroid biome-specific. In amplified worlds, those asteroids use the four-times diameter scaling described above.

### Excluding biomes

Use one biome ID per entry in `asteroid_excluded_biomes`:

```toml
asteroid_excluded_biomes = [
    "minecraft:deep_dark",
    "minecraft:lush_caves",
    "examplemod:quiet_void"
]
```

This list applies to ordinary asteroids, including asteroids that use a biome material or fallback material. Asteroids selected through `additional_asteroid_types` intentionally ignore this list so they can serve as universal special types.

### Embedded resources

`asteroid_resources` uses this format:

```text
block_id=chance;y_min..y_max,y_min..y_max
```

The chance is checked for each asteroid block position in the listed inclusive Y ranges. Multiple ranges are separated by commas:

```toml
asteroid_resources = [
    "minecraft:coal_ore=0.025;-64..2031",
    "minecraft:iron_ore=0.0125;-64..2031",
    "minecraft:copper_ore=0.01;-64..2031",
    "minecraft:diamond_ore=0.001;800..1200",
    "minecraft:gold_ore=0.01;900..1000,1100..1200"
]
```

The first resource entry that succeeds at a position supplies that block, so entry order can affect the final distribution when ranges overlap. Resource placement is disabled for ice asteroids, which remain entirely one ice material.

Y ranges can extend outside the current world height; only positions inside the world and inside the asteroid can receive blocks. Reverse ranges such as `1200..800` are accepted and treated as `800..1200`.

### Ice asteroids

`ice_asteroid_chance` controls how often an ordinary asteroid becomes an ice asteroid. `ice_materials` then chooses one material using weights:

```toml
ice_asteroid_chance = 0.025
ice_materials = [
    "minecraft:ice=70",
    "minecraft:packed_ice=25",
    "minecraft:blue_ice=5"
]
```

An ice asteroid is filled entirely with its selected ice block. It does not receive resource blocks, but it can still have tunnels when `cave_asteroid_chance` allows them.

## Complete example

This example keeps the belt near the defaults, uses smooth ellipsoids, adds rare obsidian asteroids, and excludes two biomes from ordinary asteroid generation:

```toml
[asteroid_belt]
asteroids_per_chunk = 0.02
belt_center_y = 1024
belt_half_height = 200
belt_concentration = 2.0
minimum_diameter = 10
maximum_diameter = 100
asteroid_shape = "ellipsoid"
ice_asteroid_chance = 0.025
cave_asteroid_chance = 0.20
void_below_asteroids = false

asteroid_materials = [
    "minecraft:stone=60",
    "minecraft:andesite=15",
    "minecraft:diorite=15",
    "minecraft:granite=10"
]

additional_asteroid_types = [
    "minecraft:obsidian=0.05",
    "minecraft:crying_obsidian=0.005"
]

biome_materials = [
    "minecraft:desert=minecraft:stone",
    "minecraft:forest=minecraft:diorite",
    "minecraft:stony_peaks=minecraft:granite",
    "*=minecraft:stone"
]

asteroid_excluded_biomes = [
    "minecraft:deep_dark",
    "minecraft:lush_caves"
]

asteroid_resources = [
    "minecraft:coal_ore=0.025;-64..2031",
    "minecraft:iron_ore=0.0125;-64..2031",
    "minecraft:copper_ore=0.01;-64..2031",
    "minecraft:diamond_ore=0.001;800..1200"
]

ice_materials = [
    "minecraft:ice=70",
    "minecraft:packed_ice=25",
    "minecraft:blue_ice=5"
]
```

## Configuration recipes

### Disable asteroid generation

```toml
asteroids_per_chunk = 0.0
```

### Make the belt denser and more concentrated

```toml
asteroids_per_chunk = 0.10
belt_concentration = 4.0
```

Higher `asteroids_per_chunk` increases the number of candidate asteroid centers. Higher `belt_concentration` keeps more of those centers close to `belt_center_y`.

### Create a broad, evenly distributed belt

```toml
belt_center_y = 1000
belt_half_height = 500
belt_concentration = 1.0
```

### Use smooth round asteroids

```toml
asteroid_shape = "sphere"
cave_asteroid_chance = 0.0
```

### Make the overworld empty below the belt

```toml
void_below_asteroids = true
```

When enabled, the mod clears non-air blocks below Y=300 as overworld chunks load. This is destructive to terrain in those chunks, so enable it before generating the area you want to clear.

## Troubleshooting

### No asteroids appear

Check the following:

- The world is the overworld.
- `asteroids_per_chunk` is greater than `0.0`.
- You are exploring newly generated chunks.
- The asteroid center range is inside the world height.
- The biome is not in `asteroid_excluded_biomes`.
- The config contains valid block IDs and list syntax.

### Asteroids are made from an unexpected material

Material priority is:

1. A selected entry from `additional_asteroid_types`.
2. An exact `biome_materials` match.
3. The `*` biome material fallback.
4. A weighted entry from `asteroid_materials`.
5. Stone as the hard-coded fallback if no valid material remains.

Ice selection applies to ordinary asteroids before ordinary biome material selection. Ice asteroids use `ice_materials` instead of the other material lists.

### Config changes do not affect an existing area

Asteroids are placed during chunk generation. Changing the config does not rerun world generation for chunks that are already saved. Test changes in a new world or in unexplored terrain.

## License

See [LICENSE](LICENSE).
