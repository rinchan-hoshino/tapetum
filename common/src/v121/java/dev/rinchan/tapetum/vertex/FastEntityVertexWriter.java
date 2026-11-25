package dev.rinchan.tapetum.vertex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.rinchan.tapetum.geometry.BakedQuadMesh;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import net.caffeinemc.mods.sodium.client.render.vertex.buffer.BufferBuilderExtension;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Layer 1 unified off-heap vertex writer supporting both Sodium 36B {@code EntityVertex.FORMAT}
 * and Iris 54B {@code IrisVertexFormats.ENTITY} direct {@code copyMemory} submission, with
 * automatic fallback to standard {@link VertexConsumer} for unknown formats or wrappers.
 */
public final class FastEntityVertexWriter {
    public enum Mode {
        FAST_54B,
        FAST_36B,
        FALLBACK_CONSUMER
    }

    /** Capacity in vertices per scratch flush (divisible by both 4 and 3: 1020 = 255 quads = 340 tris). */
    public static final int SCRATCH_MAX_VERTS = 1020;
    private static final int SCRATCH_BYTES = SCRATCH_MAX_VERTS * EntityVertexLayout.STRIDE_54B;

    private static final boolean SODIUM_PRESENT = detectSodium();

    private static final ThreadLocal<ScratchBuffer> PRIMARY_SCRATCH = ThreadLocal.withInitial(ScratchBuffer::new);
    private static final ThreadLocal<ScratchBuffer> UNSHADED_SCRATCH = ThreadLocal.withInitial(ScratchBuffer::new);

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
            float r,
            float g,
            float b,
            float a) {
        emitQuads(mesh, pose.pose(), pose.normal(), consumer, light, overlay, EntityVertexLayout.packAbgr(r, g, b, a));
    }

    public static void emitQuads(
            BakedQuadMesh mesh,
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int light,
            int overlay,
            int abgr) {
        emitQuads(mesh, pose.pose(), pose.normal(), consumer, light, overlay, abgr);
    }

    public static void emitQuads(
            BakedQuadMesh mesh,
            Matrix4f poseMat,
            Matrix3f normalMat,
            VertexConsumer consumer,
            int light,
            int overlay,
            int abgr) {
        if (mesh == null || mesh.isEmpty()) {
            return;
        }
        Mode mode = resolveMode(consumer);
        if (mode == Mode.FALLBACK_CONSUMER) {
            emitQuadsFallback(mesh, poseMat, normalMat, consumer, light, overlay, abgr, false, 0f, 0f, 0f);
            return;
        }
        SodiumAccess.emitQuadsFast(
                mesh, poseMat, normalMat, consumer, light, overlay, abgr, mode == Mode.FAST_54B, false, 0f, 0f, 0f);
    }

    public static void emitQuadsFixedNormal(
            BakedQuadMesh mesh,
            Matrix4f poseMat,
            VertexConsumer consumer,
            int light,
            int overlay,
            int abgr,
            float fixedNx,
            float fixedNy,
            float fixedNz) {
        if (mesh == null || mesh.isEmpty()) {
            return;
        }
        Mode mode = resolveMode(consumer);
        if (mode == Mode.FALLBACK_CONSUMER) {
            emitQuadsFallback(mesh, poseMat, null, consumer, light, overlay, abgr, true, fixedNx, fixedNy, fixedNz);
            return;
        }
        SodiumAccess.emitQuadsFast(
                mesh, poseMat, null, consumer, light, overlay, abgr, mode == Mode.FAST_54B, true, fixedNx, fixedNy, fixedNz);
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
        return writeQuadsToBuffer(
                mesh, startQuad, quadCount, poseMat, normalMat, light, overlay, abgr,
                format54B, entityId, blockEntityId, itemId, false, 0f, 0f, 0f, dstBasePtr);
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
            boolean useFixedNormal,
            float fixedNx,
            float fixedNy,
            float fixedNz,
            long dstBasePtr) {
        float m00 = poseMat.m00(), m01 = poseMat.m01(), m02 = poseMat.m02();
        float m10 = poseMat.m10(), m11 = poseMat.m11(), m12 = poseMat.m12();
        float m20 = poseMat.m20(), m21 = poseMat.m21(), m22 = poseMat.m22();
        float m30 = poseMat.m30(), m31 = poseMat.m31(), m32 = poseMat.m32();

        float n00 = 1f, n01 = 0f, n02 = 0f;
        float n10 = 0f, n11 = 1f, n12 = 0f;
        float n20 = 0f, n21 = 0f, n22 = 1f;
        if (!useFixedNormal && normalMat != null) {
            n00 = normalMat.m00(); n01 = normalMat.m01(); n02 = normalMat.m02();
            n10 = normalMat.m10(); n11 = normalMat.m11(); n12 = normalMat.m12();
            n20 = normalMat.m20(); n21 = normalMat.m21(); n22 = normalMat.m22();
        }

        int fixedPackedNormal = useFixedNormal ? EntityVertexLayout.packNormI8(fixedNx, fixedNy, fixedNz) : 0;
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
            int packedNormal;
            if (useFixedNormal) {
                packedNormal = fixedPackedNormal;
            } else {
                float lnx = normArr[nBase];
                float lny = normArr[nBase + 1];
                float lnz = normArr[nBase + 2];
                float wnx = n00 * lnx + n10 * lny + n20 * lnz;
                float wny = n01 * lnx + n11 * lny + n21 * lnz;
                float wnz = n02 * lnx + n12 * lny + n22 * lnz;
                float lenSq = wnx * wnx + wny * wny + wnz * wnz;
                if (lenSq > 1e-12f && Math.abs(lenSq - 1.0f) > 1e-4f) {
                    float inv = (float) (1.0 / Math.sqrt(lenSq));
                    wnx *= inv;
                    wny *= inv;
                    wnz *= inv;
                }
                packedNormal = EntityVertexLayout.packNormI8(wnx, wny, wnz);
            }

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

    public static void pushWithFixedNormal(
            VertexConsumer delegate,
            MemoryStack stack,
            long srcPtr,
            int count,
            VertexFormat format,
            float nx,
            float ny,
            float nz) {
        if (count <= 0 || !SODIUM_PRESENT) {
            return;
        }
        SodiumAccess.pushWithFixedNormal(delegate, stack, srcPtr, count, format, nx, ny, nz);
    }

    public static void routeLightPassBuffer(
            VertexBufferWriter ordinaryWriter,
            VertexBufferWriter emissiveWriter,
            int primitiveSize,
            MemoryStack stack,
            long ptr,
            int count,
            VertexFormat format,
            int stride) {
        if (count <= 0) {
            return;
        }
        int runStart = 0;
        boolean runEmissive = false;
        boolean hasRun = false;

        for (int primStart = 0; primStart < count; primStart += primitiveSize) {
            long firstPtr = ptr + (long) primStart * stride;
            boolean primEmissive = MemoryUtil.memGetInt(firstPtr + EntityVertexLayout.OFFSET_UV2) != 0;
            for (int k = 1; k < primitiveSize; k++) {
                boolean vEmissive =
                        MemoryUtil.memGetInt(ptr + (long) (primStart + k) * stride + EntityVertexLayout.OFFSET_UV2)
                                != 0;
                if (vEmissive != primEmissive) {
                    throw new IllegalArgumentException("Mixed light values within one mesh primitive");
                }
            }
            if (!hasRun) {
                runStart = primStart;
                runEmissive = primEmissive;
                hasRun = true;
            } else if (primEmissive != runEmissive) {
                VertexBufferWriter target = runEmissive ? emissiveWriter : ordinaryWriter;
                target.push(stack, ptr + (long) runStart * stride, primStart - runStart, format);
                runStart = primStart;
                runEmissive = primEmissive;
            }
        }
        if (hasRun && runStart < count) {
            VertexBufferWriter target = runEmissive ? emissiveWriter : ordinaryWriter;
            target.push(stack, ptr + (long) runStart * stride, count - runStart, format);
        }
    }

    public static void copyWithOverwrittenNormal(long srcPtr, long dstPtr, int count, int stride, int packedNormal) {
        int totalBytes = count * stride;
        MemoryUtil.memCopy(srcPtr, dstPtr, totalBytes);
        long p = dstPtr + EntityVertexLayout.OFFSET_NORMAL;
        for (int i = 0; i < count; i++) {
            MemoryUtil.memPutInt(p, packedNormal);
            p += stride;
        }
    }

    public static long primaryScratchPtr() {
        return PRIMARY_SCRATCH.get().ptr;
    }

    public static VertexFormat entityFormat36B() {
        return SODIUM_PRESENT ? SodiumAccess.entityFormat() : null;
    }

    public static void pushScratch(
            VertexConsumer consumer, MemoryStack stack, long ptr, int count, boolean format54B) {
        SodiumAccess.pushScratch(consumer, stack, ptr, count, format54B);
    }

    private static void emitQuadsFallback(
            BakedQuadMesh mesh,
            Matrix4f poseMat,
            Matrix3f normalMat,
            VertexConsumer consumer,
            int light,
            int overlay,
            int abgr,
            boolean useFixedNormal,
            float fixedNx,
            float fixedNy,
            float fixedNz) {
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
        if (!useFixedNormal && normalMat != null) {
            n00 = normalMat.m00(); n01 = normalMat.m01(); n02 = normalMat.m02();
            n10 = normalMat.m10(); n11 = normalMat.m11(); n12 = normalMat.m12();
            n20 = normalMat.m20(); n21 = normalMat.m21(); n22 = normalMat.m22();
        }

        float[] posArr = mesh.pos;
        float[] uvArr = mesh.uv;
        float[] normArr = mesh.normal;
        for (int q = 0; q < mesh.quadCount; q++) {
            float wnx, wny, wnz;
            if (useFixedNormal) {
                wnx = fixedNx;
                wny = fixedNy;
                wnz = fixedNz;
            } else {
                int nBase = q * 3;
                float lnx = normArr[nBase];
                float lny = normArr[nBase + 1];
                float lnz = normArr[nBase + 2];
                wnx = n00 * lnx + n10 * lny + n20 * lnz;
                wny = n01 * lnx + n11 * lny + n21 * lnz;
                wnz = n02 * lnx + n12 * lny + n22 * lnz;
                float lenSq = wnx * wnx + wny * wny + wnz * wnz;
                if (lenSq > 1e-12f && Math.abs(lenSq - 1.0f) > 1e-4f) {
                    float inv = (float) (1.0 / Math.sqrt(lenSq));
                    wnx *= inv;
                    wny *= inv;
                    wnz *= inv;
                }
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
        static VertexFormat entityFormat() {
            return EntityVertex.FORMAT;
        }

        static VertexFormat extractFormat(VertexConsumer consumer) {
            if (consumer instanceof BufferBuilderExtension ext) {
                return ext.sodium$getVertexFormat();
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
                boolean format54B,
                boolean useFixedNormal,
                float fixedNx,
                float fixedNy,
                float fixedNz) {
            VertexBufferWriter writer = VertexBufferWriter.of(consumer);
            VertexFormat format = format54B ? IrisVertexBridge.getVerifiedEntityFormat() : EntityVertex.FORMAT;
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
                            mesh,
                            offset,
                            batchQuads,
                            poseMat,
                            normalMat,
                            light,
                            overlay,
                            abgr,
                            format54B,
                            entityId,
                            blockEntityId,
                            itemId,
                            useFixedNormal,
                            fixedNx,
                            fixedNy,
                            fixedNz,
                            scratchPtr);
                    writer.push(stack, scratchPtr, verts, format);
                }
            }
        }

        static void pushScratch(
                VertexConsumer consumer, MemoryStack stack, long ptr, int count, boolean format54B) {
            VertexFormat format = format54B ? IrisVertexBridge.getVerifiedEntityFormat() : EntityVertex.FORMAT;
            VertexBufferWriter.of(consumer).push(stack, ptr, count, format);
        }

        static void pushWithFixedNormal(
                VertexConsumer delegate,
                MemoryStack stack,
                long srcPtr,
                int count,
                VertexFormat format,
                float nx,
                float ny,
                float nz) {
            VertexBufferWriter writer = VertexBufferWriter.of(delegate);
            int stride = format.getVertexSize();
            VertexFormat irisFmt = IrisVertexBridge.getVerifiedEntityFormat();
            boolean knownLayout = format == EntityVertex.FORMAT || (irisFmt != null && format == irisFmt);
            if (!knownLayout) {
                writer.push(stack, srcPtr, count, format);
                return;
            }
            int packedNormal = EntityVertexLayout.packNormI8(nx, ny, nz);
            long dstPtr = UNSHADED_SCRATCH.get().ptr;
            int maxVerts = SCRATCH_BYTES / stride;
            maxVerts -= (maxVerts % 12);
            for (int offset = 0; offset < count; offset += maxVerts) {
                int batch = Math.min(maxVerts, count - offset);
                int bytes = batch * stride;
                MemoryIntrinsics.copyMemory(srcPtr + (long) offset * stride, dstPtr, bytes);
                long p = dstPtr + EntityVertexLayout.OFFSET_NORMAL;
                for (int i = 0; i < batch; i++) {
                    MemoryUtil.memPutInt(p, packedNormal);
                    p += stride;
                }
                writer.push(stack, dstPtr, batch, format);
            }
        }
    }
}
