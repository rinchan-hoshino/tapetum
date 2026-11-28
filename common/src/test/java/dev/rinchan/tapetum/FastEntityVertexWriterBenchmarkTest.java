package dev.rinchan.tapetum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rinchan.tapetum.geometry.BakedQuadMesh;
import dev.rinchan.tapetum.vertex.EntityVertexLayout;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import dev.rinchan.tapetum.vertex.TangentMath;
import java.util.Arrays;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryUtil;

/**
 * Deterministic CPU throughput benchmark comparing Tapetum's pre-baked 54-byte Iris extended
 * shader vertex emission ({@link BakedQuadMesh} + {@link FastEntityVertexWriter#writeQuadsToBuffer})
 * against a per-frame quad normal/midUV/tangent recomputation baseline producing the identical
 * 54-byte off-heap vertex layout.
 */
final class FastEntityVertexWriterBenchmarkTest {
    private static final int QUAD_COUNT = 256; // 1,024 vertices (typical detailed modded entity/armor piece)
    private static final int WARMUP_ITERS = 2_000;
    private static final int MEASURE_ITERS = 10_000; // 2.56M quads / 10.24M vertices per trial
    private static final int TRIALS = 5;

    private static BakedQuadMesh buildDeterministicMesh(int quadCount) {
        BakedQuadMesh.Builder builder = BakedQuadMesh.plainBuilder();
        for (int q = 0; q < quadCount; q++) {
            float angle = (q * 0.61803398875f) * (float) (Math.PI * 2.0);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float ox = (q % 16) * 0.125f - 1.0f;
            float oy = (q / 16) * 0.125f;
            float oz = ((q * 7) % 11) * 0.05f - 0.25f;
            float s = 0.1f;

            float x0 = ox, y0 = oy, z0 = oz;
            float x1 = ox + s * cos, y1 = oy, z1 = oz + s * sin;
            float x2 = ox + s * cos, y2 = oy + s, z2 = oz + s * sin;
            float x3 = ox, y3 = oy + s, z3 = oz;

            // Outward normal perpendicular to (cos, 0, sin) and (0, 1, 0) -> (-sin, 0, cos)
            float nx = -sin;
            float ny = 0.0f;
            float nz = cos;

            float u0 = (q % 8) * 0.125f;
            float v0 = ((q / 8) % 8) * 0.125f;
            float u1 = u0 + 0.125f;
            float v1 = v0 + 0.125f;

            builder.addQuad(
                    x0, y0, z0, u0, v0,
                    x1, y1, z1, u1, v0,
                    x2, y2, z2, u1, v1,
                    x3, y3, z3, u0, v1,
                    nx, ny, nz);
        }
        return builder.build();
    }

