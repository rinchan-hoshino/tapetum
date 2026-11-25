package com.tacz.guns.client.model.bedrock;

import com.mojang.blaze3d.vertex.VertexConsumer;

public abstract class UnshadedVertexConsumer implements VertexConsumer {
    protected VertexConsumer delegate;
    protected float nx, ny, nz;

    public static VertexConsumer wrap(VertexConsumer delegate) {
        return delegate;
    }
}
