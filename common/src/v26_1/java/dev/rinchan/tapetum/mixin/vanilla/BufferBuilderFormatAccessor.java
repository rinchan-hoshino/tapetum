package dev.rinchan.tapetum.mixin.vanilla;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferBuilder.class)
public interface BufferBuilderFormatAccessor {
    @Accessor("format")
    VertexFormat tapetum$getFormat();
}
