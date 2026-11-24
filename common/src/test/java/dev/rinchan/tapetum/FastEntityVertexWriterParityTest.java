package dev.rinchan.tapetum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import dev.rinchan.tapetum.geometry.BakedQuadMesh;
import dev.rinchan.tapetum.vertex.EntityVertexLayout;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import dev.rinchan.tapetum.vertex.TangentMath;
import net.irisshaders.iris.vertices.NormalHelper;
import net.irisshaders.iris.vertices.views.TriView;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryUtil;

final class FastEntityVertexWriterParityTest {
    @Test
    void flatTangentMatchesIrisNormalHelperBitForBit() {
        float[][] cases = {
            // Front quad (+Z normal)
            {0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f, 1f},
            // Tilted triangle
            {0f, 0.70710677f, 0.70710677f, -0.5f, 0f, 0.25f, 0.125f, 0.25f, 0.5f, 0.3f, -0.1f, 0.625f, 0.25f, 0.1f, 0.8f, -0.6f, 0.375f, 0.75f},
            // Degenerate UVs
            {0f, 1f, 0f, 0f, 0f, 0f, 0.5f, 0.5f, 1f, 0f, 0f, 0.5f, 0.5f, 0f, 0f, 1f, 0.5f, 0.5f},
        };

        Vector4f out = new Vector4f();
        for (float[] c : cases) {
            int irisPacked = NormalHelper.computeTangent(
                    c[0], c[1], c[2],
                    c[3], c[4], c[5], c[6], c[7],
                    c[8], c[9], c[10], c[11], c[12],
                    c[13], c[14], c[15], c[16], c[17]);

            TangentMath.computeFlatTangent(
                    c[0], c[1], c[2],
                    c[3], c[4], c[5], c[6], c[7],
                    c[8], c[9], c[10], c[11], c[12],
                    c[13], c[14], c[15], c[16], c[17],
                    out);
            int tapetumPacked = out.w == 0.0f
                    ? EntityVertexLayout.DEGENERATE_TANGENT
                    : EntityVertexLayout.packNormI8(out.x, out.y, out.z, out.w);
            assertEquals(irisPacked, tapetumPacked);
        }
    }

    @Test
    void smoothTangentMatchesIrisNormalHelperBitForBit() {
        float nx = 0.26726124f, ny = 0.5345225f, nz = 0.8017837f;
        float[] x = {0.0f, 1.0f, 0.25f};
        float[] y = {0.1f, -0.2f, 0.9f};
        float[] z = {0.3f, 0.4f, -0.1f};
        float[] u = {0.0f, 0.5f, 0.25f};
        float[] v = {0.0f, 0.25f, 0.75f};

        TriView tri = new TriView() {
            @Override
            public float x(int i) { return x[i]; }
            @Override
            public float y(int i) { return y[i]; }
            @Override
            public float z(int i) { return z[i]; }
            @Override
            public float u(int i) { return u[i]; }
            @Override
            public float v(int i) { return v[i]; }
        };

        int irisPacked = NormalHelper.computeTangentSmooth(nx, ny, nz, tri);
        Vector4f out = new Vector4f();
        TangentMath.computeSmoothTangent(
                nx, ny, nz,
                x[0], y[0], z[0], u[0], v[0],
                x[1], y[1], z[1], u[1], v[1],
                x[2], y[2], z[2], u[2], v[2],
                out);
        int tapetumPacked = out.w == 0.0f
                ? EntityVertexLayout.DEGENERATE_TANGENT
                : EntityVertexLayout.packNormI8(out.x, out.y, out.z, out.w);
        assertEquals(irisPacked, tapetumPacked);
    }

