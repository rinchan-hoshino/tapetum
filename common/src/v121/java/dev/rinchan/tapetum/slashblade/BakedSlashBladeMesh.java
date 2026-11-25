package dev.rinchan.tapetum.slashblade;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.rinchan.tapetum.geometry.MeshCacheRegistry;
import dev.rinchan.tapetum.vertex.EntityVertexLayout;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import dev.rinchan.tapetum.vertex.IrisVertexBridge;
import dev.rinchan.tapetum.vertex.TangentMath;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Layer 2 pre-baked triangle mesh for SlashBlade OBJ {@code GroupObject}s.
 * Pre-computes per-triangle {@code midUV} and Iris-compatible per-vertex smooth tangents
 * ({@code NormalHelper.computeTangentSmooth}) once at bake time, then submits directly through
 * 54B/36B {@link FastEntityVertexWriter} off-heap buffers.
 */
public final class BakedSlashBladeMesh {
    public static final byte FLAG_HAS_UV = 1 << 0;
    public static final byte FLAG_HAS_NORMAL = 1 << 1;
    public static final byte FLAG_Y_IS_ZERO = 1 << 2;

    public final int vertexCount;
    public final int triCount;
    public final boolean allTriangles;
    public final float[] pos;
    public final float[] uv;
    public final float[] uvOffset;
    public final float[] triRawMidUv;
    public final float[] triMidUvOffset;
    public final float[] normal;
    public final float[] tangent;
    public final byte[] tangentW;
    public final byte[] flags;

    public BakedSlashBladeMesh(
            int vertexCount,
            boolean allTriangles,
            float[] pos,
            float[] uv,
            float[] uvOffset,
            float[] normal,
            byte[] flags) {
        this.vertexCount = vertexCount;
        this.allTriangles = allTriangles && (vertexCount % 3 == 0);
        this.triCount = this.allTriangles ? (vertexCount / 3) : 0;
        this.pos = pos;
        this.uv = uv;
        this.uvOffset = uvOffset;
        this.normal = normal;
        this.flags = flags;

        if (this.allTriangles && this.triCount > 0) {
            this.triRawMidUv = new float[this.triCount * 2];
            this.triMidUvOffset = new float[this.triCount * 2];
            this.tangent = new float[vertexCount * 3];
            this.tangentW = new byte[vertexCount];
            bakeTriangleAttributes();
        } else {
            this.triRawMidUv = new float[0];
            this.triMidUvOffset = new float[0];
            this.tangent = new float[0];
            this.tangentW = new byte[0];
        }
        if (vertexCount > 0) {
            MeshCacheRegistry.recordBakedMesh();
        }
    }

    public boolean isEmpty() {
        return vertexCount == 0;
    }

