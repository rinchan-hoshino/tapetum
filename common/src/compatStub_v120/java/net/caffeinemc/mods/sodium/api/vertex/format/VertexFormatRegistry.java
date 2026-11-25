package net.caffeinemc.mods.sodium.api.vertex.format;

import com.mojang.blaze3d.vertex.VertexFormat;

public interface VertexFormatRegistry {
    static VertexFormatRegistry instance() { throw new UnsupportedOperationException(); }
    VertexFormatDescription get(VertexFormat format);
}