    @Test
    void writeQuadsToBufferWritesExact54BAnd36BLayouts() {
        float[] pos = {
            0f, 0f, 0f,
            1f, 0f, 0f,
            1f, 1f, 0f,
            0f, 1f, 0f
        };
        float[] uv = {
            0f, 0f,
            0.5f, 0f,
            0.5f, 0.5f,
            0f, 0.5f
        };
        float[] midUv = {0.25f, 0.25f};
        float[] normal = {0f, 0f, 1f};
        Vector4f tanOut = new Vector4f();
        TangentMath.computeFlatTangent(
                0f, 0f, 1f,
                0f, 0f, 0f, 0f, 0f,
                1f, 0f, 0f, 0.5f, 0f,
                1f, 1f, 0f, 0.5f, 0.5f,
                tanOut);
        float[] tangent = {tanOut.x, tanOut.y, tanOut.z};
        byte[] tangentW = {(byte) tanOut.w};

        BakedQuadMesh mesh = new BakedQuadMesh(1, pos, uv, midUv, normal, tangent, tangentW);

        Matrix4f pose = new Matrix4f().translate(2f, 3f, -4f).rotateY((float) (Math.PI / 2.0));
        Matrix3f normMat = new Matrix3f(pose).invert().transpose();

        long buf54 = MemoryUtil.nmemAlloc(4L * EntityVertexLayout.STRIDE_54B);
        long buf36 = MemoryUtil.nmemAlloc(4L * EntityVertexLayout.STRIDE_36B);
        try {
            int written54 = FastEntityVertexWriter.writeQuadsToBuffer(
                    mesh, 0, 1, pose, normMat,
                    0x00F000F0, 0x000A0000, 0xFF112233,
                    true, (short) 12, (short) 34, (short) 56,
                    false, 0f, 0f, 0f, buf54);
            assertEquals(4, written54);

            // Check vertex 1 in 54B buffer
            long v1 = buf54 + EntityVertexLayout.STRIDE_54B;
            assertEquals(2f, MemoryUtil.memGetFloat(v1), 1e-5f);
            assertEquals(3f, MemoryUtil.memGetFloat(v1 + 4L), 1e-5f);
            assertEquals(-5f, MemoryUtil.memGetFloat(v1 + 8L), 1e-5f);
            assertEquals(0xFF112233, MemoryUtil.memGetInt(v1 + 12L));
            assertEquals(0.5f, MemoryUtil.memGetFloat(v1 + 16L), 1e-6f);
            assertEquals(0.0f, MemoryUtil.memGetFloat(v1 + 20L), 1e-6f);
            assertEquals(0x000A0000, MemoryUtil.memGetInt(v1 + 24L));
            assertEquals(0x00F000F0, MemoryUtil.memGetInt(v1 + 28L));
            assertEquals(net.caffeinemc.mods.sodium.api.util.NormI8.pack(1f, 0f, 0f), MemoryUtil.memGetInt(v1 + 32L));
            assertEquals((short) 12, MemoryUtil.memGetShort(v1 + 36L));
            assertEquals((short) 34, MemoryUtil.memGetShort(v1 + 38L));
            assertEquals((short) 56, MemoryUtil.memGetShort(v1 + 40L));
            assertEquals(0.25f, MemoryUtil.memGetFloat(v1 + 42L), 1e-6f);
            assertEquals(0.25f, MemoryUtil.memGetFloat(v1 + 46L), 1e-6f);
            assertNotEquals(0, MemoryUtil.memGetInt(v1 + 50L));

            // Check 36B buffer
            int written36 = FastEntityVertexWriter.writeQuadsToBuffer(
                    mesh, 0, 1, pose, normMat,
                    0x00F000F0, 0x000A0000, 0xFF112233,
                    false, (short) 0, (short) 0, (short) 0,
                    false, 0f, 0f, 0f, buf36);
            assertEquals(4, written36);
            long v1_36 = buf36 + EntityVertexLayout.STRIDE_36B;
            assertEquals(net.caffeinemc.mods.sodium.api.util.NormI8.pack(1f, 0f, 0f), MemoryUtil.memGetInt(v1_36 + 32L));
        } finally {
            MemoryUtil.nmemFree(buf54);
            MemoryUtil.nmemFree(buf36);
        }
    }
}