    private void bakeTriangleAttributes() {
        org.joml.Vector4f scratch = new org.joml.Vector4f();
        for (int t = 0; t < triCount; t++) {
            int v0 = t * 3;
            int v1 = v0 + 1;
            int v2 = v0 + 2;

            boolean hasUv0 = (flags[v0] & FLAG_HAS_UV) != 0;
            boolean hasUv1 = (flags[v1] & FLAG_HAS_UV) != 0;
            boolean hasUv2 = (flags[v2] & FLAG_HAS_UV) != 0;

            float ru0 = hasUv0 ? uv[v0 * 2] : 0.0f;
            float rv0 = hasUv0 ? uv[v0 * 2 + 1] : 0.0f;
            float ou0 = hasUv0 ? uvOffset[v0 * 2] : 0.0f;
            float ov0 = hasUv0 ? uvOffset[v0 * 2 + 1] : 0.0f;

            float ru1 = hasUv1 ? uv[v1 * 2] : 0.0f;
            float rv1 = hasUv1 ? uv[v1 * 2 + 1] : 0.0f;
            float ou1 = hasUv1 ? uvOffset[v1 * 2] : 0.0f;
            float ov1 = hasUv1 ? uvOffset[v1 * 2 + 1] : 0.0f;

            float ru2 = hasUv2 ? uv[v2 * 2] : 0.0f;
            float rv2 = hasUv2 ? uv[v2 * 2 + 1] : 0.0f;
            float ou2 = hasUv2 ? uvOffset[v2 * 2] : 0.0f;
            float ov2 = hasUv2 ? uvOffset[v2 * 2 + 1] : 0.0f;

            triRawMidUv[t * 2] = (ru0 + ru1 + ru2) / 3.0f;
            triRawMidUv[t * 2 + 1] = (rv0 + rv1 + rv2) / 3.0f;
            triMidUvOffset[t * 2] = (ou0 + ou1 + ou2) / 3.0f;
            triMidUvOffset[t * 2 + 1] = (ov0 + ov1 + ov2) / 3.0f;

            float eu0 = ru0 + ou0, ev0 = rv0 + ov0;
            float eu1 = ru1 + ou1, ev1 = rv1 + ov1;
            float eu2 = ru2 + ou2, ev2 = rv2 + ov2;

            float x0 = pos[v0 * 3], y0 = pos[v0 * 3 + 1], z0 = pos[v0 * 3 + 2];
            float x1 = pos[v1 * 3], y1 = pos[v1 * 3 + 1], z1 = pos[v1 * 3 + 2];
            float x2 = pos[v2 * 3], y2 = pos[v2 * 3 + 1], z2 = pos[v2 * 3 + 2];

            for (int k = 0; k < 3; k++) {
                int vi = v0 + k;
                float nx = (flags[vi] & FLAG_HAS_NORMAL) != 0 ? normal[vi * 3] : 0.0f;
                float ny = (flags[vi] & FLAG_HAS_NORMAL) != 0 ? normal[vi * 3 + 1] : 1.0f;
                float nz = (flags[vi] & FLAG_HAS_NORMAL) != 0 ? normal[vi * 3 + 2] : 0.0f;
                TangentMath.computeSmoothTangent(
                        nx, ny, nz,
                        x0, y0, z0, eu0, ev0,
                        x1, y1, z1, eu1, ev1,
                        x2, y2, z2, eu2, ev2,
                        scratch);
                tangent[vi * 3] = scratch.x;
                tangent[vi * 3 + 1] = scratch.y;
                tangent[vi * 3 + 2] = scratch.z;
                tangentW[vi] = (byte) Math.round(scratch.w);
            }
        }
    }

