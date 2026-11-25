package dev.rinchan.tapetum.mixin.tlm;

import com.github.ysbbbbbb.kaleidoscopedoll.entity.DollEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.geometry.DollBlockMeshCache;
import dev.rinchan.tapetum.shadow.ShadowCullingPolicy;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "com.github.ysbbbbbb.kaleidoscopedoll.client.render.DollEntityRender",
        remap = false)
public abstract class DollEntityRenderFastMixin {
    @Unique
    private static int tapetum$currentPackedLight = LightTexture.FULL_BRIGHT;

    @Inject(
            method =
                    "render(Lcom/github/ysbbbbbb/kaleidoscopedoll/entity/DollEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void tapetum$captureDollLightAndSkipShadow(
            DollEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo ci) {
        if (ShadowCullingPolicy.shouldCullDecorativeEntityShadow(entity)) {
            ci.cancel();
            return;
        }
        tapetum$currentPackedLight = packedLight;
    }

    @Inject(
            method =
                    "renderBlock(Lcom/github/ysbbbbbb/kaleidoscopedoll/entity/DollEntity;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void tapetum$renderBlockFast(
            DollEntity entity,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            CallbackInfo ci) {
        if (TapetumConfig.isMaidAndBedrockFastRender()
                && DollBlockMeshCache.tryRenderFast(entity, poseStack, bufferSource, state, tapetum$currentPackedLight)) {
            ci.cancel();
        }
    }
}
