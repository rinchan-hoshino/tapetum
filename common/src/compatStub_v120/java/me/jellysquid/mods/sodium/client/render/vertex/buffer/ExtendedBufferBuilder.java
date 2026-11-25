package me.jellysquid.mods.sodium.client.render.vertex.buffer;

import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;

public interface ExtendedBufferBuilder extends VertexBufferWriter {
    VertexFormatDescription sodium$getFormatDescription();
}
