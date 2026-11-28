package dev.rinchan.tapetum.geometry;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.rinchan.tapetum.vertex.EntityVertexLayout;
import dev.rinchan.tapetum.vertex.TangentMath;
import java.util.Arrays;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class BakedQuadMesh {
    public static final BakedQuadMesh EMPTY =
            new BakedQuadMesh(0, new float[0], new float[0], new float[0], new float[0], new float[0], new byte[0]);

    public final int quadCount;
    public final float[] pos;
    public final float[] uv;
    public final float[] midUv;
    public final float[] normal;
    public final float[] tangent;
    public final byte[] tangentW;

    public BakedQuadMesh(
            int quadCount,
            float[] pos,
            float[] uv,
            float[] midUv,
            float[] normal,
            float[] tangent,
            byte[] tangentW) {
        this.quadCount = quadCount;
        this.pos = pos;
        this.uv = uv;
        this.midUv = midUv;
        this.normal = normal;
        this.tangent = tangent;
        this.tangentW = tangentW;
    }

    public boolean isEmpty() {
        return quadCount == 0;
    }

    public int vertexCount() {
        return quadCount * 4;
    }

    public static Builder builder() {
        return SodiumBuilderFactory.create();
    }

    public static Builder plainBuilder() {
        return new Builder();
    }

    public static class Builder implements VertexConsumer {
        private float[] pos = new float[192];
        private float[] uv = new float[128];
        private float[] midUv = new float[32];
        private float[] normal = new float[48];
        private float[] tangent = new float[48];
        private byte[] tangentW = new byte[16];
        private int quadCount;

        private final float[] qx = new float[4];
        private final float[] qy = new float[4];
        private final float[] qz = new float[4];
        private final float[] qu = new float[4];
        private final float[] qv = new float[4];
        private float qnx, qny = 1.0f, qnz;
        private int staged;
        private final Vector4f tangentScratch = new Vector4f();

        public void addQuad(
                float x0, float y0, float z0, float u0, float v0,
                float x1, float y1, float z1, float u1, float v1,
                float x2, float y2, float z2, float u2, float v2,
                float x3, float y3, float z3, float u3, float v3,
                float nx, float ny, float nz) {
            float nLenSq = nx * nx + ny * ny + nz * nz;
            if (nLenSq > 1e-12f && Math.abs(nLenSq - 1.0f) > 1e-4f) {
                float inv = (float) (1.0 / Math.sqrt(nLenSq));
                nx *= inv;
                ny *= inv;
                nz *= inv;
            }
            ensureCapacity(quadCount + 1);
            int pBase = quadCount * 12;
            pos[pBase] = x0; pos[pBase + 1] = y0; pos[pBase + 2] = z0;
            pos[pBase + 3] = x1; pos[pBase + 4] = y1; pos[pBase + 5] = z1;
            pos[pBase + 6] = x2; pos[pBase + 7] = y2; pos[pBase + 8] = z2;
            pos[pBase + 9] = x3; pos[pBase + 10] = y3; pos[pBase + 11] = z3;

            int uvBase = quadCount * 8;
            uv[uvBase] = u0; uv[uvBase + 1] = v0;
            uv[uvBase + 2] = u1; uv[uvBase + 3] = v1;
            uv[uvBase + 4] = u2; uv[uvBase + 5] = v2;
            uv[uvBase + 6] = u3; uv[uvBase + 7] = v3;

            int mBase = quadCount * 2;
            midUv[mBase] = (u0 + u1 + u2 + u3) * 0.25f;
            midUv[mBase + 1] = (v0 + v1 + v2 + v3) * 0.25f;

            int nBase = quadCount * 3;
            normal[nBase] = nx; normal[nBase + 1] = ny; normal[nBase + 2] = nz;

            TangentMath.computeFlatTangent(
                    nx, ny, nz,
                    x0, y0, z0, u0, v0,
                    x1, y1, z1, u1, v1,
                    x2, y2, z2, u2, v2,
                    tangentScratch);
            tangent[nBase] = tangentScratch.x;
            tangent[nBase + 1] = tangentScratch.y;
            tangent[nBase + 2] = tangentScratch.z;
            tangentW[quadCount] = (byte) Math.round(tangentScratch.w);
            quadCount++;
        }

        private void ensureCapacity(int needed) {
            if (needed <= tangentW.length) {
                return;
            }
            int next = Math.max(needed, tangentW.length * 2);
            pos = Arrays.copyOf(pos, next * 12);
            uv = Arrays.copyOf(uv, next * 8);
            midUv = Arrays.copyOf(midUv, next * 2);
            normal = Arrays.copyOf(normal, next * 3);
            tangent = Arrays.copyOf(tangent, next * 3);
            tangentW = Arrays.copyOf(tangentW, next);
        }

        public BakedQuadMesh build() {
            if (quadCount == 0) {
                return EMPTY;
            }
            MeshCacheRegistry.recordBakedMesh();
            return new BakedQuadMesh(
                    quadCount,
                    Arrays.copyOf(pos, quadCount * 12),
                    Arrays.copyOf(uv, quadCount * 8),
                    Arrays.copyOf(midUv, quadCount * 2),
                    Arrays.copyOf(normal, quadCount * 3),
                    Arrays.copyOf(tangent, quadCount * 3),
                    Arrays.copyOf(tangentW, quadCount));
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            qx[staged] = (float) x;
            qy[staged] = (float) y;
            qz[staged] = (float) z;
            qu[staged] = 0.0f;
            qv[staged] = 0.0f;
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            qu[staged] = u;
            qv[staged] = v;
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float nx, float ny, float nz) {
            if (staged == 0) {
                qnx = nx;
                qny = ny;
                qnz = nz;
            }
            return this;
        }

        @Override
        public void endVertex() {
            staged++;
            if (staged == 4) {
                addQuad(
                        qx[0], qy[0], qz[0], qu[0], qv[0],
                        qx[1], qy[1], qz[1], qu[1], qv[1],
                        qx[2], qy[2], qz[2], qu[2], qv[2],
                        qx[3], qy[3], qz[3], qu[3], qv[3],
                        qnx, qny, qnz);
                staged = 0;
            }
        }

        public void captureOffHeapQuads(long ptr, int count, int stride) {
            if (count <= 0 || (stride != EntityVertexLayout.STRIDE_36B && stride != EntityVertexLayout.STRIDE_54B)) {
                return;
            }
            int quads = count / 4;
            for (int q = 0; q < quads; q++) {
                long p0 = ptr + (long) (q * 4) * stride;
                long p1 = p0 + stride;
                long p2 = p1 + stride;
                long p3 = p2 + stride;
                float x0 = MemoryUtil.memGetFloat(p0), y0 = MemoryUtil.memGetFloat(p0 + 4L), z0 = MemoryUtil.memGetFloat(p0 + 8L);
                float x1 = MemoryUtil.memGetFloat(p1), y1 = MemoryUtil.memGetFloat(p1 + 4L), z1 = MemoryUtil.memGetFloat(p1 + 8L);
                float x2 = MemoryUtil.memGetFloat(p2), y2 = MemoryUtil.memGetFloat(p2 + 4L), z2 = MemoryUtil.memGetFloat(p2 + 8L);
                float x3 = MemoryUtil.memGetFloat(p3), y3 = MemoryUtil.memGetFloat(p3 + 4L), z3 = MemoryUtil.memGetFloat(p3 + 8L);
                int packedNormal = MemoryUtil.memGetInt(p0 + EntityVertexLayout.OFFSET_NORMAL);
                float nx = EntityVertexLayout.unpackNormX(packedNormal);
                float ny = EntityVertexLayout.unpackNormY(packedNormal);
                float nz = EntityVertexLayout.unpackNormZ(packedNormal);
                if (nx * nx + ny * ny + nz * nz <= 1e-12f) {
                    float e1x = x1 - x0, e1y = y1 - y0, e1z = z1 - z0;
                    float e2x = x2 - x0, e2y = y2 - y0, e2z = z2 - z0;
                    nx = e1y * e2z - e1z * e2y;
                    ny = e1z * e2x - e1x * e2z;
                    nz = e1x * e2y - e1y * e2x;
                    if (nx * nx + ny * ny + nz * nz <= 1e-12f) {
                        float e3x = x3 - x0, e3y = y3 - y0, e3z = z3 - z0;
                        nx = e2y * e3z - e2z * e3y;
                        ny = e2z * e3x - e2x * e3z;
                        nz = e2x * e3y - e2y * e3x;
                    }
                }
                addQuad(
                        x0, y0, z0, MemoryUtil.memGetFloat(p0 + 16L), MemoryUtil.memGetFloat(p0 + 20L),
                        x1, y1, z1, MemoryUtil.memGetFloat(p1 + 16L), MemoryUtil.memGetFloat(p1 + 20L),
                        x2, y2, z2, MemoryUtil.memGetFloat(p2 + 16L), MemoryUtil.memGetFloat(p2 + 20L),
                        x3, y3, z3, MemoryUtil.memGetFloat(p3 + 16L), MemoryUtil.memGetFloat(p3 + 20L),
                        nx, ny, nz);
            }
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {}

        @Override
        public void unsetDefaultColor() {}
    }

    private static final class SodiumBuilderFactory {
        private static final boolean SODIUM_PRESENT = detectSodium();

        static Builder create() {
            return SODIUM_PRESENT ? SodiumBuilderHolder.create() : new Builder();
        }

        private static boolean detectSodium() {
            try {
                Class.forName(
                        "net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter",
                        false,
                        BakedQuadMesh.class.getClassLoader());
                return true;
            } catch (ClassNotFoundException | LinkageError ignored) {
                return false;
            }
        }
    }

    private static final class SodiumBuilderHolder {
        static Builder create() {
            return new SodiumCaptureBuilder();
        }
    }

    private static final class SodiumCaptureBuilder extends Builder implements VertexBufferWriter {
        @Override
        public boolean canUseIntrinsics() {
            return true;
        }

        @Override
        public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
            if (count <= 0 || format == null) {
                return;
            }
            captureOffHeapQuads(ptr, count, format.stride());
        }
    }
}
