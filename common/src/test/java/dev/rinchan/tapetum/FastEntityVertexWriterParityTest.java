package dev.rinchan.tapetum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.rinchan.tapetum.geometry.BakedQuadMesh;
import dev.rinchan.tapetum.vertex.EntityVertexLayout;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import dev.rinchan.tapetum.vertex.TangentMath;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
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

    @Test
    void vanillaModelPartCube54BMatchesReferenceVertexConsumer() {
        ModelPart.Cube cube = new ModelPart.Cube(
                0, 0, -4f, 0f, -2f, 8f, 12f, 4f,
                0f, 0f, 0f, false, 64f, 64f,
                EnumSet.allOf(Direction.class));

        BakedQuadMesh.Builder baker = BakedQuadMesh.plainBuilder();
        cube.compile(new PoseStack().last(), baker, 0x00F000F0, 0x000A0000, -1);
        BakedQuadMesh mesh = baker.build();
        assertEquals(6, mesh.quadCount, "Cube must bake all 6 faces");

        PoseStack poseStack = new PoseStack();
        poseStack.translate(1.5f, -0.5f, 2.25f);
        poseStack.mulPose(new Matrix4f().rotate(new Quaternionf().rotateYXZ(0.65f, -0.35f, 0.2f)));
        PoseStack.Pose pose = poseStack.last();

        int light = 0x00C000E0;
        int overlay = 0x000A0000;
        int argb = 0xFFCC8844;
        int abgr = EntityVertexLayout.argbToAbgr(argb);

        RecordingVertexConsumer ref = new RecordingVertexConsumer();
        cube.compile(pose, ref, light, overlay, argb);
        ref.finish();
        assertEquals(24, ref.verts.size(), "Reference cube compile must emit 24 vertices");

        long buf54 = MemoryUtil.nmemAlloc(24L * EntityVertexLayout.STRIDE_54B);
        try {
            int written = FastEntityVertexWriter.writeQuadsToBuffer(
                    mesh, 0, mesh.quadCount, pose.pose(), pose.normal(),
                    light, overlay, abgr,
                    true, (short) 7, (short) 8, (short) 9,
                    buf54);
            assertEquals(24, written);

            for (int q = 0; q < 6; q++) {
                float expectedMidU = 0f;
                float expectedMidV = 0f;
                for (int k = 0; k < 4; k++) {
                    expectedMidU += ref.verts.get(q * 4 + k).u;
                    expectedMidV += ref.verts.get(q * 4 + k).v;
                }
                expectedMidU *= 0.25f;
                expectedMidV *= 0.25f;

                for (int k = 0; k < 4; k++) {
                    int idx = q * 4 + k;
                    RecordedVertex rv = ref.verts.get(idx);
                    long p = buf54 + (long) idx * EntityVertexLayout.STRIDE_54B;

                    assertEquals(rv.x, MemoryUtil.memGetFloat(p), 1e-4f, "pos.x v" + idx);
                    assertEquals(rv.y, MemoryUtil.memGetFloat(p + 4L), 1e-4f, "pos.y v" + idx);
                    assertEquals(rv.z, MemoryUtil.memGetFloat(p + 8L), 1e-4f, "pos.z v" + idx);

                    int expectedColor = (rv.r & 0xFF)
                            | ((rv.g & 0xFF) << 8)
                            | ((rv.b & 0xFF) << 16)
                            | ((rv.a & 0xFF) << 24);
                    assertEquals(expectedColor, MemoryUtil.memGetInt(p + 12L), "color v" + idx);
                    assertEquals(rv.u, MemoryUtil.memGetFloat(p + 16L), 1e-6f, "u v" + idx);
                    assertEquals(rv.v, MemoryUtil.memGetFloat(p + 20L), 1e-6f, "v v" + idx);
                    assertEquals(rv.overlay, MemoryUtil.memGetInt(p + 24L), "overlay v" + idx);
                    assertEquals(rv.light, MemoryUtil.memGetInt(p + 28L), "light v" + idx);

                    int actualNormal = MemoryUtil.memGetInt(p + 32L);
                    int expectedNormal = EntityVertexLayout.packNormI8(rv.nx, rv.ny, rv.nz);
                    assertNotEquals(0, actualNormal, "Normal must be non-zero on v" + idx);
                    assertEquals(expectedNormal, actualNormal, "normal v" + idx);

                    assertEquals(expectedMidU, MemoryUtil.memGetFloat(p + 42L), 1e-6f, "midU v" + idx);
                    assertEquals(expectedMidV, MemoryUtil.memGetFloat(p + 46L), 1e-6f, "midV v" + idx);

                    int actualTangent = MemoryUtil.memGetInt(p + 50L);
                    assertNotEquals(0, actualTangent, "Tangent must be non-zero on v" + idx);
                    float tx = EntityVertexLayout.unpackNormX(actualTangent);
                    float ty = EntityVertexLayout.unpackNormY(actualTangent);
                    float tz = EntityVertexLayout.unpackNormZ(actualTangent);
                    float tLen = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
                    assertTrue(tLen > 0.9f, "Tangent length must be near 1 on v" + idx + " got " + tLen);
                    assertEquals(0.0f, tx * rv.nx + ty * rv.ny + tz * rv.nz, 0.05f, "Tangent orthogonal to normal v" + idx);
                }
            }
        } finally {
            MemoryUtil.nmemFree(buf54);
        }
    }

    @Test
    void sodiumCaptureBuilderRecoversFaceNormalFromCrossProductWhenZero() {
        BakedQuadMesh.Builder builder = BakedQuadMesh.plainBuilder();
        long buf36 = MemoryUtil.nmemAlloc(4L * EntityVertexLayout.STRIDE_36B);
        try {
            float[][] verts = {
                {0f, 0f, 0f, 0f, 0f},
                {1f, 0f, 0f, 1f, 0f},
                {1f, 1f, 0f, 1f, 1f},
                {0f, 1f, 0f, 0f, 1f}
            };
            for (int i = 0; i < 4; i++) {
                long p = buf36 + (long) i * EntityVertexLayout.STRIDE_36B;
                MemoryUtil.memPutFloat(p, verts[i][0]);
                MemoryUtil.memPutFloat(p + 4L, verts[i][1]);
                MemoryUtil.memPutFloat(p + 8L, verts[i][2]);
                MemoryUtil.memPutInt(p + 12L, -1);
                MemoryUtil.memPutFloat(p + 16L, verts[i][3]);
                MemoryUtil.memPutFloat(p + 20L, verts[i][4]);
                MemoryUtil.memPutInt(p + 24L, 0x000A0000);
                MemoryUtil.memPutInt(p + 28L, 0x00F000F0);
                MemoryUtil.memPutInt(p + 32L, 0); // Intentionally zero packed normal
            }
            builder.captureOffHeapQuads(buf36, 4, EntityVertexLayout.STRIDE_36B);
        } finally {
            MemoryUtil.nmemFree(buf36);
        }

        BakedQuadMesh mesh = builder.build();
        assertEquals(1, mesh.quadCount);
        assertEquals(0.0f, mesh.normal[0], 1e-5f);
        assertEquals(0.0f, mesh.normal[1], 1e-5f);
        assertEquals(1.0f, mesh.normal[2], 1e-5f);
        assertNotEquals(0, mesh.tangentW[0], "Recovered quad must produce valid non-degenerate tangent");
    }

    private static final class RecordedVertex {
        float x, y, z;
        int r, g, b, a;
        float u, v;
        int overlay;
        int light;
        float nx, ny, nz;
    }

    private static final class RecordingVertexConsumer implements VertexConsumer {
        final List<RecordedVertex> verts = new ArrayList<>();
        private RecordedVertex current;

        void finish() {
            if (current != null) {
                verts.add(current);
                current = null;
            }
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            finish();
            current = new RecordedVertex();
            current.x = x;
            current.y = y;
            current.z = z;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            current.r = red;
            current.g = green;
            current.b = blue;
            current.a = alpha;
            return this;
        }

        public VertexConsumer setColor(int argb) {
            return setColor((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            current.u = u;
            current.v = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            current.overlay = (u & 0xFFFF) | ((v & 0xFFFF) << 16);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            current.light = (u & 0xFFFF) | ((v & 0xFFFF) << 16);
            return this;
        }

        public VertexConsumer setUv3(float u, float v) {
            return this;
        }

        public VertexConsumer setLineWidth(float width) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float nx, float ny, float nz) {
            current.nx = nx;
            current.ny = ny;
            current.nz = nz;
            return this;
        }
    }
}
