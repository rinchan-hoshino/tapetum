package dev.rinchan.tapetum.mixin.tlm;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.AnimatableEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.predicate.AnimationEvent;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.context.AnimationContext;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.AnimationProcessor;
import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.hygiene.FrameClock;
import dev.rinchan.tapetum.hygiene.RenderDistanceGuard;
import dev.rinchan.tapetum.vertex.IrisVertexBridge;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AnimationProcessor.class, remap = false)
public abstract class TlmAnimationFrameCacheMixin {
    @Unique
    private static final double SHADOW_ANIM_LOD_DIST_SQR = 12.0 * 12.0;

    @Shadow
    private boolean rendererDirty;

    @Shadow
    public boolean reloadAnimations;

    @Shadow
    private AnimatableEntity<?> animatable;

    @Unique
    private long tapetum$lastEvaluatedFrameId = -1L;

    @Unique
    private double tapetum$lastSeekTime = Double.NaN;

    @Inject(
            method =
                    "tickAnimation(DLcom/github/tartaricacid/touhoulittlemaid/geckolib3/core/event/predicate/AnimationEvent;Lcom/github/tartaricacid/touhoulittlemaid/geckolib3/core/molang/context/AnimationContext;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void tapetum$reuseSameFramePose(
            double seekTime,
            AnimationEvent<?> event,
            AnimationContext<?> context,
            CallbackInfoReturnable<Boolean> cir) {
        if (!TapetumConfig.isMaidAndBedrockFastRender()) {
            return;
        }
        long frameId = FrameClock.currentFrameId();
        if (!this.reloadAnimations && !this.rendererDirty && this.tapetum$lastEvaluatedFrameId >= 0L) {
            if (this.tapetum$lastEvaluatedFrameId == frameId
                    && Double.doubleToRawLongBits(seekTime) == Double.doubleToRawLongBits(this.tapetum$lastSeekTime)) {
                cir.setReturnValue(false);
                return;
            }
            long frameAge = frameId - this.tapetum$lastEvaluatedFrameId;
            if (frameAge >= 0L && frameAge <= 2L && TapetumConfig.isSmartShadowCulling() && IrisVertexBridge.isShadowPass()) {
                Entity entity = this.animatable != null ? this.animatable.getEntity() : null;
                if (entity != null && RenderDistanceGuard.isEntityBeyondCamera(entity, SHADOW_ANIM_LOD_DIST_SQR)) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
        this.tapetum$lastEvaluatedFrameId = frameId;
        this.tapetum$lastSeekTime = seekTime;
    }
}
