package net.caffeinemc.mods.sodium.api.vertex.buffer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.system.MemoryStack;

public interface VertexBufferWriter {
    static VertexBufferWriter of(VertexConsumer consumer) { throw new UnsupportedOperationException(); }
    static VertexBufferWriter tryOf(VertexConsumer consumer) { throw new UnsupportedOperationException(); }
    void push(MemoryStack stack, long ptr, int count, VertexFormat format);
    default boolean canUseIntrinsics() { return true; }
    static void copyInto(VertexBufferWriter writer, MemoryStack stack, long ptr, int count, VertexFormat format) {
        throw new UnsupportedOperationException();
    }
}
