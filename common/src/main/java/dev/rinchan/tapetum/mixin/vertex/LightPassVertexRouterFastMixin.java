package dev.rinchan.tapetum.mixin.vertex;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.rinchan.tapetum.vertex.EntityVertexLayout;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import dev.rinchan.tapetum.vertex.FormatAwareWriter;
import dev.rinchan.tapetum.vertex.IrisVertexBridge;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Implements Sodium's {@link VertexBufferWriter} on SimpleBedrockModel v2's
 * {@code LightPassVertexRouter} so TaCZ v2 meshes stop spamming Sodium's bad-consumer warning
 * and route contiguous ordinary/emissive primitive runs in O(1) without per-vertex overhead.
 */
@Pseudo
@Mixin(
        targets = "com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.capture.LightPassVertexRouter",
        remap = false)
public abstract class LightPassVertexRouterFastMixin
        implements VertexConsumer, VertexBufferWriter, FormatAwareWriter {
    @Shadow
    private VertexConsumer ordinary;

    @Shadow
    private VertexConsumer emissive;

    @Shadow
    private int primitiveSize;

    @Shadow
    private int primitiveVertices;

    @Shadow
    private boolean pending;

    @Shadow
    private void flush() {}

    @Override
    public boolean canUseIntrinsics() {
        return VertexBufferWriter.tryOf(this.ordinary) != null
                && VertexBufferWriter.tryOf(this.emissive) != null;
    }

    @Override
    public VertexFormat tapetum$getTargetFormat() {
        VertexFormat a = FastEntityVertexWriter.extractFormat(this.ordinary);
        if (a == null) {
            return null;
        }
        VertexFormat b = FastEntityVertexWriter.extractFormat(this.emissive);
        return a == b ? a : null;
    }

    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormat format) {
        if (this.pending) {
            this.flush();
        }
        if (count <= 0) {
            return;
        }
        VertexBufferWriter ordWriter = VertexBufferWriter.tryOf(this.ordinary);
        VertexBufferWriter emiWriter = VertexBufferWriter.tryOf(this.emissive);
        VertexFormat irisFmt = IrisVertexBridge.getVerifiedEntityFormat();
        int stride = -1;
        if (format == EntityVertex.FORMAT) {
            stride = EntityVertexLayout.STRIDE_36B;
        } else if (irisFmt != null && format == irisFmt) {
            stride = EntityVertexLayout.STRIDE_54B;
        }

        if (ordWriter != null
                && emiWriter != null
                && stride > 0
                && this.primitiveVertices == 0
                && (this.primitiveSize == 4 || this.primitiveSize == 3)
                && (count % this.primitiveSize == 0)) {
            FastEntityVertexWriter.routeLightPassBuffer(
                    ordWriter, emiWriter, this.primitiveSize, stack, ptr, count, format, stride);
            return;
        }

        int vertexSize = format.getVertexSize();
        for (int i = 0; i < count; i++) {
            long vPtr = ptr + (long) i * vertexSize;
            float vx = MemoryUtil.memGetFloat(vPtr);
            float vy = MemoryUtil.memGetFloat(vPtr + 4L);
            float vz = MemoryUtil.memGetFloat(vPtr + 8L);
            int color = MemoryUtil.memGetInt(vPtr + 12L);
            float u = MemoryUtil.memGetFloat(vPtr + 16L);
            float v = MemoryUtil.memGetFloat(vPtr + 20L);
            int overlay = MemoryUtil.memGetInt(vPtr + 24L);
            int light = MemoryUtil.memGetInt(vPtr + 28L);
            int norm = MemoryUtil.memGetInt(vPtr + 32L);
            this.addVertex(vx, vy, vz)
                    .setColor(color & 0xFF, (color >>> 8) & 0xFF, (color >>> 16) & 0xFF, (color >>> 24) & 0xFF)
                    .setUv(u, v)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(
                            EntityVertexLayout.unpackNormX(norm),
                            EntityVertexLayout.unpackNormY(norm),
                            EntityVertexLayout.unpackNormZ(norm));
        }
        if (this.pending) {
            this.flush();
        }
    }
}
