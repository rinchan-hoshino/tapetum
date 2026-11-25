package dev.rinchan.tapetum.vertex;

import com.mojang.renderpearl.api.vertex.VertexFormat;

public interface FormatAwareWriter {
    VertexFormat tapetum$getTargetFormat();
}
