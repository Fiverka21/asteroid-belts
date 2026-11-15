package asteroid.belts.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;

@Mixin(LightEngine.class)
public interface LightEngineAccessor {
    @Accessor("chunkSource")
    LightChunkGetter asteroidbelts$getChunkSource();
}
