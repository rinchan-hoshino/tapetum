package com.tacz.guns.client.model.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.world.item.ItemDisplayContext;

public class BedrockModel {
    protected List<BedrockPart> shouldRender;
    public List<BedrockPart> getShouldRender() { return shouldRender; }
    public void render(PoseStack matrixStack, ItemDisplayContext transformType, com.mojang.blaze3d.vertex.VertexConsumer consumer, int light, int overlay) {}
    public void render(PoseStack matrixStack, ItemDisplayContext transformType, net.minecraft.client.renderer.RenderType renderType, int light, int overlay) {}
}
