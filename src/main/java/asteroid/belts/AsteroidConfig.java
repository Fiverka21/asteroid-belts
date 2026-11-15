package asteroid.belts;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AsteroidConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        SERVER = new Server(builder);
        SERVER_SPEC = builder.build();
    }

    private AsteroidConfig() {
    }

    public static final class Server {
        public final ModConfigSpec.DoubleValue asteroidsPerChunk;
        public final ModConfigSpec.IntValue beltCenterY;
        public final ModConfigSpec.IntValue beltHalfHeight;
        public final ModConfigSpec.DoubleValue beltConcentration;
        public final ModConfigSpec.IntValue ringRadius;
        public final ModConfigSpec.IntValue ringSpacing;
        public final ModConfigSpec.IntValue ringHalfWidth;
        public final ModConfigSpec.DoubleValue ringConcentration;
        public final ModConfigSpec.IntValue minimumDiameter;
        public final ModConfigSpec.IntValue maximumDiameter;
        public final ModConfigSpec.DoubleValue iceAsteroidChance;
        public final ModConfigSpec.ConfigValue<List<? extends String>> asteroidMaterials;
        public final ModConfigSpec.ConfigValue<List<? extends String>> biomeMaterials;
        public final ModConfigSpec.ConfigValue<List<? extends String>> asteroidResources;
        public final ModConfigSpec.ConfigValue<List<? extends String>> iceMaterials;

        private Server(ModConfigSpec.Builder builder) {
            builder.push("asteroid_belt");

            asteroidsPerChunk = builder
                    .comment("Average number of asteroid centers selected per source chunk.",
                            "Set to 0 to disable asteroid generation.")
                    .defineInRange("asteroids_per_chunk", 20.0D, 0.0D, 32.0D);
            beltCenterY = builder
                    .comment("The Y coordinate at the center of the asteroid belt.")
                    .defineInRange("belt_center_y", 512, -2032, 2032);
            beltHalfHeight = builder
                    .comment("How far the belt extends above and below its center.")
                    .defineInRange("belt_half_height", 256, 0, 1024);
            beltConcentration = builder
                    .comment("How strongly asteroid centers favor the belt center.",
                            "1.0 is a mild falloff; higher values make the belt thinner and denser in the middle.")
                    .defineInRange("belt_concentration", 3.0D, 1.0D, 8.0D);
            ringRadius = builder
                    .comment("Radius of the first circular asteroid belt from the world origin, in blocks.")
                    .defineInRange("ring_radius", 1024, 0, 30000000);
            ringSpacing = builder
                    .comment("Distance between repeated circular asteroid belts.")
                    .defineInRange("ring_spacing", 1024, 1, 4096);
            ringHalfWidth = builder
                    .comment("How far each circular belt extends inward and outward from its radius.")
                    .defineInRange("ring_half_width", 256, 1, 4096);
            ringConcentration = builder
                    .comment("How strongly asteroid centers favor the middle of the circular belt.",
                            "Higher values make the circular belt edges less populated.")
                    .defineInRange("ring_concentration", 3.0D, 1.0D, 8.0D);
            minimumDiameter = builder
                    .comment("Minimum asteroid diameter in blocks.")
                    .defineInRange("minimum_diameter", 10, 2, 100);
            maximumDiameter = builder
                    .comment("Maximum asteroid diameter in blocks.")
                    .defineInRange("maximum_diameter", 100, 2, 100);
            iceAsteroidChance = builder
                    .comment("Chance that a generated asteroid is made entirely from an ice material.")
                    .defineInRange("ice_asteroid_chance", 0.025D, 0.0D, 1.0D);

            asteroidMaterials = builder
                    .comment("Weighted fallback materials in the form block_id=weight.",
                            "The selected material fills an asteroid unless a biome override applies.")
                    .defineList("asteroid_materials",
                            List.of("minecraft:stone=60", "minecraft:andesite=15", "minecraft:diorite=15", "minecraft:granite=10"),
                            () -> "minecraft:stone=1",
                            AsteroidConfig::isWeightedBlockEntry);
            biomeMaterials = builder
                    .comment("Biome material overrides in the form biome_id=block_id.",
                            "The biome at the asteroid center selects the override, keeping each asteroid consistent.")
                    .defineList("biome_materials",
                            List.of("minecraft:desert=minecraft:sandstone",
                                    "minecraft:badlands=minecraft:red_sandstone",
                                    "minecraft:wooded_badlands=minecraft:red_sandstone",
                                    "minecraft:eroded_badlands=minecraft:red_sandstone",
                                    "minecraft:stony_peaks=minecraft:granite",
                                    "minecraft:jagged_peaks=minecraft:stone",
                                    "minecraft:frozen_peaks=minecraft:packed_ice",
                                    "minecraft:deep_dark=minecraft:deepslate"),
                            () -> "minecraft:plains=minecraft:stone",
                            AsteroidConfig::isBiomeBlockEntry);
            asteroidResources = builder
                    .comment("Optional resource blocks in the form block_id=amount.",
                            "Amount is the chance per asteroid block, from 0.0 to 1.0.",
                            "The first matching entry wins, so put rarer blocks first if desired.")
                    .defineList("asteroid_resources",
                            List.of("minecraft:coal_ore=0.025", "minecraft:iron_ore=0.0125", "minecraft:copper_ore=0.01"),
                            () -> "minecraft:coal_ore=0.01",
                            AsteroidConfig::isResourceEntry);
            iceMaterials = builder
                    .comment("Weighted ice materials in the form block_id=weight.",
                            "One material is selected for each ice asteroid, making it entirely one ice type.")
                    .defineList("ice_materials",
                            List.of("minecraft:ice=70", "minecraft:packed_ice=25", "minecraft:blue_ice=5"),
                            () -> "minecraft:ice=1",
                            AsteroidConfig::isWeightedBlockEntry);

            builder.pop();
        }
    }

    private static boolean isWeightedBlockEntry(Object value) {
        String[] parts = splitEntry(value);
        if (parts == null) {
            return false;
        }

        try {
            return Double.parseDouble(parts[1]) > 0.0D;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean isResourceEntry(Object value) {
        String[] parts = splitEntry(value);
        if (parts == null) {
            return false;
        }

        try {
            double amount = Double.parseDouble(parts[1]);
            return amount > 0.0D && amount <= 1.0D;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean isBiomeBlockEntry(Object value) {
        String[] parts = splitEntry(value);
        return parts != null && !parts[0].isBlank() && !parts[1].isBlank();
    }

    private static String[] splitEntry(Object value) {
        if (!(value instanceof String entry)) {
            return null;
        }

        String[] parts = entry.split("=", 2);
        return parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank() ? parts : null;
    }
}
