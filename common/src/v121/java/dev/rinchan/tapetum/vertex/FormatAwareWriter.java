package dev.rinchan.tapetum.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * Implemented by fast-path {@code VertexConsumer} wrappers (such as {@code VertexMultiConsumer$Double},
 * {@code LightPassVertexRouter}, and {@code UnshadedVertexConsumer}) so {@link FastEntityVertexWriter}
 * can query the underlying {@code BufferBuilder}'s exact {@link VertexFormat} in O(1).
 */
public interface FormatAwareWriter {
    VertexFormat tapetum$getTargetFormat();
}
