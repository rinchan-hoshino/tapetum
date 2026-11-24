package dev.rinchan.tapetum.mixin.tacz;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.compat.iris.IrisCompat;
import dev.rinchan.tapetum.config.TapetumConfig;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BedrockModel.class, remap = false)
public abstract class TaCZBedrockModelBatchMixin {
    @Shadow
    protected List<IFunctionalRenderer> delegateRenderers;

    @Shadow
    protected IFunctionalRenderer particleWindowRenderer;

    @Unique
    private ItemDisplayContext tapetum$activeTransformType;

    @Inject(
            method =
                    "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IIFFFFLnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At("HEAD"))
    private void tapetum$captureTransformType(
            PoseStack poseStack,
            ItemDisplayContext transformType,
            RenderType renderType,
            int light,
            int overlay,
            float red,
            float green,
            float blue,
            float alpha,
            MultiBufferSource bufferSource,
            CallbackInfo ci) {
        this.tapetum$activeTransformType = transformType;
    }

    @Inject(
            method =
                    "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IIFFFFLnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At("RETURN"))
    private void tapetum$clearTransformType(
            PoseStack poseStack,
            ItemDisplayContext transformType,
            RenderType renderType,
            int light,
            int overlay,
            float red,
            float green,
            float blue,
            float alpha,
            MultiBufferSource bufferSource,
            CallbackInfo ci) {
        this.tapetum$activeTransformType = null;
    }

    @Redirect(
            method =
                    "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IIFFFFLnet/minecraft/client/renderer/MultiBufferSource;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/tacz/guns/compat/iris/IrisCompat;endBatch(Lnet/minecraft/client/renderer/MultiBufferSource;)Z",
                            ordinal = 0))
    private boolean tapetum$preserveBatching(MultiBufferSource bufferSource) {
        if (!TapetumConfig.isTaczFastRender()
                || this.particleWindowRenderer != null
                || (this.delegateRenderers != null && !this.delegateRenderers.isEmpty())
                || (this.tapetum$activeTransformType != null && this.tapetum$activeTransformType.firstPerson())) {
            return IrisCompat.endBatch(bufferSource);
        }
        return true;
    }
}
