# Asteroid Belts

NeoForge 1.21.1 mod that raises the overworld build limit to Y=2032 and adds a configurable asteroid belt.

## Asteroid belt

Asteroids generate around Y=1024, extending 256 blocks above and below the belt center. Their density falls toward the belt edges, and each asteroid is deterministic from the world seed so nearby chunks line up. Asteroid diameters range from 10 to 100 blocks by default. In amplified worlds, asteroids using a configured biome material override, such as granite in badlands, are scaled to 4 times the configured range, making the default range 40 to 400 blocks.

Asteroid centers are roughly uniform across X/Z, without radial rings. Vertically, the density is highest at Y=1024 and gradually falls off across 200 blocks above and below it. The vertical concentration and asteroid rate can be changed in the server config.

Asteroids use biome-specific materials when configured, with weighted stone-like fallback materials. Resource blocks can be embedded using a separate chance and Y range list for each block. A small percentage of asteroids are made entirely from one selected ice material.

The server config is created at `config/asteroidbelts-server.toml` after the first launch. Material entries use `block_id=weight`, resource entries use `block_id=chance;y_min..y_max,y_min..y_max`, and biome materials use one `biome_id=block_id` entry per line. A `*=block_id` entry supplies the fallback for modded biomes. Block IDs can refer to blocks from other mods. Y ranges are inclusive, and each resource entry can have different ranges. For example:

```toml
asteroid_materials = ["minecraft:stone=60", "minecraft:granite=40"]
asteroid_resources = ["minecraft:iron_ore=0.02;-64..2031", "minecraft:copper_ore=0.01;600..800,0..200", "minecraft:diamond_ore=0.001;800..1200"]
biome_materials = [
    "minecraft:desert=minecraft:stone",
    "minecraft:forest=minecraft:diorite",
    "minecraft:stony_peaks=minecraft:granite",
    "*=minecraft:stone"
]
```
