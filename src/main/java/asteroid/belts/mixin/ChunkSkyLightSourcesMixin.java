package asteroid.belts.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;

@Mixin(ChunkSkyLightSources.class)
public abstract class ChunkSkyLightSourcesMixin {
    private static final int SKY_LIGHT_CUTOFF_Y = 300;

    @Inject(method = "isEdgeOccluded", at = @At("HEAD"), cancellable = true)
    private static void asteroidbelts$ignoreHighSkyBlockOpacity(BlockGetter level, BlockPos upperPos, BlockState upperState,
            BlockPos lowerPos, BlockState lowerState, CallbackInfoReturnable<Boolean> callback) {
        if (upperPos.getY() > SKY_LIGHT_CUTOFF_Y || lowerPos.getY() > SKY_LIGHT_CUTOFF_Y) {
            callback.setReturnValue(false);
        }
    }
}