    /**
     * Baseline representing per-frame world-space quad emission without pre-baked tangents/midUV:
     * transforms all 4 vertices, recomputes face normal, midUV, and flat tangent on the CPU every
     * frame, and writes the same 54-byte Iris extended entity vertex format.
     */
    private static int emitBaselineRecompute54B(
            BakedQuadMesh mesh,
            Matrix4f poseMat,
            Matrix3f normalMat,
            int light,
            int overlay,
            int abgr,
            short entityId,
            short blockEntityId,
            short itemId,
            long dstBasePtr) {
        float m00 = poseMat.m00(), m01 = poseMat.m01(), m02 = poseMat.m02();
        float m10 = poseMat.m10(), m11 = poseMat.m11(), m12 = poseMat.m12();
        float m20 = poseMat.m20(), m21 = poseMat.m21(), m22 = poseMat.m22();
        float m30 = poseMat.m30(), m31 = poseMat.m31(), m32 = poseMat.m32();

        float n00 = normalMat.m00(), n01 = normalMat.m01(), n02 = normalMat.m02();
        float n10 = normalMat.m10(), n11 = normalMat.m11(), n12 = normalMat.m12();
        float n20 = normalMat.m20(), n21 = normalMat.m21(), n22 = normalMat.m22();

        float[] posArr = mesh.pos;
        float[] uvArr = mesh.uv;
        float[] normArr = mesh.normal;
        int quadCount = mesh.quadCount;

        Vector4f tangentScratch = new Vector4f();
        long ptr = dstBasePtr;

        for (int q = 0; q < quadCount; q++) {
            int pBase = q * 12;
            int uvBase = q * 8;
            int nBase = q * 3;

            float x0 = m00 * posArr[pBase] + m10 * posArr[pBase + 1] + m20 * posArr[pBase + 2] + m30;
            float y0 = m01 * posArr[pBase] + m11 * posArr[pBase + 1] + m21 * posArr[pBase + 2] + m31;
            float z0 = m02 * posArr[pBase] + m12 * posArr[pBase + 1] + m22 * posArr[pBase + 2] + m32;

            float x1 = m00 * posArr[pBase + 3] + m10 * posArr[pBase + 4] + m20 * posArr[pBase + 5] + m30;
            float y1 = m01 * posArr[pBase + 3] + m11 * posArr[pBase + 4] + m21 * posArr[pBase + 5] + m31;
            float z1 = m02 * posArr[pBase + 3] + m12 * posArr[pBase + 4] + m22 * posArr[pBase + 5] + m32;

            float x2 = m00 * posArr[pBase + 6] + m10 * posArr[pBase + 7] + m20 * posArr[pBase + 8] + m30;
            float y2 = m01 * posArr[pBase + 6] + m11 * posArr[pBase + 7] + m21 * posArr[pBase + 8] + m31;
            float z2 = m02 * posArr[pBase + 6] + m12 * posArr[pBase + 7] + m22 * posArr[pBase + 8] + m32;

            float x3 = m00 * posArr[pBase + 9] + m10 * posArr[pBase + 10] + m20 * posArr[pBase + 11] + m30;
            float y3 = m01 * posArr[pBase + 9] + m11 * posArr[pBase + 10] + m21 * posArr[pBase + 11] + m31;
            float z3 = m02 * posArr[pBase + 9] + m12 * posArr[pBase + 10] + m22 * posArr[pBase + 11] + m32;

            float u0 = uvArr[uvBase], v0 = uvArr[uvBase + 1];
            float u1 = uvArr[uvBase + 2], v1 = uvArr[uvBase + 3];
            float u2 = uvArr[uvBase + 4], v2 = uvArr[uvBase + 5];
            float u3 = uvArr[uvBase + 6], v3 = uvArr[uvBase + 7];

            float midU = (u0 + u1 + u2 + u3) * 0.25f;
            float midV = (v0 + v1 + v2 + v3) * 0.25f;

            float lnx = normArr[nBase], lny = normArr[nBase + 1], lnz = normArr[nBase + 2];
            float wnx = n00 * lnx + n10 * lny + n20 * lnz;
            float wny = n01 * lnx + n11 * lny + n21 * lnz;
            float wnz = n02 * lnx + n12 * lny + n22 * lnz;
            float invN = TangentMath.rsqrt(wnx * wnx + wny * wny + wnz * wnz);
            wnx *= invN;
            wny *= invN;
            wnz *= invN;
            int packedNormal = EntityVertexLayout.packNormI8(wnx, wny, wnz);

            TangentMath.computeFlatTangent(
                    wnx, wny, wnz,
                    x0, y0, z0, u0, v0,
                    x1, y1, z1, u1, v1,
                    x2, y2, z2, u2, v2,
                    tangentScratch);
            int packedTangent = EntityVertexLayout.packNormI8(
                    tangentScratch.x, tangentScratch.y, tangentScratch.z, tangentScratch.w);

            writeVertex54B(ptr, x0, y0, z0, abgr, u0, v0, overlay, light, packedNormal, entityId, blockEntityId, itemId, midU, midV, packedTangent);
            ptr += EntityVertexLayout.STRIDE_54B;
            writeVertex54B(ptr, x1, y1, z1, abgr, u1, v1, overlay, light, packedNormal, entityId, blockEntityId, itemId, midU, midV, packedTangent);
            ptr += EntityVertexLayout.STRIDE_54B;
            writeVertex54B(ptr, x2, y2, z2, abgr, u2, v2, overlay, light, packedNormal, entityId, blockEntityId, itemId, midU, midV, packedTangent);
            ptr += EntityVertexLayout.STRIDE_54B;
            writeVertex54B(ptr, x3, y3, z3, abgr, u3, v3, overlay, light, packedNormal, entityId, blockEntityId, itemId, midU, midV, packedTangent);
            ptr += EntityVertexLayout.STRIDE_54B;
        }
        return quadCount * 4;
    }

