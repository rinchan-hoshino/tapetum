package dev.rinchan.tapetum.mixin.shadow;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.rinchan.tapetum.shadow.ShadowCullingPolicy;
import mods.flammpfeil.slashblade.client.renderer.entity.BladeStandEntityRenderer;
import mods.flammpfeil.slashblade.client.renderer.layers.LayerSlashBlade;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.entity.BladeStandEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class SlashBladeShadowMixin {
    private SlashBladeShadowMixin() {}

    @Mixin(value = BladeRenderState.class, remap = false)
    public abstract static class Luminous {
        @Inject(
                method = {
                    "renderOverridedLuminous(Lnet/minecraft/world/item/ItemStack;Lmods/flammpfeil/slashblade/client/renderer/model/obj/WavefrontObject;Ljava/lang/String;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                    "renderOverridedLuminousDepthWrite(Lnet/minecraft/world/item/ItemStack;Lmods/flammpfeil/slashblade/client/renderer/model/obj/WavefrontObject;Ljava/lang/String;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                    "renderOverridedReverseLuminous(Lnet/minecraft/world/item/ItemStack;Lmods/flammpfeil/slashblade/client/renderer/model/obj/WavefrontObject;Ljava/lang/String;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
                },
                at = @At("HEAD"),
                cancellable = true)
        private static void tapetum$skipLuminousShadow(
                ItemStack itemStack,
                WavefrontObject model,
                String target,
                ResourceLocation texture,
                PoseStack matrixStack,
                MultiBufferSource bufferSource,
                int packedLight,
                CallbackInfo ci) {
            if (ShadowCullingPolicy.shouldCullSlashBladeLuminousShadow()) {
                ci.cancel();
            }
        }
    }

    @Mixin(value = BladeStandEntityRenderer.class, remap = false)
    public abstract static class BladeStand {
        @Inject(
                method =
                        "render(Lmods/flammpfeil/slashblade/entity/BladeStandEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                at = @At("HEAD"),
                cancellable = true)
        private void tapetum$cullDistantBladeStandShadow(
                BladeStandEntity entity,
                float entityYaw,
                float partialTicks,
                PoseStack poseStack,
                MultiBufferSource bufferSource,
                int packedLight,
                CallbackInfo ci) {
            if (ShadowCullingPolicy.shouldCullSlashBladeStandShadow(entity)) {
                ci.cancel();
            }
        }
    }

    @Mixin(value = LayerSlashBlade.class, remap = false)
    public abstract static class HotbarBlade {
        @Inject(
                method =
                        "renderHotbarItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;)V",
                at = @At("HEAD"),
                cancellable = true)
        private void tapetum$skipHotbarBladeShadow(
                PoseStack poseStack,
                MultiBufferSource bufferSource,
                int packedLight,
                LivingEntity entity,
                CallbackInfo ci) {
            if (ShadowCullingPolicy.shouldCullSlashBladeHotbarShadow()) {
                ci.cancel();
            }
        }
    }
}
