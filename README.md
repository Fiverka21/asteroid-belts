# Asteroid Belts

NeoForge 1.21.1 mod that raises the overworld build limit to Y=1024 and adds a configurable asteroid belt.

## Asteroid belt

Asteroids generate around Y=512, extending 256 blocks above and below the belt center. Their density falls toward the belt edges, and each asteroid is deterministic from the world seed so nearby chunks line up. Asteroid diameters range from 10 to 100 blocks by default.

By default, asteroids are also restricted to repeated circular belts centered on the world origin. The first ring starts at radius 1024 and another ring appears every 1024 blocks. Ring radius, spacing, width, edge concentration, and asteroid rate can be changed in the server config.

Asteroids use biome-specific materials when configured, with weighted stone-like fallback materials. Resource blocks can be embedded using configurable per-block amounts. A small percentage of asteroids are made entirely from one selected ice material.

The server config is created at `config/asteroidbelts-server.toml` after the first launch. Material entries use `block_id=weight`, resource entries use `block_id=amount`, and biome overrides use `biome_id=block_id`. For example:

```toml
asteroid_materials = ["minecraft:stone=60", "minecraft:granite=40"]
asteroid_resources = ["minecraft:iron_ore=0.02", "minecraft:diamond_ore=0.001"]
biome_materials = ["minecraft:desert=minecraft:sandstone"]
```