    private static void writeVertex54B(
            long ptr,
            float x, float y, float z,
            int abgr,
            float u, float v,
            int overlay, int light, int packedNormal,
            short entityId, short blockEntityId, short itemId,
            float midU, float midV, int packedTangent) {
        MemoryUtil.memPutFloat(ptr, x);
        MemoryUtil.memPutFloat(ptr + 4L, y);
        MemoryUtil.memPutFloat(ptr + 8L, z);
        MemoryUtil.memPutInt(ptr + 12L, abgr);
        MemoryUtil.memPutFloat(ptr + 16L, u);
        MemoryUtil.memPutFloat(ptr + 20L, v);
        MemoryUtil.memPutInt(ptr + 24L, overlay);
        MemoryUtil.memPutInt(ptr + 28L, light);
        MemoryUtil.memPutInt(ptr + 32L, packedNormal);
        MemoryUtil.memPutShort(ptr + 36L, entityId);
        MemoryUtil.memPutShort(ptr + 38L, blockEntityId);
        MemoryUtil.memPutShort(ptr + 40L, itemId);
        MemoryUtil.memPutFloat(ptr + 42L, midU);
        MemoryUtil.memPutFloat(ptr + 46L, midV);
        MemoryUtil.memPutInt(ptr + 50L, packedTangent);
    }

