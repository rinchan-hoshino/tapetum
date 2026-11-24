package dev.rinchan.tapetum.mixin.tlm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.geometry.BakedQuadMesh;
import dev.rinchan.tapetum.geometry.MeshCacheRegistry;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart",
        remap = false)
public abstract class SbmBedrockPartFastMixin {
    @Shadow
    public ObjectList<?> cubes;

    @Shadow
    private void compile(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int texU,
            int texV,
            float red,
            float green,
            float blue,
            float alpha) {}

    @Unique
    private BakedQuadMesh tapetum$bakedPart;

    @Unique
    private int tapetum$bakedCubeCount = -1;

    @Unique
    private int tapetum$bakedEpoch = -1;

    @Unique
    private boolean tapetum$baking;

    @Inject(
            method =
                    "compile(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At("HEAD"),
            cancellable = true)
    private void tapetum$compileBaked(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int texU,
            int texV,
            float red,
            float green,
            float blue,
            float alpha,
            CallbackInfo ci) {
        if (this.tapetum$baking || !TapetumConfig.isMaidAndBedrockFastRender()) {
            return;
        }
        int size = this.cubes == null ? 0 : this.cubes.size();
        if (size == 0) {
            ci.cancel();
            return;
        }
        int epoch = MeshCacheRegistry.reloadEpoch();
        BakedQuadMesh mesh = this.tapetum$bakedPart;
        if (mesh == null || this.tapetum$bakedCubeCount != size || this.tapetum$bakedEpoch != epoch) {
            BakedQuadMesh.Builder builder = BakedQuadMesh.builder();
            this.tapetum$baking = true;
            try {
                this.compile(new PoseStack().last(), builder, 0, 0, 1.0f, 1.0f, 1.0f, 1.0f);
            } finally {
                this.tapetum$baking = false;
            }
            mesh = builder.build();
            this.tapetum$bakedPart = mesh;
            this.tapetum$bakedCubeCount = size;
            this.tapetum$bakedEpoch = epoch;
        }
        if (!mesh.isEmpty()) {
            FastEntityVertexWriter.emitQuads(mesh, pose, consumer, texU, texV, red, green, blue, alpha);
        }
        ci.cancel();
    }
}
