package dev.rinchan.tapetum.vertex;

import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;

public interface FormatAwareWriter {
    VertexFormatDescription tapetum$getTargetFormatDesc();
}