    @Test
    void benchmarkCached54BEmissionVsPerFrameRecomputation() {
        BakedQuadMesh mesh = buildDeterministicMesh(QUAD_COUNT);
        Matrix4f pose = new Matrix4f()
                .translate(1.25f, -0.5f, 3.75f)
                .rotateY(0.73f)
                .rotateX(0.31f);
        Matrix3f normalMat = new Matrix3f(pose).invert().transpose();

        int light = 0x00F000F0;
        int overlay = 0x000A0000;
        int abgr = 0xFFFFFFFF;
        short entityId = 17, blockEntityId = 23, itemId = 5;

        long bufFast = MemoryUtil.nmemAlloc((long) QUAD_COUNT * 4L * EntityVertexLayout.STRIDE_54B);
        long bufBase = MemoryUtil.nmemAlloc((long) QUAD_COUNT * 4L * EntityVertexLayout.STRIDE_54B);
        try {
            // Verify output parity before timing.
            int fastVerts = FastEntityVertexWriter.writeQuadsToBuffer(
                    mesh, 0, QUAD_COUNT, pose, normalMat, light, overlay, abgr,
                    true, entityId, blockEntityId, itemId, bufFast);
            int baseVerts = emitBaselineRecompute54B(
                    mesh, pose, normalMat, light, overlay, abgr,
                    entityId, blockEntityId, itemId, bufBase);
            assertEquals(QUAD_COUNT * 4, fastVerts);
            assertEquals(fastVerts, baseVerts);

            for (int v = 0; v < fastVerts; v++) {
                long off = (long) v * EntityVertexLayout.STRIDE_54B;
                assertEquals(MemoryUtil.memGetFloat(bufBase + off), MemoryUtil.memGetFloat(bufFast + off), 1e-5f);
                assertEquals(MemoryUtil.memGetFloat(bufBase + off + 4L), MemoryUtil.memGetFloat(bufFast + off + 4L), 1e-5f);
                assertEquals(MemoryUtil.memGetFloat(bufBase + off + 8L), MemoryUtil.memGetFloat(bufFast + off + 8L), 1e-5f);
                assertEquals(MemoryUtil.memGetInt(bufBase + off + 32L), MemoryUtil.memGetInt(bufFast + off + 32L));
                assertEquals(MemoryUtil.memGetFloat(bufBase + off + 42L), MemoryUtil.memGetFloat(bufFast + off + 42L), 1e-6f);
                assertEquals(MemoryUtil.memGetFloat(bufBase + off + 46L), MemoryUtil.memGetFloat(bufFast + off + 46L), 1e-6f);
            }

            long sink = 0L;
            // Warmup both paths equally.
            for (int i = 0; i < WARMUP_ITERS; i++) {
                FastEntityVertexWriter.writeQuadsToBuffer(
                        mesh, 0, QUAD_COUNT, pose, normalMat, light, overlay, abgr,
                        true, entityId, blockEntityId, itemId, bufFast);
                sink ^= MemoryUtil.memGetLong(bufFast + ((i & 255) * EntityVertexLayout.STRIDE_54B));
                emitBaselineRecompute54B(
                        mesh, pose, normalMat, light, overlay, abgr,
                        entityId, blockEntityId, itemId, bufBase);
                sink ^= MemoryUtil.memGetLong(bufBase + ((i & 255) * EntityVertexLayout.STRIDE_54B));
            }

            long[] fastNs = new long[TRIALS];
            long[] baseNs = new long[TRIALS];
            for (int t = 0; t < TRIALS; t++) {
                long t0 = System.nanoTime();
                for (int i = 0; i < MEASURE_ITERS; i++) {
                    FastEntityVertexWriter.writeQuadsToBuffer(
                            mesh, 0, QUAD_COUNT, pose, normalMat, light, overlay, abgr,
                            true, entityId, blockEntityId, itemId, bufFast);
                    sink ^= MemoryUtil.memGetLong(bufFast + ((i & 255) * EntityVertexLayout.STRIDE_54B));
                }
                fastNs[t] = System.nanoTime() - t0;

                long t1 = System.nanoTime();
                for (int i = 0; i < MEASURE_ITERS; i++) {
                    emitBaselineRecompute54B(
                            mesh, pose, normalMat, light, overlay, abgr,
                            entityId, blockEntityId, itemId, bufBase);
                    sink ^= MemoryUtil.memGetLong(bufBase + ((i & 255) * EntityVertexLayout.STRIDE_54B));
                }
                baseNs[t] = System.nanoTime() - t1;
            }

            Arrays.sort(fastNs);
            Arrays.sort(baseNs);
            long medFastNs = fastNs[TRIALS / 2];
            long medBaseNs = baseNs[TRIALS / 2];

            double totalQuads = (double) QUAD_COUNT * MEASURE_ITERS;
            double totalVerts = totalQuads * 4.0;
            double fastMQuadsPerSec = totalQuads / (medFastNs / 1e9) / 1e6;
            double baseMQuadsPerSec = totalQuads / (medBaseNs / 1e9) / 1e6;
            double fastMVertsPerSec = totalVerts / (medFastNs / 1e9) / 1e6;
            double baseMVertsPerSec = totalVerts / (medBaseNs / 1e9) / 1e6;
            double speedup = (double) medBaseNs / (double) medFastNs;

            System.out.printf(
                    "[TAPETUM-BENCH] quads=%d iters=%d trials=%d | tapetum=%.2f Mquads/s (%.2f Mverts/s, %.2f ms) | baseline=%.2f Mquads/s (%.2f Mverts/s, %.2f ms) | speedup=%.2fx | sink=%d%n",
                    QUAD_COUNT, MEASURE_ITERS, TRIALS,
                    fastMQuadsPerSec, fastMVertsPerSec, medFastNs / 1e6,
                    baseMQuadsPerSec, baseMVertsPerSec, medBaseNs / 1e6,
                    speedup, sink);

            assertTrue(speedup > 1.05, "Expected cached 54B emission to outperform per-frame tangent recomputation, got " + speedup + "x");
        } finally {
            MemoryUtil.nmemFree(bufFast);
            MemoryUtil.nmemFree(bufBase);
        }
    }
}