    public void render(
            VertexConsumer consumer,
            Matrix4f poseMat,
            Matrix3f normalMat,
            int light,
            int color,
            float uvScaleX,
            float uvScaleY,
            float uvOffX,
            float uvOffY,
            boolean alphaOverrideYzz) {
        if (vertexCount == 0) {
            return;
        }
        if (allTriangles && uvScaleX > 0.0f && uvScaleY > 0.0f) {
            FastEntityVertexWriter.Mode mode = FastEntityVertexWriter.resolveMode(consumer);
            if (mode != FastEntityVertexWriter.Mode.FALLBACK_CONSUMER) {
                boolean format54B = (mode == FastEntityVertexWriter.Mode.FAST_54B);
                short entityId = format54B ? IrisVertexBridge.currentEntityId() : 0;
                short blockEntityId = format54B ? IrisVertexBridge.currentBlockEntityId() : 0;
                short itemId = format54B ? IrisVertexBridge.currentItemId() : 0;

                long scratchPtr = FastEntityVertexWriter.primaryScratchPtr();
                int maxTrisPerChunk = FastEntityVertexWriter.SCRATCH_MAX_VERTS / 3;
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    for (int t = 0; t < triCount; t += maxTrisPerChunk) {
                        int batchTris = Math.min(maxTrisPerChunk, triCount - t);
                        int verts = writeTrianglesToBuffer(
                                t,
                                batchTris,
                                poseMat,
                                normalMat,
                                light,
                                color,
                                uvScaleX,
                                uvScaleY,
                                uvOffX,
                                uvOffY,
                                alphaOverrideYzz,
                                format54B,
                                entityId,
                                blockEntityId,
                                itemId,
                                scratchPtr);
                        FastEntityVertexWriter.pushScratch(consumer, stack, scratchPtr, verts, format54B);
                    }
                }
                return;
            }
        }
        renderFallback(consumer, poseMat, normalMat, light, color, uvScaleX, uvScaleY, uvOffX, uvOffY, alphaOverrideYzz);
    }

    public int writeTrianglesToBuffer(
            int startTri,
            int countTris,
            Matrix4f poseMat,
            Matrix3f normalMat,
            int light,
            int colorArgb,
            float uvScaleX,
            float uvScaleY,
            float uvOffX,
            float uvOffY,
            boolean alphaOverrideYzz,
            boolean format54B,
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

        int baseAbgr = EntityVertexLayout.argbToAbgr(colorArgb);
        int zeroAlphaAbgr = EntityVertexLayout.replaceAbgrAlpha(baseAbgr, 0);
        boolean identityUv =
                uvScaleX == 1.0f && uvScaleY == 1.0f && uvOffX == 0.0f && uvOffY == 0.0f;
        float handednessSign = format54B ? (TangentMath.determinant3x3(poseMat) < 0.0f ? -1.0f : 1.0f) : 1.0f;

        long stride = format54B ? EntityVertexLayout.STRIDE_54B : EntityVertexLayout.STRIDE_36B;
        long ptr = dstBasePtr;
        int endTri = startTri + countTris;

        for (int t = startTri; t < endTri; t++) {
            float midU = 0.0f, midV = 0.0f;
            if (format54B) {
                int mBase = t * 2;
                if (identityUv) {
                    midU = triRawMidUv[mBase] + triMidUvOffset[mBase];
                    midV = triRawMidUv[mBase + 1] + triMidUvOffset[mBase + 1];
                } else {
                    int v0 = t * 3;
                    int uvCount = (((flags[v0] & FLAG_HAS_UV) != 0) ? 1 : 0)
                            + (((flags[v0 + 1] & FLAG_HAS_UV) != 0) ? 1 : 0)
                            + (((flags[v0 + 2] & FLAG_HAS_UV) != 0) ? 1 : 0);
                    float offScale = uvCount / 3.0f;
                    midU = triRawMidUv[mBase] * uvScaleX + uvOffX * offScale + triMidUvOffset[mBase];
                    midV = triRawMidUv[mBase + 1] * uvScaleY + uvOffY * offScale + triMidUvOffset[mBase + 1];
                }
            }

            int vStart = t * 3;
            for (int k = 0; k < 3; k++) {
                int vi = vStart + k;
                int pBase = vi * 3;
                float lx = pos[pBase];
                float ly = pos[pBase + 1];
                float lz = pos[pBase + 2];
                float wx = m00 * lx + m10 * ly + m20 * lz + m30;
                float wy = m01 * lx + m11 * ly + m21 * lz + m31;
                float wz = m02 * lx + m12 * ly + m22 * lz + m32;

                byte flag = flags[vi];
                int abgr = (alphaOverrideYzz && (flag & FLAG_Y_IS_ZERO) != 0) ? zeroAlphaAbgr : baseAbgr;

                float u, v;
                if ((flag & FLAG_HAS_UV) != 0) {
                    int uvBase = vi * 2;
                    if (identityUv) {
                        u = uv[uvBase] + uvOffset[uvBase];
                        v = uv[uvBase + 1] + uvOffset[uvBase + 1];
                    } else {
                        u = uv[uvBase] * uvScaleX + uvOffX + uvOffset[uvBase];
                        v = uv[uvBase + 1] * uvScaleY + uvOffY + uvOffset[uvBase + 1];
                    }
                } else {
                    u = 0.0f;
                    v = 0.0f;
                }

                float wnx = 0.0f, wny = 1.0f, wnz = 0.0f;
                int packedNormal = 0;
                if ((flag & FLAG_HAS_NORMAL) != 0) {
                    float lnx = normal[pBase];
                    float lny = normal[pBase + 1];
                    float lnz = normal[pBase + 2];
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
                    packedNormal = EntityVertexLayout.packNormI8(wnx, wny, wnz);
                }

                MemoryUtil.memPutFloat(ptr, wx);
                MemoryUtil.memPutFloat(ptr + 4L, wy);
                MemoryUtil.memPutFloat(ptr + 8L, wz);
                MemoryUtil.memPutInt(ptr + 12L, abgr);
                MemoryUtil.memPutFloat(ptr + 16L, u);
                MemoryUtil.memPutFloat(ptr + 20L, v);
                MemoryUtil.memPutInt(ptr + 24L, OverlayTexture.NO_OVERLAY);
                MemoryUtil.memPutInt(ptr + 28L, light);
                MemoryUtil.memPutInt(ptr + 32L, packedNormal);

                if (format54B) {
                    int packedTangent = TangentMath.transformAndPackSmoothTangent(
                            poseMat,
                            tangent[pBase],
                            tangent[pBase + 1],
                            tangent[pBase + 2],
                            tangentW[vi],
                            wnx,
                            wny,
                            wnz,
                            handednessSign);
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
        return countTris * 3;
    }

    private void renderFallback(
            VertexConsumer consumer,
            Matrix4f poseMat,
            Matrix3f normalMat,
            int light,
            int color,
            float uvScaleX,
            float uvScaleY,
            float uvOffX,
            float uvOffY,
            boolean alphaOverrideYzz) {
        int r = FastColor.ARGB32.red(color);
        int g = FastColor.ARGB32.green(color);
        int b = FastColor.ARGB32.blue(color);
        int baseAlpha = FastColor.ARGB32.alpha(color);
        boolean identityUv =
                uvScaleX == 1.0f && uvScaleY == 1.0f && uvOffX == 0.0f && uvOffY == 0.0f;

        float n00 = normalMat.m00(), n01 = normalMat.m01(), n02 = normalMat.m02();
        float n10 = normalMat.m10(), n11 = normalMat.m11(), n12 = normalMat.m12();
        float n20 = normalMat.m20(), n21 = normalMat.m21(), n22 = normalMat.m22();

        for (int i = 0; i < vertexCount; i++) {
            int pBase = i * 3;
            byte flag = flags[i];
            consumer.addVertex(poseMat, pos[pBase], pos[pBase + 1], pos[pBase + 2]);
            int alpha = (alphaOverrideYzz && (flag & FLAG_Y_IS_ZERO) != 0) ? 0 : baseAlpha;
            consumer.setColor(r, g, b, alpha);

            if ((flag & FLAG_HAS_UV) != 0) {
                int uvBase = i * 2;
                float u = identityUv
                        ? (uv[uvBase] + uvOffset[uvBase])
                        : (uv[uvBase] * uvScaleX + uvOffX + uvOffset[uvBase]);
                float v = identityUv
                        ? (uv[uvBase + 1] + uvOffset[uvBase + 1])
                        : (uv[uvBase + 1] * uvScaleY + uvOffY + uvOffset[uvBase + 1]);
                consumer.setUv(u, v);
            } else {
                consumer.setUv(0.0f, 0.0f);
            }

            consumer.setOverlay(OverlayTexture.NO_OVERLAY);
            consumer.setLight(light);

            if ((flag & FLAG_HAS_NORMAL) != 0) {
                float lnx = normal[pBase];
                float lny = normal[pBase + 1];
                float lnz = normal[pBase + 2];
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
                consumer.setNormal(wnx, wny, wnz);
            }
        }
    }
}
