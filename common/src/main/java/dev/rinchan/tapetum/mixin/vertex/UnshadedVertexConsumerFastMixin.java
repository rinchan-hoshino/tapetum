package dev.rinchan.tapetum.mixin.vertex;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import dev.rinchan.tapetum.vertex.FormatAwareWriter;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Implements Sodium's {@link VertexBufferWriter} on TaCZ's {@code UnshadedVertexConsumer} so
 * {@code _illuminated} model parts stay on the 54B/36B off-heap fast path without mutating caller
 * source buffers.
 */
@Pseudo
@Mixin(targets = "com.tacz.guns.client.model.bedrock.UnshadedVertexConsumer", remap = false)
public abstract class UnshadedVertexConsumerFastMixin implements VertexBufferWriter, FormatAwareWriter {
    @Shadow
    private VertexConsumer delegate;

    @Shadow
    private float normalX;

    @Shadow
    private float normalY;

    @Shadow
    private float normalZ;

    @Override
    public boolean canUseIntrinsics() {
        return VertexBufferWriter.tryOf(this.delegate) != null;
    }

    @Override
    public VertexFormat tapetum$getTargetFormat() {
        return FastEntityVertexWriter.extractFormat(this.delegate);
    }

    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormat format) {
        FastEntityVertexWriter.pushWithFixedNormal(
                this.delegate, stack, ptr, count, format, this.normalX, this.normalY, this.normalZ);
    }
}
