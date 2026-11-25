package com.tacz.guns.client.model.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.world.item.ItemDisplayContext;

public class BedrockPart {
    public ObjectList<BedrockCube> cubes;
    public ObjectList<BedrockPart> children;
    public boolean visible = true;
    public boolean illuminated;
    public void translateAndRotateAndScale(PoseStack poseStack) {}
    public void compile(PoseStack.Pose pose, VertexConsumer consumer, int texU, int texV, float r, float g, float b, float a) {}
    public void render(PoseStack poseStack, ItemDisplayContext transformType, VertexConsumer consumer, int light, int overlay) {}
}
