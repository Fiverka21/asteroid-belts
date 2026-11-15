package asteroid.belts;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class AsteroidFeature extends Feature<NoneFeatureConfiguration> {
    private static final int CHUNK_SIZE = 16;

    public AsteroidFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (level.getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }

        Settings settings = Settings.read();
        if (settings.asteroidsPerChunk <= 0.0D || settings.materials.isEmpty()) {
            return false;
        }

        int chunkX = Math.floorDiv(context.origin().getX(), CHUNK_SIZE);
        int chunkZ = Math.floorDiv(context.origin().getZ(), CHUNK_SIZE);
        int chunkMinX = chunkX * CHUNK_SIZE;
        int chunkMinZ = chunkZ * CHUNK_SIZE;
        int chunkMaxX = chunkMinX + CHUNK_SIZE - 1;
        int chunkMaxZ = chunkMinZ + CHUNK_SIZE - 1;
        int maximumRadius = settings.maximumDiameter / 2;
        int sourceMinX = Math.floorDiv(chunkMinX - maximumRadius, CHUNK_SIZE);
        int sourceMaxX = Math.floorDiv(chunkMaxX + maximumRadius, CHUNK_SIZE);
        int sourceMinZ = Math.floorDiv(chunkMinZ - maximumRadius, CHUNK_SIZE);
        int sourceMaxZ = Math.floorDiv(chunkMaxZ + maximumRadius, CHUNK_SIZE);
        boolean placed = false;

        for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX++) {
            for (int sourceZ = sourceMinZ; sourceZ <= sourceMaxZ; sourceZ++) {
                RandomSource random = RandomSource.create(seedFor(level.getSeed(), sourceX, sourceZ));
                int guaranteedAsteroids = (int) Math.floor(settings.asteroidsPerChunk);
                int asteroidSlots = guaranteedAsteroids + (random.nextDouble() < settings.asteroidsPerChunk - guaranteedAsteroids ? 1 : 0);

                for (int slot = 0; slot < asteroidSlots; slot++) {
                    Asteroid asteroid = createAsteroid(level, context.chunkGenerator(), settings, random, sourceX, sourceZ);
                    if (asteroid != null && asteroid.intersects(chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, maximumRadius)) {
                        placed |= placeAsteroid(level, settings, asteroid, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
                    }
                }
            }
        }

        return placed;
    }

    private static Asteroid createAsteroid(WorldGenLevel level, ChunkGenerator chunkGenerator, Settings settings,
            RandomSource random, int sourceX, int sourceZ) {
        int centerX = sourceX * CHUNK_SIZE + random.nextInt(CHUNK_SIZE);
        int centerZ = sourceZ * CHUNK_SIZE + random.nextInt(CHUNK_SIZE);
        double horizontalRadius = Math.sqrt((double) centerX * centerX + (double) centerZ * centerZ);
        double ringCenter = settings.ringRadius;
        if (horizontalRadius > settings.ringRadius) {
            double nearestRing = Math.rint((horizontalRadius - settings.ringRadius) / settings.ringSpacing);
            ringCenter += Math.max(0.0D, nearestRing) * settings.ringSpacing;
        }
        double distanceFromRing = Math.abs(horizontalRadius - ringCenter);
        if (distanceFromRing > settings.ringHalfWidth) {
            return null;
        }

        double ringPosition = distanceFromRing / settings.ringHalfWidth;
        if (random.nextDouble() > Math.pow(1.0D - ringPosition, settings.ringConcentration)) {
            return null;
        }

        int diameter = settings.minimumDiameter + random.nextInt(settings.maximumDiameter - settings.minimumDiameter + 1);
        int radius = Math.max(1, diameter / 2);
        double beltOffset = random.nextDouble() - random.nextDouble();
        beltOffset = Math.copySign(Math.pow(Math.abs(beltOffset), settings.beltConcentration), beltOffset);
        int centerY = settings.beltCenterY + (int) Math.round(beltOffset * settings.beltHalfHeight);
        long asteroidSeed = seedFor(level.getSeed(), sourceX, sourceZ, random.nextInt());
        RandomSource asteroidRandom = RandomSource.create(asteroidSeed);
        boolean ice = asteroidRandom.nextDouble() < settings.iceAsteroidChance;
        BlockState material = ice
                ? chooseWeighted(settings.iceMaterials, asteroidRandom, Blocks.ICE.defaultBlockState())
                : chooseBiomeMaterial(level, chunkGenerator, settings, new BlockPos(centerX, centerY, centerZ), asteroidRandom);
        return new Asteroid(centerX, centerY, centerZ, radius, asteroidSeed, material, ice);
    }

    private static BlockState chooseBiomeMaterial(WorldGenLevel level, ChunkGenerator chunkGenerator, Settings settings,
            BlockPos center, RandomSource random) {
        RandomState randomState = level.getLevel().getChunkSource().randomState();
        Holder<Biome> biome = chunkGenerator.getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(center.getX()), QuartPos.fromBlock(center.getY()), QuartPos.fromBlock(center.getZ()), randomState.sampler());
        String biomeId = biome.unwrapKey().map(key -> key.location().toString()).orElse("");
        for (BiomeMaterial override : settings.biomeMaterials) {
            if (override.biomeId.equals(biomeId)) {
                return override.block.defaultBlockState();
            }
        }
        return chooseWeighted(settings.materials, random, Blocks.STONE.defaultBlockState());
    }

    private static boolean placeAsteroid(WorldGenLevel level, Settings settings, Asteroid asteroid,
            int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        boolean placed = false;
        int radius = asteroid.radius;
        int minX = Math.max(chunkMinX, asteroid.centerX - radius);
        int maxX = Math.min(chunkMaxX, asteroid.centerX + radius);
        int minZ = Math.max(chunkMinZ, asteroid.centerZ - radius);
        int maxZ = Math.min(chunkMaxZ, asteroid.centerZ + radius);
        int minY = Math.max(level.getMinBuildHeight(), asteroid.centerY - radius);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, asteroid.centerY + radius);
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int offsetX = x - asteroid.centerX;
                    int offsetY = y - asteroid.centerY;
                    int offsetZ = z - asteroid.centerZ;
                    double distance = (double) (offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ) / (double) (radius * radius);
                    if (distance > 1.0D) {
                        continue;
                    }

                    RandomSource blockRandom = RandomSource.create(seedFor(asteroid.seed, x, y, z));
                    if (distance > 0.84D && blockRandom.nextDouble() < 0.22D) {
                        continue;
                    }

                    position.set(x, y, z);
                    if (!level.getBlockState(position).isAir()) {
                        continue;
                    }

                    BlockState block = asteroid.material;
                    if (!asteroid.isIce()) {
                        for (WeightedBlock resource : settings.resources) {
                            if (blockRandom.nextDouble() < resource.weight) {
                                block = resource.block.defaultBlockState();
                                break;
                            }
                        }
                    }

                    level.setBlock(position, block, 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static BlockState chooseWeighted(List<WeightedBlock> blocks, RandomSource random, BlockState fallback) {
        double totalWeight = 0.0D;
        for (WeightedBlock block : blocks) {
            totalWeight += block.weight;
        }

        if (totalWeight <= 0.0D) {
            return fallback;
        }

        double selection = random.nextDouble() * totalWeight;
        for (WeightedBlock block : blocks) {
            selection -= block.weight;
            if (selection < 0.0D) {
                return block.block.defaultBlockState();
            }
        }
        return blocks.get(blocks.size() - 1).block.defaultBlockState();
    }

    private static long seedFor(long seed, long x, long y, long z) {
        long mixed = seed ^ (x * 341873128712L) ^ (y * 132897987541L) ^ (z * 42317861L);
        mixed ^= mixed >>> 30;
        mixed *= -4658895280553007687L;
        mixed ^= mixed >>> 27;
        mixed *= -7723592293110705685L;
        return mixed ^ (mixed >>> 31);
    }

    private static long seedFor(long seed, long x, long z) {
        return seedFor(seed, x, 0L, z);
    }

    private record Asteroid(int centerX, int centerY, int centerZ, int radius, long seed, BlockState material, boolean isIce) {
        private boolean intersects(int minX, int maxX, int minZ, int maxZ, int maximumRadius) {
            return centerX + maximumRadius >= minX && centerX - maximumRadius <= maxX
                    && centerZ + maximumRadius >= minZ && centerZ - maximumRadius <= maxZ;
        }
    }

    private record WeightedBlock(Block block, double weight) {
    }

    private record BiomeMaterial(String biomeId, Block block) {
    }

    private static final class Settings {
        private final double asteroidsPerChunk;
        private final int beltCenterY;
        private final int beltHalfHeight;
        private final double beltConcentration;
        private final int ringRadius;
        private final int ringSpacing;
        private final int ringHalfWidth;
        private final double ringConcentration;
        private final int minimumDiameter;
        private final int maximumDiameter;
        private final double iceAsteroidChance;
        private final List<WeightedBlock> materials;
        private final List<BiomeMaterial> biomeMaterials;
        private final List<WeightedBlock> resources;
        private final List<WeightedBlock> iceMaterials;

        private Settings(double asteroidsPerChunk, int beltCenterY, int beltHalfHeight, double beltConcentration,
                int ringRadius, int ringSpacing, int ringHalfWidth, double ringConcentration, int minimumDiameter,
                int maximumDiameter, double iceAsteroidChance, List<WeightedBlock> materials,
                List<BiomeMaterial> biomeMaterials, List<WeightedBlock> resources, List<WeightedBlock> iceMaterials) {
            this.asteroidsPerChunk = asteroidsPerChunk;
            this.beltCenterY = beltCenterY;
            this.beltHalfHeight = beltHalfHeight;
            this.beltConcentration = beltConcentration;
            this.ringRadius = ringRadius;
            this.ringSpacing = ringSpacing;
            this.ringHalfWidth = ringHalfWidth;
            this.ringConcentration = ringConcentration;
            this.minimumDiameter = minimumDiameter;
            this.maximumDiameter = maximumDiameter;
            this.iceAsteroidChance = iceAsteroidChance;
            this.materials = materials;
            this.biomeMaterials = biomeMaterials;
            this.resources = resources;
            this.iceMaterials = iceMaterials;
        }

        private static Settings read() {
            AsteroidConfig.Server config = AsteroidConfig.SERVER;
            int minimumDiameter = Math.min(config.minimumDiameter.get(), config.maximumDiameter.get());
            int maximumDiameter = Math.max(config.minimumDiameter.get(), config.maximumDiameter.get());
            return new Settings(config.asteroidsPerChunk.get(), config.beltCenterY.get(), config.beltHalfHeight.get(),
                    config.beltConcentration.get(),
                    config.ringRadius.get(), config.ringSpacing.get(), config.ringHalfWidth.get(), config.ringConcentration.get(),
                    minimumDiameter, maximumDiameter, config.iceAsteroidChance.get(),
                    parseWeighted(config.asteroidMaterials.get()), parseBiomeMaterials(config.biomeMaterials.get()),
                    parseWeighted(config.asteroidResources.get()), parseWeighted(config.iceMaterials.get()));
        }

        private static List<WeightedBlock> parseWeighted(List<? extends String> entries) {
            List<WeightedBlock> parsed = new ArrayList<>();
            for (String entry : entries) {
                String[] parts = entry.split("=", 2);
                if (parts.length != 2) {
                    continue;
                }

                ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
                if (id == null) {
                    continue;
                }
                Optional<Block> block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(id);
                try {
                    double weight = Double.parseDouble(parts[1].trim());
                    if (block.isPresent() && block.get() != Blocks.AIR && weight > 0.0D) {
                        parsed.add(new WeightedBlock(block.get(), weight));
                    }
                } catch (NumberFormatException ignored) {
                    // Invalid entries are ignored so one bad config line does not stop world generation.
                }
            }
            return parsed;
        }

        private static List<BiomeMaterial> parseBiomeMaterials(List<? extends String> entries) {
            List<BiomeMaterial> parsed = new ArrayList<>();
            for (String entry : entries) {
                String[] parts = entry.split("=", 2);
                if (parts.length != 2) {
                    continue;
                }

                ResourceLocation blockId = ResourceLocation.tryParse(parts[1].trim());
                Optional<Block> block = blockId == null
                        ? Optional.empty()
                        : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(blockId);
                if (block.isPresent() && block.get() != Blocks.AIR) {
                    parsed.add(new BiomeMaterial(parts[0].trim(), block.get()));
                }
            }
            return parsed;
        }
    }
}
