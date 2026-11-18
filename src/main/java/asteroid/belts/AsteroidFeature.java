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
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class AsteroidFeature extends Feature<NoneFeatureConfiguration> {
    private static final int CHUNK_SIZE = 16;
    private static final int VOID_FLOOR_Y = 300;

    public AsteroidFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (level.getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }

        Settings settings = Settings.read(context.chunkGenerator());
        if (settings.asteroidsPerChunk <= 0.0D
                || (settings.materials.isEmpty() && settings.additionalAsteroidTypes.isEmpty())) {
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
        BlockPos spawnPosition = level.getLevel().getSharedSpawnPos();
        boolean placed = false;

        for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX++) {
            for (int sourceZ = sourceMinZ; sourceZ <= sourceMaxZ; sourceZ++) {
                RandomSource random = RandomSource.create(seedFor(level.getSeed(), sourceX, sourceZ));
                int guaranteedAsteroids = (int) Math.floor(settings.asteroidsPerChunk);
                int asteroidSlots = guaranteedAsteroids + (random.nextDouble() < settings.asteroidsPerChunk - guaranteedAsteroids ? 1 : 0);

                for (int slot = 0; slot < asteroidSlots; slot++) {
                    Asteroid asteroid = createAsteroid(level, context.chunkGenerator(), settings, random, sourceX, sourceZ);
                    if (asteroid != null && (settings.voidBelowAsteroids
                            || !asteroid.coversHorizontal(spawnPosition.getX(), spawnPosition.getZ()))
                            && asteroid.intersects(chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, maximumRadius)) {
                        placed |= placeAsteroid(level, settings, asteroid, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
                    }
                }
            }
        }

        return placed;
    }

    public static void clearBelowAsteroids(ChunkAccess chunk) {
        int maximumY = Math.min(chunk.getMaxBuildHeight() - 1, VOID_FLOOR_Y - 1);
        if (maximumY < chunk.getMinBuildHeight()) {
            return;
        }

        int firstSection = Math.floorDiv(chunk.getMinBuildHeight(), 16);
        int lastSection = Math.floorDiv(maximumY, 16);
        for (int sectionY = firstSection; sectionY <= lastSection; sectionY++) {
            int sectionIndex = sectionY - firstSection;
            int sectionMinY = sectionY * 16;
            int sectionMaxY = sectionMinY + 15;
            if (sectionMaxY <= maximumY) {
                clearBlockEntities(chunk, sectionMinY, sectionMaxY);
                LevelChunkSection section = chunk.getSections()[sectionIndex];
                if (!section.hasOnlyAir()) {
                    chunk.getSections()[sectionIndex] = new LevelChunkSection(
                            new net.minecraft.world.level.chunk.PalettedContainer<>(
                                    Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(),
                                    net.minecraft.world.level.chunk.PalettedContainer.Strategy.SECTION_STATES),
                            section.getBiomes());
                }
                continue;
            }

            clearPartialSection(chunk, sectionMinY, maximumY);
        }
        chunk.setUnsaved(true);
    }

    private static void clearPartialSection(ChunkAccess chunk, int minimumY, int maximumY) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunk.getPos().getMaxBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunk.getPos().getMaxBlockZ();
        clearBlockEntities(chunk, minimumY, maximumY);
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        for (int x = chunkMinX; x <= chunkMaxX; x++) {
            for (int y = minimumY; y <= maximumY; y++) {
                for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
                    position.set(x, y, z);
                    if (!chunk.getBlockState(position).isAir()) {
                        chunk.setBlockState(position, Blocks.AIR.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    private static void clearBlockEntities(ChunkAccess chunk, int minimumY, int maximumY) {
        for (BlockPos position : List.copyOf(chunk.getBlockEntitiesPos())) {
            if (position.getY() >= minimumY && position.getY() <= maximumY) {
                chunk.removeBlockEntity(position);
            }
        }
    }

    private static Asteroid createAsteroid(WorldGenLevel level, ChunkGenerator chunkGenerator, Settings settings,
            RandomSource random, int sourceX, int sourceZ) {
        int centerX = sourceX * CHUNK_SIZE + random.nextInt(CHUNK_SIZE);
        int centerZ = sourceZ * CHUNK_SIZE + random.nextInt(CHUNK_SIZE);
        double diameterRoll = random.nextDouble();
        double beltOffset = random.nextDouble() - random.nextDouble();
        beltOffset = Math.copySign(Math.pow(Math.abs(beltOffset), settings.beltConcentration), beltOffset);
        int centerY = settings.beltCenterY + (int) Math.round(beltOffset * settings.beltHalfHeight);
        BlockPos center = new BlockPos(centerX, centerY, centerZ);
        long asteroidSeed = seedFor(level.getSeed(), sourceX, sourceZ, random.nextInt());
        RandomSource asteroidRandom = RandomSource.create(asteroidSeed);
        MaterialChoice additionalMaterial = chooseAdditionalMaterial(settings.additionalAsteroidTypes, asteroidRandom);
        if (additionalMaterial == null && settings.excludedBiomes.contains(biomeIdAt(level, chunkGenerator, center))) {
            return null;
        }
        RandomSource shapeRandom = RandomSource.create(seedFor(asteroidSeed, 17L, 31L, 47L));
        NormalNoise macroNoise = NormalNoise.create(
                RandomSource.create(seedFor(asteroidSeed, 101L, 211L, 307L)), -2, 1.0D, 0.5D, 0.25D);
        NormalNoise detailNoise = NormalNoise.create(
                RandomSource.create(seedFor(asteroidSeed, 401L, 503L, 601L)), -4, 1.0D, 0.5D, 0.25D);
        boolean irregular = settings.shape == AsteroidShape.IRREGULAR;
        double skewXY = irregular ? signedSkew(shapeRandom) : 0.0D;
        double skewXZ = irregular ? signedSkew(shapeRandom) : 0.0D;
        double skewYX = irregular ? signedSkew(shapeRandom) : 0.0D;
        double skewYZ = irregular ? signedSkew(shapeRandom) : 0.0D;
        double skewZX = irregular ? signedSkew(shapeRandom) : 0.0D;
        double skewZY = irregular ? signedSkew(shapeRandom) : 0.0D;
        double centerBiasX = irregular ? signedSkew(shapeRandom) * 0.9D : 0.0D;
        double centerBiasY = irregular ? signedSkew(shapeRandom) * 0.9D : 0.0D;
        double centerBiasZ = irregular ? signedSkew(shapeRandom) * 0.9D : 0.0D;
        RandomSource tunnelRandom = RandomSource.create(seedFor(asteroidSeed, 701L, 809L, 907L));
        Tunnel[] tunnels = createTunnels(asteroidSeed, tunnelRandom,
                asteroidRandom.nextDouble() < settings.caveAsteroidChance);
        RandomSource craterRandom = RandomSource.create(seedFor(asteroidSeed, 1103L, 1201L, 1301L));
        Crater[] craters = irregular ? createCraters(craterRandom) : new Crater[0];
        boolean ice = additionalMaterial == null && asteroidRandom.nextDouble() < settings.iceAsteroidChance;
        MaterialChoice material = additionalMaterial != null
                ? additionalMaterial
                : ice
                ? new MaterialChoice(chooseWeighted(settings.iceMaterials, asteroidRandom, Blocks.ICE.defaultBlockState()), false)
                : chooseBiomeMaterial(level, chunkGenerator, settings, center, asteroidRandom);
        int minimumDiameter = settings.minimumDiameter;
        int maximumDiameter = settings.maximumDiameter;
        if (material.biomeSpecific && Settings.isAmplified(chunkGenerator)) {
            minimumDiameter *= 4;
            maximumDiameter *= 4;
        }
        int diameterRange = maximumDiameter - minimumDiameter + 1;
        int diameter = minimumDiameter + (int) Math.floor(Math.pow(diameterRoll, 2.0D) * diameterRange);
        int radius = Math.max(1, diameter / 2);
        int radiusX = settings.shape == AsteroidShape.SPHERE ? radius : variedRadius(radius, shapeRandom);
        int radiusY = settings.shape == AsteroidShape.SPHERE ? radius : variedRadius(radius, shapeRandom);
        int radiusZ = settings.shape == AsteroidShape.SPHERE ? radius : variedRadius(radius, shapeRandom);
        return new Asteroid(centerX, centerY, centerZ, radius, radiusX, radiusY, radiusZ, asteroidSeed, material.block,
                ice, settings.shape,
                macroNoise, detailNoise, skewXY, skewXZ, skewYX, skewYZ, skewZX, skewZY,
                centerBiasX, centerBiasY, centerBiasZ, tunnels, craters);
    }

    private static int variedRadius(int radius, RandomSource random) {
        return Math.max(1, (int) Math.round(radius * (0.50D + random.nextDouble() * 0.50D)));
    }

    private static double signedSkew(RandomSource random) {
        return (random.nextDouble() - 0.5D) * 0.44D;
    }

    private static Crater[] createCraters(RandomSource random) {
        int craterCount = 4 + random.nextInt(6);
        Crater[] craters = new Crater[craterCount];
        for (int index = 0; index < craterCount; index++) {
            double directionX = random.nextDouble() * 2.0D - 1.0D;
            double directionY = random.nextDouble() * 2.0D - 1.0D;
            double directionZ = random.nextDouble() * 2.0D - 1.0D;
            double directionLength = Math.sqrt(directionX * directionX + directionY * directionY
                    + directionZ * directionZ);
            if (directionLength < 0.1D) {
                directionX = 0.0D;
                directionY = 1.0D;
                directionZ = 0.0D;
                directionLength = 1.0D;
            }
            craters[index] = new Crater(directionX / directionLength, directionY / directionLength,
                    directionZ / directionLength, 0.08D + random.nextDouble() * 0.14D,
                    0.08D + random.nextDouble() * 0.14D);
        }
        return craters;
    }

    private static Tunnel[] createTunnels(long asteroidSeed, RandomSource random, boolean hasTunnels) {
        if (!hasTunnels) {
            return new Tunnel[0];
        }

        int tunnelCount = random.nextDouble() < 0.25D ? 2 : 1;
        Tunnel[] tunnels = new Tunnel[tunnelCount];
        for (int index = 0; index < tunnelCount; index++) {
            tunnels[index] = createTunnel(asteroidSeed, random);
        }
        return tunnels;
    }

    private static Tunnel createTunnel(long asteroidSeed, RandomSource random) {
        double axisX = random.nextDouble() * 2.0D - 1.0D;
        double axisY = random.nextDouble() * 2.0D - 1.0D;
        double axisZ = random.nextDouble() * 2.0D - 1.0D;
        double axisLength = Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        if (axisLength < 0.1D) {
            axisX = 1.0D;
            axisY = 0.0D;
            axisZ = 0.0D;
            axisLength = 1.0D;
        }
        axisX /= axisLength;
        axisY /= axisLength;
        axisZ /= axisLength;

        double referenceX = Math.abs(axisY) < 0.85D ? 0.0D : 1.0D;
        double referenceY = Math.abs(axisY) < 0.85D ? 1.0D : 0.0D;
        double referenceZ = 0.0D;
        double perpendicularX = axisY * referenceZ - axisZ * referenceY;
        double perpendicularY = axisZ * referenceX - axisX * referenceZ;
        double perpendicularZ = axisX * referenceY - axisY * referenceX;
        double perpendicularLength = Math.sqrt(perpendicularX * perpendicularX
                + perpendicularY * perpendicularY + perpendicularZ * perpendicularZ);
        perpendicularX /= perpendicularLength;
        perpendicularY /= perpendicularLength;
        perpendicularZ /= perpendicularLength;

        double secondPerpendicularX = axisY * perpendicularZ - axisZ * perpendicularY;
        double secondPerpendicularY = axisZ * perpendicularX - axisX * perpendicularZ;
        double secondPerpendicularZ = axisX * perpendicularY - axisY * perpendicularX;
        NormalNoise tunnelNoise = NormalNoise.create(
                RandomSource.create(seedFor(asteroidSeed, random.nextLong(), 953L, 997L)), -3,
                1.0D, 0.5D, 0.25D);
        double radius = 0.10D + random.nextDouble() * 0.10D;
        return new Tunnel(axisX, axisY, axisZ, perpendicularX, perpendicularY, perpendicularZ,
                secondPerpendicularX, secondPerpendicularY, secondPerpendicularZ, radius, tunnelNoise);
    }

    private static MaterialChoice chooseBiomeMaterial(WorldGenLevel level, ChunkGenerator chunkGenerator, Settings settings,
            BlockPos center, RandomSource random) {
        String biomeId = biomeIdAt(level, chunkGenerator, center);
        BiomeMaterial fallback = null;
        for (BiomeMaterial override : settings.biomeMaterials) {
            if (override.biomeId.equals(biomeId)) {
                return new MaterialChoice(override.block.defaultBlockState(), true);
            }
            if (override.biomeId.equals("*")) {
                fallback = override;
            }
        }
        if (fallback != null) {
            return new MaterialChoice(fallback.block.defaultBlockState(), true);
        }
        return new MaterialChoice(chooseWeighted(settings.materials, random, Blocks.STONE.defaultBlockState()), false);
    }

    private static MaterialChoice chooseAdditionalMaterial(List<AdditionalAsteroidType> additionalAsteroidTypes,
            RandomSource random) {
        for (AdditionalAsteroidType type : additionalAsteroidTypes) {
            if (random.nextDouble() < type.chance) {
                return new MaterialChoice(type.block.defaultBlockState(), false);
            }
        }
        return null;
    }

    private static String biomeIdAt(WorldGenLevel level, ChunkGenerator chunkGenerator, BlockPos center) {
        RandomState randomState = level.getLevel().getChunkSource().randomState();
        Holder<Biome> biome = chunkGenerator.getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(center.getX()), QuartPos.fromBlock(center.getY()), QuartPos.fromBlock(center.getZ()), randomState.sampler());
        return biome.unwrapKey().map(key -> key.location().toString()).orElse("");
    }

    private static boolean placeAsteroid(WorldGenLevel level, Settings settings, Asteroid asteroid,
            int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        boolean placed = false;
        int minX = Math.max(chunkMinX, asteroid.centerX - asteroid.radiusX);
        int maxX = Math.min(chunkMaxX, asteroid.centerX + asteroid.radiusX);
        int minZ = Math.max(chunkMinZ, asteroid.centerZ - asteroid.radiusZ);
        int maxZ = Math.min(chunkMaxZ, asteroid.centerZ + asteroid.radiusZ);
        int minY = Math.max(level.getMinBuildHeight(), asteroid.centerY - asteroid.radiusY);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, asteroid.centerY + asteroid.radiusY);
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int offsetX = x - asteroid.centerX;
                    int offsetY = y - asteroid.centerY;
                    int offsetZ = z - asteroid.centerZ;
                    double normalizedX = (double) offsetX / asteroid.radiusX;
                    double normalizedY = (double) offsetY / asteroid.radiusY;
                    double normalizedZ = (double) offsetZ / asteroid.radiusZ;
                    double distance = asteroid.shapeDistance(normalizedX, normalizedY, normalizedZ);
                    if (distance > 1.0D) {
                        continue;
                    }

                    if (asteroid.isTunnel(normalizedX, normalizedY, normalizedZ)) {
                        continue;
                    }

                    RandomSource blockRandom = RandomSource.create(seedFor(asteroid.seed, x, y, z));
                    if (distance > 0.76D && blockRandom.nextDouble() < 0.10D) {
                        continue;
                    }

                    position.set(x, y, z);
                    BlockState existing = level.getBlockState(position);
                    if (!existing.isAir() && !isSnow(existing)) {
                        continue;
                    }

                    BlockState block = asteroid.material;
                    if (!asteroid.isIce()) {
                        for (ResourceRule resource : settings.resources) {
                            if (resource.appliesAt(y) && blockRandom.nextDouble() < resource.chance) {
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
        clearSnowOnSurface(level, asteroid, minX, maxX, minY, maxY, minZ, maxZ, position);
        return placed;
    }

    private static void clearSnowOnSurface(WorldGenLevel level, Asteroid asteroid, int minX, int maxX,
            int minY, int maxY, int minZ, int maxZ, BlockPos.MutableBlockPos position) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= Math.min(level.getMaxBuildHeight() - 1, maxY + 1); y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    position.set(x, y, z);
                    if (!isSnow(level.getBlockState(position)) || !insideAsteroid(asteroid, x, y - 1, z)) {
                        continue;
                    }
                    level.setBlock(position, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static boolean insideAsteroid(Asteroid asteroid, int x, int y, int z) {
        double normalizedX = (double) (x - asteroid.centerX) / asteroid.radiusX;
        double normalizedY = (double) (y - asteroid.centerY) / asteroid.radiusY;
        double normalizedZ = (double) (z - asteroid.centerZ) / asteroid.radiusZ;
        return asteroid.shapeDistance(normalizedX, normalizedY, normalizedZ) <= 1.0D
                && !asteroid.isTunnel(normalizedX, normalizedY, normalizedZ);
    }

    private static boolean isSnow(BlockState state) {
        return state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK);
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

    private record Asteroid(int centerX, int centerY, int centerZ, int radius, int radiusX, int radiusY, int radiusZ,
            long seed, BlockState material, boolean isIce, AsteroidShape shape, NormalNoise macroNoise, NormalNoise detailNoise,
            double skewXY, double skewXZ, double skewYX, double skewYZ, double skewZX, double skewZY,
            double centerBiasX, double centerBiasY, double centerBiasZ, Tunnel[] tunnels, Crater[] craters) {
        private double shapeDistance(double normalizedX, double normalizedY, double normalizedZ) {
            double baseDistance = normalizedX * normalizedX + normalizedY * normalizedY + normalizedZ * normalizedZ;
            if (shape != AsteroidShape.IRREGULAR) {
                return baseDistance;
            }

            double shearedX = normalizedX + normalizedY * skewXY + normalizedZ * skewXZ + centerBiasX;
            double shearedY = normalizedY + normalizedX * skewYX + normalizedZ * skewYZ + centerBiasY;
            double shearedZ = normalizedZ + normalizedX * skewZX + normalizedY * skewZY + centerBiasZ;
            double warpX = macroNoise.getValue(shearedX * 1.25D + 11.0D, shearedY * 1.25D + 17.0D,
                    shearedZ * 1.25D + 23.0D) * 0.22D;
            double warpY = macroNoise.getValue(shearedX * 1.25D + 37.0D, shearedY * 1.25D + 43.0D,
                    shearedZ * 1.25D + 47.0D) * 0.22D;
            double warpZ = macroNoise.getValue(shearedX * 1.25D + 53.0D, shearedY * 1.25D + 59.0D,
                    shearedZ * 1.25D + 61.0D) * 0.22D;
            double warpedX = shearedX + warpX;
            double warpedY = shearedY + warpY;
            double warpedZ = shearedZ + warpZ;
            double broadNoise = macroNoise.getValue(warpedX * 1.8D + 71.0D, warpedY * 1.8D + 79.0D,
                    warpedZ * 1.8D + 83.0D);
            double detailNoiseValue = detailNoise.getValue(warpedX * 5.0D + 89.0D, warpedY * 5.0D + 97.0D,
                    warpedZ * 5.0D + 101.0D);
            double boundary = Math.min(1.0D,
                    Math.max(0.42D, 0.78D + broadNoise * 0.16D + detailNoiseValue * 0.10D));
            double radialDistance = Math.sqrt(baseDistance);
            if (radialDistance > 0.35D) {
                double surfaceX = normalizedX / radialDistance;
                double surfaceY = normalizedY / radialDistance;
                double surfaceZ = normalizedZ / radialDistance;
                for (Crater crater : craters) {
                    double dot = surfaceX * crater.directionX + surfaceY * crater.directionY
                            + surfaceZ * crater.directionZ;
                    double craterProgress = (1.0D - dot) / crater.angularRadius;
                    if (craterProgress < 1.0D) {
                        double bowl = 1.0D - craterProgress;
                        double rim = Math.max(0.0D, 1.0D - Math.abs(craterProgress - 0.82D) / 0.18D);
                        boundary += crater.depth * 0.18D * rim - crater.depth * bowl * bowl;
                    }
                }
            }
            boundary = Math.min(1.0D, Math.max(0.35D, boundary));
            return baseDistance / (boundary * boundary);
        }

        private boolean isTunnel(double normalizedX, double normalizedY, double normalizedZ) {
            for (Tunnel tunnel : tunnels) {
                if (tunnel.contains(normalizedX, normalizedY, normalizedZ)) {
                    return true;
                }
            }
            return false;
        }

        private boolean intersects(int minX, int maxX, int minZ, int maxZ, int maximumRadius) {
            return centerX + maximumRadius >= minX && centerX - maximumRadius <= maxX
                    && centerZ + maximumRadius >= minZ && centerZ - maximumRadius <= maxZ;
        }

        private boolean coversHorizontal(int x, int z) {
            return x >= centerX - radiusX && x <= centerX + radiusX
                    && z >= centerZ - radiusZ && z <= centerZ + radiusZ;
        }
    }

    private record Tunnel(double axisX, double axisY, double axisZ,
            double perpendicularX, double perpendicularY, double perpendicularZ,
            double secondPerpendicularX, double secondPerpendicularY, double secondPerpendicularZ,
            double radius, NormalNoise noise) {
        private boolean contains(double x, double y, double z) {
            double longitudinal = x * axisX + y * axisY + z * axisZ;
            if (Math.abs(longitudinal) > 1.05D) {
                return false;
            }

            double offsetOne = noise.getValue(longitudinal * 1.45D + 13.0D, 29.0D, 41.0D) * 0.24D;
            double offsetTwo = noise.getValue(longitudinal * 1.45D + 53.0D, 67.0D, 79.0D) * 0.24D;
            double lateralOne = x * perpendicularX + y * perpendicularY + z * perpendicularZ - offsetOne;
            double lateralTwo = x * secondPerpendicularX + y * secondPerpendicularY
                    + z * secondPerpendicularZ - offsetTwo;
            double localRadius = radius * (0.75D
                    + noise.getValue(longitudinal * 2.2D + 89.0D, 97.0D, 103.0D) * 0.25D);
            return lateralOne * lateralOne + lateralTwo * lateralTwo <= localRadius * localRadius;
        }
    }

    private record Crater(double directionX, double directionY, double directionZ,
            double angularRadius, double depth) {
    }

    private record WeightedBlock(Block block, double weight) {
    }

    private record AdditionalAsteroidType(Block block, double chance) {
    }

    private record MaterialChoice(BlockState block, boolean biomeSpecific) {
    }

    private record ResourceRule(Block block, double chance, List<YRange> ranges) {
        private boolean appliesAt(int y) {
            for (YRange range : ranges) {
                if (y >= range.minimum && y <= range.maximum) {
                    return true;
                }
            }
            return false;
        }
    }

    private record YRange(int minimum, int maximum) {
    }

    private record BiomeMaterial(String biomeId, Block block) {
    }

    private enum AsteroidShape {
        IRREGULAR,
        ELLIPSOID,
        SPHERE;

        private static AsteroidShape fromConfig(String value) {
            try {
                return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException | NullPointerException exception) {
                return IRREGULAR;
            }
        }
    }

    private static final class Settings {
        private final double asteroidsPerChunk;
        private final int beltCenterY;
        private final int beltHalfHeight;
        private final double beltConcentration;
        private final int minimumDiameter;
        private final int maximumDiameter;
        private final AsteroidShape shape;
        private final double iceAsteroidChance;
        private final double caveAsteroidChance;
        private final boolean voidBelowAsteroids;
        private final List<WeightedBlock> materials;
        private final List<AdditionalAsteroidType> additionalAsteroidTypes;
        private final List<BiomeMaterial> biomeMaterials;
        private final List<String> excludedBiomes;
        private final List<ResourceRule> resources;
        private final List<WeightedBlock> iceMaterials;

        private Settings(double asteroidsPerChunk, int beltCenterY, int beltHalfHeight, double beltConcentration,
                int minimumDiameter,
                int maximumDiameter, AsteroidShape shape, double iceAsteroidChance, double caveAsteroidChance,
                boolean voidBelowAsteroids, List<WeightedBlock> materials,
                List<AdditionalAsteroidType> additionalAsteroidTypes, List<BiomeMaterial> biomeMaterials,
                List<String> excludedBiomes,
                List<ResourceRule> resources,
                List<WeightedBlock> iceMaterials) {
            this.asteroidsPerChunk = asteroidsPerChunk;
            this.beltCenterY = beltCenterY;
            this.beltHalfHeight = beltHalfHeight;
            this.beltConcentration = beltConcentration;
            this.minimumDiameter = minimumDiameter;
            this.maximumDiameter = maximumDiameter;
            this.shape = shape;
            this.iceAsteroidChance = iceAsteroidChance;
            this.caveAsteroidChance = caveAsteroidChance;
            this.voidBelowAsteroids = voidBelowAsteroids;
            this.materials = materials;
            this.additionalAsteroidTypes = additionalAsteroidTypes;
            this.biomeMaterials = biomeMaterials;
            this.excludedBiomes = excludedBiomes;
            this.resources = resources;
            this.iceMaterials = iceMaterials;
        }

        private static Settings read(ChunkGenerator chunkGenerator) {
            AsteroidConfig.Server config = AsteroidConfig.SERVER;
            int minimumDiameter = Math.min(config.minimumDiameter.get(), config.maximumDiameter.get());
            int maximumDiameter = Math.max(config.minimumDiameter.get(), config.maximumDiameter.get());
            return new Settings(config.asteroidsPerChunk.get(), config.beltCenterY.get(), config.beltHalfHeight.get(),
                    config.beltConcentration.get(),
                    minimumDiameter, maximumDiameter, AsteroidShape.fromConfig(config.asteroidShape.get()),
                    config.iceAsteroidChance.get(),
                    config.caveAsteroidChance.get(), config.voidBelowAsteroids.get(),
                    parseWeighted(config.asteroidMaterials.get()), parseAdditionalAsteroidTypes(config.additionalAsteroidTypes.get()),
                    parseBiomeMaterials(config.biomeMaterials.get()),
                    parseBiomeIds(config.excludedBiomes.get()),
                    parseResources(config.asteroidResources.get()), parseWeighted(config.iceMaterials.get()));
        }

        private static List<String> parseBiomeIds(List<? extends String> entries) {
            List<String> parsed = new ArrayList<>();
            for (String entry : entries) {
                if (entry != null && !entry.isBlank()) {
                    parsed.add(entry.trim());
                }
            }
            return parsed;
        }

        private static boolean isAmplified(ChunkGenerator chunkGenerator) {
            return chunkGenerator instanceof NoiseBasedChunkGenerator noiseGenerator
                    && noiseGenerator.generatorSettings().is(NoiseGeneratorSettings.AMPLIFIED);
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

        private static List<AdditionalAsteroidType> parseAdditionalAsteroidTypes(List<? extends String> entries) {
            List<AdditionalAsteroidType> parsed = new ArrayList<>();
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
                    double chance = Double.parseDouble(parts[1].trim());
                    if (block.isPresent() && block.get() != Blocks.AIR && chance > 0.0D && chance <= 1.0D) {
                        parsed.add(new AdditionalAsteroidType(block.get(), chance));
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

        private static List<ResourceRule> parseResources(List<? extends String> entries) {
            List<ResourceRule> parsed = new ArrayList<>();
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
                if (block.isEmpty() || block.get() == Blocks.AIR) {
                    continue;
                }

                try {
                    String[] resourceParts = parts[1].split(";", 2);
                    if (resourceParts.length != 2) {
                        continue;
                    }
                    double chance = Double.parseDouble(resourceParts[0].trim());
                    List<YRange> ranges = new ArrayList<>();
                    for (String range : resourceParts[1].split(",")) {
                        String[] bounds = range.trim().split("\\.\\.", 2);
                        if (bounds.length != 2) {
                            ranges.clear();
                            break;
                        }
                        int first = Integer.parseInt(bounds[0].trim());
                        int second = Integer.parseInt(bounds[1].trim());
                        ranges.add(new YRange(Math.min(first, second), Math.max(first, second)));
                    }
                    if (chance > 0.0D && chance <= 1.0D && !ranges.isEmpty()) {
                        parsed.add(new ResourceRule(block.get(), chance, ranges));
                    }
                } catch (NumberFormatException ignored) {
                    // Invalid entries are ignored so one bad config line does not stop world generation.
                }
            }
            return parsed;
        }
    }
}
