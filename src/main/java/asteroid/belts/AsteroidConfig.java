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
        public final ModConfigSpec.IntValue minimumDiameter;
        public final ModConfigSpec.IntValue maximumDiameter;
        public final ModConfigSpec.DoubleValue iceAsteroidChance;
        public final ModConfigSpec.DoubleValue caveAsteroidChance;
        public final ModConfigSpec.ConfigValue<List<? extends String>> asteroidMaterials;
        public final ModConfigSpec.ConfigValue<List<? extends String>> biomeMaterials;
        public final ModConfigSpec.ConfigValue<List<? extends String>> asteroidResources;
        public final ModConfigSpec.ConfigValue<List<? extends String>> iceMaterials;

        private Server(ModConfigSpec.Builder builder) {
            builder.push("asteroid_belt");

            asteroidsPerChunk = builder
                    .comment("Average number of asteroid centers selected per source chunk.",
                            "Set to 0 to disable asteroid generation.")
                    .defineInRange("asteroids_per_chunk", 0.02D, 0.0D, 4.0D);
            beltCenterY = builder
                    .comment("The Y coordinate at the center of the asteroid belt.")
                    .defineInRange("belt_center_y", 1024, -2032, 2032);
            beltHalfHeight = builder
                    .comment("How far the belt extends above and below its center.")
                    .defineInRange("belt_half_height", 200, 0, 1024);
            beltConcentration = builder
                    .comment("How strongly asteroid centers favor the belt center.",
                            "1.0 gives a gradual triangular falloff; higher values make the belt thinner and denser in the middle.")
                    .defineInRange("belt_concentration", 2.0D, 1.0D, 8.0D);
            minimumDiameter = builder
                    .comment("Minimum asteroid diameter in blocks.")
                    .defineInRange("minimum_diameter", 10, 2, 100);
            maximumDiameter = builder
                    .comment("Maximum asteroid diameter in blocks.")
                    .defineInRange("maximum_diameter", 100, 2, 100);
            iceAsteroidChance = builder
                    .comment("Chance that a generated asteroid is made entirely from an ice material.")
                    .defineInRange("ice_asteroid_chance", 0.025D, 0.0D, 1.0D);
            caveAsteroidChance = builder
                    .comment("Chance that a generated asteroid contains one or more cave-like tunnels.")
                    .defineInRange("cave_asteroid_chance", 0.20D, 0.0D, 1.0D);

            asteroidMaterials = builder
                    .comment("Weighted fallback materials in the form block_id=weight.",
                            "The selected material fills an asteroid unless a biome override applies.")
                    .defineList("asteroid_materials",
                            List.of("minecraft:stone=60", "minecraft:andesite=15", "minecraft:diorite=15", "minecraft:granite=10"),
                            () -> "minecraft:stone=1",
                            AsteroidConfig::isWeightedBlockEntry);
            biomeMaterials = builder
                    .comment("Biome material overrides in the form biome_id=block_id.",
                            "Use one entry per line. The biome at the asteroid center selects the override, keeping each asteroid consistent.",
                            "Use *=block_id as the fallback for biomes without an explicit entry, including biomes from other mods.")
                    .defineList("biome_materials",
                            List.of(
                                    "minecraft:badlands=minecraft:granite",
                                    "minecraft:bamboo_jungle=minecraft:diorite",
                                    "minecraft:beach=minecraft:andesite",
                                    "minecraft:birch_forest=minecraft:diorite",
                                    "minecraft:cherry_grove=minecraft:granite",
                                    "minecraft:cold_ocean=minecraft:andesite",
                                    "minecraft:dark_forest=minecraft:deepslate",
                                    "minecraft:deep_cold_ocean=minecraft:deepslate",
                                    "minecraft:deep_dark=minecraft:deepslate",
                                    "minecraft:deep_frozen_ocean=minecraft:deepslate",
                                    "minecraft:deep_lukewarm_ocean=minecraft:deepslate",
                                    "minecraft:deep_ocean=minecraft:deepslate",
                                    "minecraft:desert=minecraft:stone",
                                    "minecraft:dripstone_caves=minecraft:deepslate",
                                    "minecraft:eroded_badlands=minecraft:granite",
                                    "minecraft:flower_forest=minecraft:diorite",
                                    "minecraft:forest=minecraft:diorite",
                                    "minecraft:frozen_ocean=minecraft:andesite",
                                    "minecraft:frozen_peaks=minecraft:granite",
                                    "minecraft:frozen_river=minecraft:andesite",
                                    "minecraft:grove=minecraft:granite",
                                    "minecraft:ice_spikes=minecraft:deepslate",
                                    "minecraft:jagged_peaks=minecraft:granite",
                                    "minecraft:jungle=minecraft:diorite",
                                    "minecraft:lukewarm_ocean=minecraft:andesite",
                                    "minecraft:lush_caves=minecraft:diorite",
                                    "minecraft:mangrove_swamp=minecraft:deepslate",
                                    "minecraft:meadow=minecraft:stone",
                                    "minecraft:mushroom_fields=minecraft:stone",
                                    "minecraft:ocean=minecraft:andesite",
                                    "minecraft:old_growth_birch_forest=minecraft:diorite",
                                    "minecraft:old_growth_pine_taiga=minecraft:granite",
                                    "minecraft:old_growth_spruce_taiga=minecraft:granite",
                                    "minecraft:plains=minecraft:stone",
                                    "minecraft:river=minecraft:andesite",
                                    "minecraft:savanna=minecraft:stone",
                                    "minecraft:savanna_plateau=minecraft:granite",
                                    "minecraft:snowy_beach=minecraft:andesite",
                                    "minecraft:snowy_plains=minecraft:stone",
                                    "minecraft:snowy_slopes=minecraft:granite",
                                    "minecraft:snowy_taiga=minecraft:granite",
                                    "minecraft:sparse_jungle=minecraft:diorite",
                                    "minecraft:stony_peaks=minecraft:granite",
                                    "minecraft:stony_shore=minecraft:andesite",
                                    "minecraft:sunflower_plains=minecraft:stone",
                                    "minecraft:swamp=minecraft:deepslate",
                                    "minecraft:taiga=minecraft:granite",
                                    "minecraft:warm_ocean=minecraft:andesite",
                                    "minecraft:windswept_forest=minecraft:granite",
                                    "minecraft:windswept_gravelly_hills=minecraft:granite",
                                    "minecraft:windswept_hills=minecraft:granite",
                                    "minecraft:windswept_savanna=minecraft:stone",
                                    "minecraft:wooded_badlands=minecraft:granite",
                                    "*=minecraft:stone"),
                            () -> "*=minecraft:stone",
                            AsteroidConfig::isBiomeBlockEntry);
            asteroidResources = builder
                    .comment("Optional resource blocks in the form block_id=amount;y_min..y_max,y_min..y_max.",
                            "Amount is the chance per asteroid block, from 0.0 to 1.0.",
                            "Y ranges are inclusive; each resource can have its own comma-separated ranges.")
                    .defineList("asteroid_resources",
                            List.of("minecraft:coal_ore=0.025;-64..2031", "minecraft:iron_ore=0.0125;-64..2031",
                                    "minecraft:copper_ore=0.01;-64..2031"),
                            () -> "minecraft:coal_ore=0.01;-64..2031",
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
            String[] resourceParts = parts[1].split(";", 2);
            if (resourceParts.length != 2) {
                return false;
            }
            double amount = Double.parseDouble(resourceParts[0].trim());
            if (amount <= 0.0D || amount > 1.0D) {
                return false;
            }
            for (String range : resourceParts[1].split(",")) {
                String[] bounds = range.trim().split("\\.\\.", 2);
                if (bounds.length != 2) {
                    return false;
                }
                Integer.parseInt(bounds[0].trim());
                Integer.parseInt(bounds[1].trim());
            }
            return true;
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
