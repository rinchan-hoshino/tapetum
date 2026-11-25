package dev.rinchan.tapetum.vertex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.rinchan.tapetum.geometry.BakedQuadMesh;
import dev.rinchan.tapetum.mixin.vanilla.BufferBuilderFormatAccessor;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class FastEntityVertexWriter {
    public enum Mode {
        FAST_54B,
        FAST_36B,
        FALLBACK_CONSUMER
    }

    public static final int SCRATCH_MAX_VERTS = 1020;
    private static final int SCRATCH_BYTES = SCRATCH_MAX_VERTS * EntityVertexLayout.STRIDE_54B;
    private static final boolean SODIUM_PRESENT = detectSodium();
    private static final ThreadLocal<ScratchBuffer> PRIMARY_SCRATCH = ThreadLocal.withInitial(ScratchBuffer::new);

    private FastEntityVertexWriter() {}

    public static boolean isSodiumPresent() {
        return SODIUM_PRESENT;
    }

    public static boolean canWrite54B(VertexConsumer consumer) {
        return resolveMode(consumer) == Mode.FAST_54B;
    }

    public static VertexFormat extractFormat(VertexConsumer consumer) {
        if (consumer == null || !SODIUM_PRESENT) {
            return null;
        }
        return SodiumAccess.extractFormat(consumer);
    }

    public static Mode resolveMode(VertexConsumer consumer) {
        if (consumer == null || !SODIUM_PRESENT) {
            return Mode.FALLBACK_CONSUMER;
        }
        return SodiumAccess.resolveMode(consumer);
    }

    public static void emitQuads(
            BakedQuadMesh mesh,
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int light,
            int overlay,
            int abgr) {
        if (mesh == null || mesh.isEmpty()) {
            return;
        }
        Mode mode = resolveMode(consumer);
        if (mode == Mode.FALLBACK_CONSUMER) {
            emitQuadsFallback(mesh, pose.pose(), pose.normal(), consumer, light, overlay, abgr);
            return;
        }
        SodiumAccess.emitQuadsFast(mesh, pose.pose(), pose.normal(), consumer, light, overlay, abgr, mode == Mode.FAST_54B);
    }

    public static int writeQuadsToBuffer(
            BakedQuadMesh mesh,
            int startQuad,
            int quadCount,
            Matrix4f poseMat,
            Matrix3f normalMat,
            int light,
            int overlay,
            int abgr,
            boolean format54B,
            short entityId,
            short blockEntityId,
            short itemId,
            long dstBasePtr) {
        float m00 = poseMat.m00(), m01 = poseMat.m01(), m02 = poseMat.m02();
        float m10 = poseMat.m10(), m11 = poseMat.m11(), m12 = poseMat.m12();
        float m20 = poseMat.m20(), m21 = poseMat.m21(), m22 = poseMat.m22();
        float m30 = poseMat.m30(), m31 = poseMat.m31(), m32 = poseMat.m32();

        float n00 = 1f, n01 = 0f, n02 = 0f;
        float n10 = 0f, n11 = 1f, n12 = 0f;
        float n20 = 0f, n21 = 0f, n22 = 1f;
        if (normalMat != null) {
            n00 = normalMat.m00(); n01 = normalMat.m01(); n02 = normalMat.m02();
            n10 = normalMat.m10(); n11 = normalMat.m11(); n12 = normalMat.m12();
            n20 = normalMat.m20(); n21 = normalMat.m21(); n22 = normalMat.m22();
        }

        float handednessSign = format54B ? (TangentMath.determinant3x3(poseMat) < 0.0f ? -1.0f : 1.0f) : 1.0f;

        float[] posArr = mesh.pos;
        float[] uvArr = mesh.uv;
        float[] midUvArr = mesh.midUv;
        float[] normArr = mesh.normal;
        float[] tanArr = mesh.tangent;
        byte[] tanWArr = mesh.tangentW;

        long stride = format54B ? EntityVertexLayout.STRIDE_54B : EntityVertexLayout.STRIDE_36B;
        long ptr = dstBasePtr;
        int endQuad = startQuad + quadCount;

        for (int q = startQuad; q < endQuad; q++) {
            int nBase = q * 3;
            float lnx = normArr[nBase], lny = normArr[nBase + 1], lnz = normArr[nBase + 2];
            float wnx = n00 * lnx + n10 * lny + n20 * lnz;
            float wny = n01 * lnx + n11 * lny + n21 * lnz;
            float wnz = n02 * lnx + n12 * lny + n22 * lnz;
            float lenSq = wnx * wnx + wny * wny + wnz * wnz;
            if (lenSq > 1e-12f && Math.abs(lenSq - 1.0f) > 1e-4f) {
                float inv = (float) (1.0 / Math.sqrt(lenSq));
                wnx *= inv; wny *= inv; wnz *= inv;
            }
            int packedNormal = EntityVertexLayout.packNormI8(wnx, wny, wnz);

            float midU = 0f, midV = 0f;
            int packedTangent = 0;
            if (format54B) {
                int mBase = q * 2;
                midU = midUvArr[mBase];
                midV = midUvArr[mBase + 1];
                packedTangent = TangentMath.transformAndPackTangent(
                        poseMat, tanArr[nBase], tanArr[nBase + 1], tanArr[nBase + 2], tanWArr[q], handednessSign);
            }

            int pBase = q * 12;
            int uvBase = q * 8;
            for (int v = 0; v < 4; v++) {
                float lx = posArr[pBase + v * 3];
                float ly = posArr[pBase + v * 3 + 1];
                float lz = posArr[pBase + v * 3 + 2];
                float wx = m00 * lx + m10 * ly + m20 * lz + m30;
                float wy = m01 * lx + m11 * ly + m21 * lz + m31;
                float wz = m02 * lx + m12 * ly + m22 * lz + m32;

                MemoryUtil.memPutFloat(ptr, wx);
                MemoryUtil.memPutFloat(ptr + 4L, wy);
                MemoryUtil.memPutFloat(ptr + 8L, wz);
                MemoryUtil.memPutInt(ptr + 12L, abgr);
                MemoryUtil.memPutFloat(ptr + 16L, uvArr[uvBase + v * 2]);
                MemoryUtil.memPutFloat(ptr + 20L, uvArr[uvBase + v * 2 + 1]);
                MemoryUtil.memPutInt(ptr + 24L, overlay);
                MemoryUtil.memPutInt(ptr + 28L, light);
                MemoryUtil.memPutInt(ptr + 32L, packedNormal);
                if (format54B) {
                    MemoryUtil.memPutShort(ptr + 36L, entityId);
                    MemoryUtil.memPutShort(ptr + 38L, blockEntityId);
                    MemoryUtil.memPutShort(ptr + 40L, itemId);
                    MemoryUtil.memPutFloat(ptr + 42L, midU);
                    MemoryUtil.memPutFloat(ptr + 46L, midV);
                    MemoryUtil.memPutInt(ptr + 50L, packedTangent);
                }
                ptr += stride;
            }
        }
        return quadCount * 4;
    }

    private static void emitQuadsFallback(
            BakedQuadMesh mesh,
            Matrix4f poseMat,
            Matrix3f normalMat,
            VertexConsumer consumer,
            int light,
            int overlay,
            int abgr) {
        int r = abgr & 0xFF;
        int g = (abgr >>> 8) & 0xFF;
        int b = (abgr >>> 16) & 0xFF;
        int a = (abgr >>> 24) & 0xFF;

        float m00 = poseMat.m00(), m01 = poseMat.m01(), m02 = poseMat.m02();
        float m10 = poseMat.m10(), m11 = poseMat.m11(), m12 = poseMat.m12();
        float m20 = poseMat.m20(), m21 = poseMat.m21(), m22 = poseMat.m22();
        float m30 = poseMat.m30(), m31 = poseMat.m31(), m32 = poseMat.m32();

        float n00 = 1f, n01 = 0f, n02 = 0f;
        float n10 = 0f, n11 = 1f, n12 = 0f;
        float n20 = 0f, n21 = 0f, n22 = 1f;
        if (normalMat != null) {
            n00 = normalMat.m00(); n01 = normalMat.m01(); n02 = normalMat.m02();
            n10 = normalMat.m10(); n11 = normalMat.m11(); n12 = normalMat.m12();
            n20 = normalMat.m20(); n21 = normalMat.m21(); n22 = normalMat.m22();
        }

        float[] posArr = mesh.pos;
        float[] uvArr = mesh.uv;
        float[] normArr = mesh.normal;
        for (int q = 0; q < mesh.quadCount; q++) {
            int nBase = q * 3;
            float lnx = normArr[nBase], lny = normArr[nBase + 1], lnz = normArr[nBase + 2];
            float wnx = n00 * lnx + n10 * lny + n20 * lnz;
            float wny = n01 * lnx + n11 * lny + n21 * lnz;
            float wnz = n02 * lnx + n12 * lny + n22 * lnz;
            float lenSq = wnx * wnx + wny * wny + wnz * wnz;
            if (lenSq > 1e-12f && Math.abs(lenSq - 1.0f) > 1e-4f) {
                float inv = (float) (1.0 / Math.sqrt(lenSq));
                wnx *= inv; wny *= inv; wnz *= inv;
            }
            int pBase = q * 12;
            int uvBase = q * 8;
            for (int v = 0; v < 4; v++) {
                float lx = posArr[pBase + v * 3];
                float ly = posArr[pBase + v * 3 + 1];
                float lz = posArr[pBase + v * 3 + 2];
                float wx = m00 * lx + m10 * ly + m20 * lz + m30;
                float wy = m01 * lx + m11 * ly + m21 * lz + m31;
                float wz = m02 * lx + m12 * ly + m22 * lz + m32;
                consumer.addVertex(wx, wy, wz)
                        .setColor(r, g, b, a)
                        .setUv(uvArr[uvBase + v * 2], uvArr[uvBase + v * 2 + 1])
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(wnx, wny, wnz);
            }
        }
    }

    private static boolean detectSodium() {
        try {
            Class.forName(
                    "net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter",
                    false,
                    FastEntityVertexWriter.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private static final class ScratchBuffer {
        final long ptr = MemoryUtil.nmemAlignedAlloc(64L, SCRATCH_BYTES);
    }

    private static final class SodiumAccess {
        static VertexFormat extractFormat(VertexConsumer consumer) {
            if (consumer instanceof BufferBuilderFormatAccessor accessor) {
                return accessor.tapetum$getFormat();
            }
            if (consumer instanceof FormatAwareWriter aware) {
                return aware.tapetum$getTargetFormat();
            }
            return null;
        }

        static Mode resolveMode(VertexConsumer consumer) {
            if (VertexBufferWriter.tryOf(consumer) == null) {
                return Mode.FALLBACK_CONSUMER;
            }
            VertexFormat fmt = extractFormat(consumer);
            if (fmt == null) {
                return Mode.FALLBACK_CONSUMER;
            }
            VertexFormat irisFmt = IrisVertexBridge.getVerifiedEntityFormat();
            if (irisFmt != null && fmt == irisFmt) {
                return Mode.FAST_54B;
            }
            if (fmt == EntityVertex.FORMAT) {
                return Mode.FAST_36B;
            }
            return Mode.FALLBACK_CONSUMER;
        }

        static void emitQuadsFast(
                BakedQuadMesh mesh,
                Matrix4f poseMat,
                Matrix3f normalMat,
                VertexConsumer consumer,
                int light,
                int overlay,
                int abgr,
                boolean format54B) {
            VertexBufferWriter writer = VertexBufferWriter.of(consumer);
            VertexFormat format = format54B ? IrisVertexBridge.getVerifiedEntityFormat() : EntityVertex.FORMAT;
            if (format == null) {
                emitQuadsFallback(mesh, poseMat, normalMat, consumer, light, overlay, abgr);
                return;
            }
            short entityId = format54B ? IrisVertexBridge.currentEntityId() : 0;
            short blockEntityId = format54B ? IrisVertexBridge.currentBlockEntityId() : 0;
            short itemId = format54B ? IrisVertexBridge.currentItemId() : 0;

            long scratchPtr = PRIMARY_SCRATCH.get().ptr;
            int maxQuadsPerChunk = SCRATCH_MAX_VERTS / 4;
            int totalQuads = mesh.quadCount;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                for (int offset = 0; offset < totalQuads; offset += maxQuadsPerChunk) {
                    int batchQuads = Math.min(maxQuadsPerChunk, totalQuads - offset);
                    int verts = writeQuadsToBuffer(
                            mesh, offset, batchQuads, poseMat, normalMat, light, overlay, abgr,
                            format54B, entityId, blockEntityId, itemId, scratchPtr);
                    writer.push(stack, scratchPtr, verts, format);
                }
            }
        }
    }
}
