package asteroid.belts.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.SkyLightEngine;

@Mixin(LightEngine.class)
public abstract class LightEngineMixin {
    private static final int SKY_LIGHT_CUTOFF_Y = 300;

    @Inject(method = "shapeOccludes", at = @At("HEAD"), cancellable = true)
    private void asteroidbelts$allowDownwardSkyLight(long sourcePos, BlockState sourceState, long targetPos,
            BlockState targetState, Direction direction, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof SkyLightEngine && direction == Direction.DOWN
                && (BlockPos.getY(sourcePos) > SKY_LIGHT_CUTOFF_Y || BlockPos.getY(targetPos) > SKY_LIGHT_CUTOFF_Y)) {
            callback.setReturnValue(false);
        }
    }
}
