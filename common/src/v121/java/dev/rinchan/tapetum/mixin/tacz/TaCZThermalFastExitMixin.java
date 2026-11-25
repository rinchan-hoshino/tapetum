package dev.rinchan.tapetum.mixin.tacz;

import com.tacz.guns.client.renderer.scope.ScopeThermalRenderer;
import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.hygiene.TaCZThermalFastState;
import dev.rinchan.tapetum.vertex.IrisVertexBridge;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ScopeThermalRenderer.class, remap = false)
public abstract class TaCZThermalFastExitMixin {
    @Shadow
    public static boolean enabled() {
        return false;
    }

    @Inject(method = "beginFrame()V", at = @At("TAIL"))
    private static void tapetum$refreshThermalState(CallbackInfo ci) {
        if (TapetumConfig.isTaczFastRender()) {
            TaCZThermalFastState.refresh(enabled());
        }
    }

    @Inject(
            method =
                    "wrap(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/MultiBufferSource;",
            at = @At("HEAD"),
            cancellable = true)
    private static void tapetum$fastExitWrap(
            MultiBufferSource bufferSource, Entity entity, CallbackInfoReturnable<MultiBufferSource> cir) {
        if (!TapetumConfig.isTaczFastRender()) {
            return;
        }
        if (!TaCZThermalFastState.activeThisFrame || IrisVertexBridge.isShadowPass()) {
            cir.setReturnValue(bufferSource);
        }
    }
}
