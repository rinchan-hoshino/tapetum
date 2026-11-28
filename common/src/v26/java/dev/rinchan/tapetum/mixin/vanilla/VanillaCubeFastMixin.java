package dev.rinchan.tapetum.mixin.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.geometry.BakedQuadMesh;
import dev.rinchan.tapetum.geometry.MeshCacheRegistry;
import dev.rinchan.tapetum.vertex.EntityVertexLayout;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import dev.rinchan.tapetum.vertex.IrisVertexBridge;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModelPart.Cube.class, priority = 900)
public abstract class VanillaCubeFastMixin {
    @Unique
    private BakedQuadMesh tapetum$bakedMesh;

    @Unique
    private int tapetum$bakedEpoch = -1;

    @Unique
    private boolean tapetum$baking;

    @Shadow
    public abstract void compile(
            PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, int color);

    @Inject(
            method =
                    "compile(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD"),
            cancellable = true)
    private void tapetum$compile54B(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            int color,
            CallbackInfo ci) {
        if (tapetum$baking
                || !TapetumConfig.isVanillaModelPart54B()
                || !IrisVertexBridge.isShaderPackInUse()
                || !FastEntityVertexWriter.canWrite54B(buffer)) {
            return;
        }
        int epoch = MeshCacheRegistry.reloadEpoch();
        BakedQuadMesh mesh = this.tapetum$bakedMesh;
        if (mesh == null || this.tapetum$bakedEpoch != epoch) {
            this.tapetum$baking = true;
            try {
                BakedQuadMesh.Builder builder = BakedQuadMesh.plainBuilder();
                this.compile(new PoseStack().last(), builder, packedLight, packedOverlay, -1);
                mesh = builder.build();
                this.tapetum$bakedMesh = mesh;
                this.tapetum$bakedEpoch = epoch;
            } finally {
                this.tapetum$baking = false;
            }
        }
        if (mesh.isEmpty()) {
            ci.cancel();
            return;
        }
        FastEntityVertexWriter.emitQuads(
                mesh, pose, buffer, packedLight, packedOverlay, EntityVertexLayout.argbToAbgr(color));
        ci.cancel();
    }
}
