package dev.rinchan.tapetum.mixin.shadow;

import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.chatbubble.ChatBubbleRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.chatbubble.EntityGraphics;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.geckolayer.GeckoLayerMaidBackItem;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.geckolayer.GeckoLayerMaidBanner;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.geckolayer.GeckoLayerMaidBipedHead;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.geckolayer.GeckoLayerMaidHeld;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.rinchan.tapetum.shadow.ShadowCullingPolicy;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class TlmShadowContractMixin {
    private TlmShadowContractMixin() {}

    @Mixin(value = ChatBubbleRenderer.class, remap = false)
    public abstract static class ChatBubble {
        @Inject(
                method =
                        "render(Lcom/github/tartaricacid/touhoulittlemaid/client/renderer/entity/chatbubble/EntityGraphics;)V",
                at = @At("HEAD"),
                cancellable = true)
        private void tapetum$skipChatBubbleShadow(EntityGraphics graphics, CallbackInfo ci) {
            if (ShadowCullingPolicy.shouldCullTlmDecorationShadow()) {
                ci.cancel();
            }
        }
    }

    @Mixin(
            value = {
                GeckoLayerMaidHeld.class,
                GeckoLayerMaidBackItem.class,
                GeckoLayerMaidBanner.class,
                GeckoLayerMaidBipedHead.class
            },
            remap = false)
    public abstract static class MaidDetailLayers {
        @Inject(
                method =
                        "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Mob;FFFFFF)V",
                at = @At("HEAD"),
                cancellable = true)
        private void tapetum$skipDetailLayerShadow(
                PoseStack poseStack,
                MultiBufferSource bufferSource,
                int packedLight,
                Mob entity,
                float limbSwing,
                float limbSwingAmount,
                float partialTicks,
                float ageInTicks,
                float netHeadYaw,
                float headPitch,
                CallbackInfo ci) {
            if (ShadowCullingPolicy.shouldCullTlmDecorationShadow()) {
                ci.cancel();
            }
        }
    }

    @Mixin(value = EntityMaidRenderer.class, remap = false)
    public abstract static class MaidBody {
        @Inject(
                method =
                        "render(Lnet/minecraft/world/entity/Mob;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                at = @At("HEAD"),
                cancellable = true)
        private void tapetum$cullDistantMaidShadow(
                Mob entity,
                float entityYaw,
                float partialTicks,
                PoseStack poseStack,
                MultiBufferSource bufferSource,
                int packedLight,
                CallbackInfo ci) {
            if (ShadowCullingPolicy.shouldCullTlmMaidBodyShadow(entity)) {
                ci.cancel();
            }
        }
    }
}
