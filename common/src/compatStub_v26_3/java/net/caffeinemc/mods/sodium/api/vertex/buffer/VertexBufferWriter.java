package net.caffeinemc.mods.sodium.api.vertex.buffer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import org.lwjgl.system.MemoryStack;

public interface VertexBufferWriter {
    static VertexBufferWriter sodium$of(VertexConsumer consumer) { throw new UnsupportedOperationException(); }
    static VertexBufferWriter sodium$tryOf(VertexConsumer consumer) { throw new UnsupportedOperationException(); }
    void sodium$push(MemoryStack stack, long ptr, int count, VertexFormat format);
    default boolean sodium$canUseIntrinsics() { return true; }
    static void sodium$copyInto(VertexBufferWriter writer, MemoryStack stack, long ptr, int count, VertexFormat format) {
        throw new UnsupportedOperationException();
    }
}
