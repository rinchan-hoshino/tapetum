package dev.rinchan.tapetum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rinchan.tapetum.geometry.BakedQuadMesh;
import dev.rinchan.tapetum.vertex.EntityVertexLayout;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import dev.rinchan.tapetum.vertex.TangentMath;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryUtil;

final class FastEntityVertexWriterParityTest {
    @Test
    void flatTangentProducesExpectedOrthonormalBasisAndDegenerateFallback() {
        Vector4f out = new Vector4f();

        // Front quad (+Z normal, +X along +U, +Y along +V -> tangent = (+1, 0, 0), w = +1)
        TangentMath.computeFlatTangent(
                0f, 0f, 1f,
                0f, 0f, 0f, 0f, 0f,
                1f, 0f, 0f, 1f, 0f,
                1f, 1f, 0f, 1f, 1f,
                out);
        assertEquals(1.0f, out.x, 1e-4f);
        assertEquals(0.0f, out.y, 1e-4f);
        assertEquals(0.0f, out.z, 1e-4f);
        assertEquals(1.0f, Math.abs(out.w), 1e-4f);

        // Tilted triangle lying in plane y + z = 0.2: tangent must be unit-length and orthogonal to face normal
        float nx = 0f, ny = 0.70710677f, nz = 0.70710677f;
        TangentMath.computeFlatTangent(
                nx, ny, nz,
                -0.5f, 0f, 0.2f, 0.125f, 0.25f,
                0.5f, 0.3f, -0.1f, 0.625f, 0.25f,
                0.1f, 0.8f, -0.6f, 0.375f, 0.75f,
                out);
        float len = (float) Math.sqrt(out.x * out.x + out.y * out.y + out.z * out.z);
        assertEquals(1.0f, len, 1e-4f);
        assertEquals(0.0f, out.x * nx + out.y * ny + out.z * nz, 1e-4f);
        assertEquals(1.0f, Math.abs(out.w), 1e-4f);

        // Degenerate UVs -> w == 0.0f
        TangentMath.computeFlatTangent(
                0f, 1f, 0f,
                0f, 0f, 0f, 0.5f, 0.5f,
                1f, 0f, 0f, 0.5f, 0.5f,
                0f, 0f, 1f, 0.5f, 0.5f,
                out);
        assertEquals(0.0f, out.w, 1e-6f);
    }

    @Test
    void smoothTangentProducesUnitOrthogonalTangent() {
        float nx = 0.26726124f, ny = 0.5345225f, nz = 0.8017837f;
        Vector4f out = new Vector4f();
        TangentMath.computeSmoothTangent(
                nx, ny, nz,
                0.0f, 0.1f, 0.3f, 0.0f, 0.0f,
                1.0f, -0.2f, 0.4f, 0.5f, 0.25f,
                0.25f, 0.9f, -0.1f, 0.25f, 0.75f,
                out);
        float len = (float) Math.sqrt(out.x * out.x + out.y * out.y + out.z * out.z);
        assertEquals(1.0f, len, 1e-4f);
        assertEquals(0.0f, out.x * nx + out.y * ny + out.z * nz, 1e-4f);
        assertTrue(out.w == 1.0f || out.w == -1.0f);
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
        int expectedNormal = EntityVertexLayout.packNormI8(1f, 0f, 0f);

        long buf54 = MemoryUtil.nmemAlloc(4L * EntityVertexLayout.STRIDE_54B);
        long buf36 = MemoryUtil.nmemAlloc(4L * EntityVertexLayout.STRIDE_36B);
        try {
            int written54 = FastEntityVertexWriter.writeQuadsToBuffer(
                    mesh, 0, 1, pose, normMat,
                    0x00F000F0, 0x000A0000, 0xFF112233,
                    true, (short) 12, (short) 34, (short) 56,
                    buf54);
            assertEquals(4, written54);

            long v1 = buf54 + EntityVertexLayout.STRIDE_54B;
            assertEquals(2f, MemoryUtil.memGetFloat(v1), 1e-5f);
            assertEquals(3f, MemoryUtil.memGetFloat(v1 + 4L), 1e-5f);
            assertEquals(-5f, MemoryUtil.memGetFloat(v1 + 8L), 1e-5f);
            assertEquals(0xFF112233, MemoryUtil.memGetInt(v1 + 12L));
            assertEquals(0.5f, MemoryUtil.memGetFloat(v1 + 16L), 1e-6f);
            assertEquals(0.0f, MemoryUtil.memGetFloat(v1 + 20L), 1e-6f);
            assertEquals(0x000A0000, MemoryUtil.memGetInt(v1 + 24L));
            assertEquals(0x00F000F0, MemoryUtil.memGetInt(v1 + 28L));
            assertEquals(expectedNormal, MemoryUtil.memGetInt(v1 + 32L));
            assertEquals((short) 12, MemoryUtil.memGetShort(v1 + 36L));
            assertEquals((short) 34, MemoryUtil.memGetShort(v1 + 38L));
            assertEquals((short) 56, MemoryUtil.memGetShort(v1 + 40L));
            assertEquals(0.25f, MemoryUtil.memGetFloat(v1 + 42L), 1e-6f);
            assertEquals(0.25f, MemoryUtil.memGetFloat(v1 + 46L), 1e-6f);
            assertNotEquals(0, MemoryUtil.memGetInt(v1 + 50L));

            int written36 = FastEntityVertexWriter.writeQuadsToBuffer(
                    mesh, 0, 1, pose, normMat,
                    0x00F000F0, 0x000A0000, 0xFF112233,
                    false, (short) 0, (short) 0, (short) 0,
                    buf36);
            assertEquals(4, written36);
            long v1_36 = buf36 + EntityVertexLayout.STRIDE_36B;
            assertEquals(expectedNormal, MemoryUtil.memGetInt(v1_36 + 32L));
        } finally {
            MemoryUtil.nmemFree(buf54);
            MemoryUtil.nmemFree(buf36);
        }
    }
}
