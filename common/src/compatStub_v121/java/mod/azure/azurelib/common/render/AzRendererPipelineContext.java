package mod.azure.azurelib.common.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class AzRendererPipelineContext<K, T> {
    public PoseStack poseStack() { return null; }
    public VertexConsumer vertexConsumer() { return null; }
    public int packedLight() { return 0; }
    public int packedOverlay() { return 0; }
    public int renderColor() { return -1; }
    public AzQuadFilter quadFilter() { return null; }
}
