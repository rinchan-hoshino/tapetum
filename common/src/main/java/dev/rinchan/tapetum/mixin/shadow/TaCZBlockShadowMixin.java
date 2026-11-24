package dev.rinchan.tapetum.mixin.shadow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.block.GunDisplayCaseRenderer;
import com.tacz.guns.client.renderer.block.GunPegboardRenderer;
import com.tacz.guns.client.renderer.block.StatueRenderer;
import com.tacz.guns.client.renderer.block.TargetRenderer;
import dev.rinchan.tapetum.shadow.ShadowCullingPolicy;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class TaCZBlockShadowMixin {
    private TaCZBlockShadowMixin() {}

    @Mixin(
            value = {GunDisplayCaseRenderer.class, GunPegboardRenderer.class},
            remap = false)
    public abstract static class DisplayBlocks {
        @Inject(
                method =
                        "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
                at = @At("HEAD"),
                cancellable = true)
        private void tapetum$skipDisplayBlockShadow(
                BlockEntity blockEntity,
                float partialTick,
                PoseStack poseStack,
                MultiBufferSource bufferSource,
                int packedLight,
                int packedOverlay,
                CallbackInfo ci) {
            if (ShadowCullingPolicy.shouldCullTaCZShowcaseShadow()) {
                ci.cancel();
            }
        }
    }

    @Mixin(
            value = {StatueRenderer.class, TargetRenderer.class},
            remap = false)
    public abstract static class StaticBlocks {
        @Inject(
                method =
                        "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
                at = @At("HEAD"),
                cancellable = true)
        private void tapetum$cullStaticBlockShadow(
                BlockEntity blockEntity,
                float partialTick,
                PoseStack poseStack,
                MultiBufferSource bufferSource,
                int packedLight,
                int packedOverlay,
                CallbackInfo ci) {
            if (blockEntity != null && ShadowCullingPolicy.shouldCullTaCZBlockShadow(blockEntity.getBlockPos())) {
                ci.cancel();
            }
        }
    }
}
