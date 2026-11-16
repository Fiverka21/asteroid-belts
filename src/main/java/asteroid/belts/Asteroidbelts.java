package asteroid.belts;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Asteroidbelts.MODID)
public class Asteroidbelts {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "asteroidbelts";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(BuiltInRegistries.FEATURE, MODID);
    public static final DeferredHolder<Feature<?>, AsteroidFeature> ASTEROID_FEATURE = FEATURES.register(
            "asteroid", () -> new AsteroidFeature(NoneFeatureConfiguration.CODEC));

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Asteroidbelts(IEventBus modEventBus, ModContainer modContainer) {
        FEATURES.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, AsteroidConfig.SERVER_SPEC);
        NeoForge.EVENT_BUS.addListener(Asteroidbelts::onChunkLoad);
    }

    private static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.dimension() == net.minecraft.world.level.Level.OVERWORLD
                && AsteroidConfig.SERVER.voidBelowAsteroids.get()) {
            AsteroidFeature.clearBelowAsteroids(event.getChunk());
        }
    }
}
