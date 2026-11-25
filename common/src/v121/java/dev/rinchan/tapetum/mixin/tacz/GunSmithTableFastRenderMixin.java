package dev.rinchan.tapetum.mixin.tacz;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tacz.guns.block.AbstractGunSmithTableBlock;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.UnshadedVertexConsumer;
import com.tacz.guns.client.renderer.block.GunSmithTableRenderer;
import com.tacz.guns.client.resource.index.ClientBlockIndex;
import com.tacz.guns.config.client.RenderConfig;
import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.geometry.TaCZStaticModelBaker;
import dev.rinchan.tapetum.hygiene.RenderDistanceGuard;
import dev.rinchan.tapetum.shadow.ShadowCullingPolicy;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import java.util.Optional;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GunSmithTableRenderer.class, remap = false)
public abstract class GunSmithTableFastRenderMixin {
    @Shadow
    public abstract Optional<ClientBlockIndex> getIndex(GunSmithTableBlockEntity blockEntity);

    @Inject(
            method =
                    "render(Lcom/tacz/guns/block/entity/GunSmithTableBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"),
            cancellable = true)
    private void tapetum$renderBakedTable(
            GunSmithTableBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int combinedLight,
            int combinedOverlay,
            CallbackInfo ci) {
        if (blockEntity == null) {
            ci.cancel();
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        if (ShadowCullingPolicy.shouldCullTaCZBlockShadow(pos)) {
            ci.cancel();
            return;
        }
        if (!TapetumConfig.isTaczFastRender()) {
            return;
        }
        if (RenderDistanceGuard.isBlockBeyondCamera(pos, ShadowCullingPolicy.GUNSMITH_VIEW_DIST_SQR)) {
            ci.cancel();
            return;
        }
        if (bufferSource == null) {
            return;
        }
        Optional<ClientBlockIndex> optIndex = this.getIndex(blockEntity);
        if (optIndex.isEmpty()) {
            ci.cancel();
            return;
        }
        ClientBlockIndex index = optIndex.get();
        BedrockModel model = index.getModel();
        ResourceLocation texture = index.getTexture();
        if (model == null || texture == null) {
            ci.cancel();
            return;
        }
        BlockState blockState = blockEntity.getBlockState();
        if (!(blockState.getBlock() instanceof AbstractGunSmithTableBlock tableBlock)
                || !tableBlock.isRoot(blockState)) {
            ci.cancel();
            return;
        }
        TaCZStaticModelBaker.BakedTableModel baked = TaCZStaticModelBaker.getOrBake(model);
        if (baked.isEmpty()) {
            ci.cancel();
            return;
        }

        Direction facing = blockState.getValue(AbstractGunSmithTableBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.ZN.rotationDegrees(180.0f));
        poseStack.mulPose(Axis.YN.rotationDegrees(tableBlock.parseRotation(facing)));

        RenderType renderType = tapetum$useTranslucentBlockEntity()
                ? RenderType.entityTranslucent(texture)
                : RenderType.entityCutout(texture);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        PoseStack.Pose last = poseStack.last();

        if (!baked.ordinaryMesh().isEmpty()) {
            FastEntityVertexWriter.emitQuads(
                    baked.ordinaryMesh(), last, consumer, combinedLight, combinedOverlay, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        if (!baked.illuminatedMesh().isEmpty()) {
            VertexConsumer unshaded = UnshadedVertexConsumer.wrap(consumer);
            FastEntityVertexWriter.emitQuads(
                    baked.illuminatedMesh(),
                    last,
                    unshaded,
                    LightTexture.pack(15, 15),
                    combinedOverlay,
                    1.0f,
                    1.0f,
                    1.0f,
                    1.0f);
        }

        poseStack.popPose();
        ci.cancel();
    }

    @Inject(
            method = "shouldRenderOffScreen(Lcom/tacz/guns/block/entity/GunSmithTableBlockEntity;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void tapetum$enableFrustumCulling(
            GunSmithTableBlockEntity blockEntity, CallbackInfoReturnable<Boolean> cir) {
        if (TapetumConfig.isTaczFastRender()) {
            cir.setReturnValue(false);
        }
    }

    public int getViewDistance() {
        return (int) ShadowCullingPolicy.GUNSMITH_VIEW_DIST;
    }

    @org.spongepowered.asm.mixin.Unique
    private static final java.util.function.Supplier<?> TAPETUM$TRANSLUCENT_SUPPLIER = tapetum$resolveTranslucentSupplier();

    @org.spongepowered.asm.mixin.Unique
    private static java.util.function.Supplier<?> tapetum$resolveTranslucentSupplier() {
        try {
            Object fieldVal = RenderConfig.class.getField("BLOCK_ENTITY_TRANSLUCENT").get(null);
            if (fieldVal instanceof java.util.function.Supplier<?> supplier) {
                return supplier;
            }
        } catch (Throwable ignored) {
        }
        return () -> Boolean.FALSE;
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean tapetum$useTranslucentBlockEntity() {
        return Boolean.TRUE.equals(TAPETUM$TRANSLUCENT_SUPPLIER.get());
    }
}
