package dev.rinchan.tapetum.mixin.tacz;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.model.bedrock.BedrockCube;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.geometry.BakedQuadMesh;
import dev.rinchan.tapetum.geometry.MeshCacheRegistry;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BedrockPart.class, remap = false)
public abstract class TaCZBedrockPartFastMixin {
    @Shadow
    public ObjectList<BedrockCube> cubes;

    @Unique
    private BakedQuadMesh tapetum$bakedPart;

    @Unique
    private int tapetum$bakedCubeCount = -1;

    @Unique
    private int tapetum$bakedEpoch = -1;

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
        if (!TapetumConfig.isTaczFastRender()) {
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
            PoseStack.Pose identity = new PoseStack().last();
            for (int i = 0; i < size; i++) {
                BedrockCube cube = this.cubes.get(i);
                if (cube != null) {
                    cube.compile(identity, builder, 0, 0, 1.0f, 1.0f, 1.0f, 1.0f);
                }
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
