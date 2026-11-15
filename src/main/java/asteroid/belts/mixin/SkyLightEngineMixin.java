package asteroid.belts.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.SkyLightEngine;

@Mixin(SkyLightEngine.class)
public abstract class SkyLightEngineMixin {
    private static final int SKY_LIGHT_CUTOFF_Y = 300;

    @Unique
    private long asteroidbelts$propagationSource = Long.MIN_VALUE;

    @Inject(method = "propagateIncrease", at = @At("HEAD"))
    private void asteroidbelts$beginDownwardLightContext(long sourcePos, long flags, int level, CallbackInfo callback) {
        asteroidbelts$propagationSource = sourcePos;
    }

    @Inject(method = "propagateIncrease", at = @At("RETURN"))
    private void asteroidbelts$endDownwardLightContext(long sourcePos, long flags, int level, CallbackInfo callback) {
        asteroidbelts$propagationSource = Long.MIN_VALUE;
    }

    @Redirect(method = "propagateIncrease", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/lighting/SkyLightEngine;getOpacity(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I"))
    private int asteroidbelts$allowOpacityFreeDownwardTarget(SkyLightEngine engine, BlockState state, BlockPos targetPos) {
        if (asteroidbelts$propagationSource != Long.MIN_VALUE
                && targetPos.getY() > SKY_LIGHT_CUTOFF_Y
                && targetPos.getY() < BlockPos.getY(asteroidbelts$propagationSource)) {
            return 0;
        }
        LightEngineAccessor accessor = (LightEngineAccessor) (Object) engine;
        return state.getLightBlock(accessor.asteroidbelts$getChunkSource().getLevel(), targetPos);
    }
}
