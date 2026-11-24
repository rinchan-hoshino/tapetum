package dev.rinchan.tapetum.vertex;

import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Tangent & handedness math aligned bit-for-bit with Iris {@code NormalHelper.computeTangent}
 * and {@code NormalHelper.computeTangentSmooth}, plus affine transform helpers for pre-baked meshes.
 */
public final class TangentMath {
    private TangentMath() {}

    public static void computeFlatTangent(
            float nx, float ny, float nz,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            Vector4f out) {
        float e1x = x1 - x0;
        float e1y = y1 - y0;
        float e1z = z1 - z0;

        float e2x = x2 - x0;
        float e2y = y2 - y0;
        float e2z = z2 - z0;

        float du1 = u1 - u0;
        float dv1 = v1 - v0;
        float du2 = u2 - u0;
        float dv2 = v2 - v0;

        float denom = du1 * dv2 - du2 * dv1;
        float f = ((double) denom == 0.0) ? 1.0f : (1.0f / denom);

        float tx = f * (dv2 * e1x - dv1 * e2x);
        float ty = f * (dv2 * e1y - dv1 * e2y);
        float tz = f * (dv2 * e1z - dv1 * e2z);
        float tInv = rsqrt(tx * tx + ty * ty + tz * tz);
        tx *= tInv;
        ty *= tInv;
        tz *= tInv;

        if (tx == 0.0f && ty == 0.0f && tz == 0.0f) {
            out.set(0.0f, 0.0f, 0.0f, 0.0f);
            return;
        }

        float bx = f * (-du2 * e1x + du1 * e2x);
        float by = f * (-du2 * e1y + du1 * e2y);
        float bz = f * (-du2 * e1z + du1 * e2z);
        float bInv = rsqrt(bx * bx + by * by + bz * bz);
        bx *= bInv;
        by *= bInv;
        bz *= bInv;

        float cx = ty * nz - tz * ny;
        float cy = tz * nx - tx * nz;
        float cz = tx * ny - ty * nx;
        float dot = bx * cx + by * cy + bz * cz;
        float w = dot < 0.0f ? -1.0f : 1.0f;
        out.set(tx, ty, tz, w);
    }

    public static void computeSmoothTangent(
            float nx, float ny, float nz,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            Vector4f out) {
        float d0 = x0 * nx + y0 * ny + z0 * nz;
        float d1 = x1 * nx + y1 * ny + z1 * nz;
        float d2 = x2 * nx + y2 * ny + z2 * nz;

        computeFlatTangent(
                nx, ny, nz,
                x0 - d0 * nx, y0 - d0 * ny, z0 - d0 * nz, u0, v0,
                x1 - d1 * nx, y1 - d1 * ny, z1 - d1 * nz, u1, v1,
                x2 - d2 * nx, y2 - d2 * ny, z2 - d2 * nz, u2, v2,
                out);
    }

    public static float determinant3x3(Matrix4f pose) {
        float m00 = pose.m00(), m01 = pose.m01(), m02 = pose.m02();
        float m10 = pose.m10(), m11 = pose.m11(), m12 = pose.m12();
        float m20 = pose.m20(), m21 = pose.m21(), m22 = pose.m22();
        return m00 * (m11 * m22 - m12 * m21)
                - m10 * (m01 * m22 - m02 * m21)
                + m20 * (m01 * m12 - m02 * m11);
    }

    public static int transformAndPackTangent(
            Matrix4f pose, float tx, float ty, float tz, byte localW, float handednessSign) {
        if (localW == 0) {
            return EntityVertexLayout.DEGENERATE_TANGENT;
        }
        float wx = pose.m00() * tx + pose.m10() * ty + pose.m20() * tz;
        float wy = pose.m01() * tx + pose.m11() * ty + pose.m21() * tz;
        float wz = pose.m02() * tx + pose.m12() * ty + pose.m22() * tz;
        float lenSq = wx * wx + wy * wy + wz * wz;
        if (lenSq == 0.0f) {
            return EntityVertexLayout.DEGENERATE_TANGENT;
        }
        if (Math.abs(lenSq - 1.0f) > 1e-4f) {
            float inv = (float) (1.0 / Math.sqrt(lenSq));
            wx *= inv;
            wy *= inv;
            wz *= inv;
        }
        return EntityVertexLayout.packNormI8(wx, wy, wz, localW * handednessSign);
    }

    public static int transformAndPackSmoothTangent(
            Matrix4f pose,
            float tx, float ty, float tz, byte localW,
            float wnx, float wny, float wnz,
            float handednessSign) {
        if (localW == 0) {
            return EntityVertexLayout.DEGENERATE_TANGENT;
        }
        float wx = pose.m00() * tx + pose.m10() * ty + pose.m20() * tz;
        float wy = pose.m01() * tx + pose.m11() * ty + pose.m21() * tz;
        float wz = pose.m02() * tx + pose.m12() * ty + pose.m22() * tz;
        float dot = wx * wnx + wy * wny + wz * wnz;
        wx -= dot * wnx;
        wy -= dot * wny;
        wz -= dot * wnz;
        float lenSq = wx * wx + wy * wy + wz * wz;
        if (lenSq == 0.0f) {
            return EntityVertexLayout.DEGENERATE_TANGENT;
        }
        if (Math.abs(lenSq - 1.0f) > 1e-4f) {
            float inv = (float) (1.0 / Math.sqrt(lenSq));
            wx *= inv;
            wy *= inv;
            wz *= inv;
        }
        return EntityVertexLayout.packNormI8(wx, wy, wz, localW * handednessSign);
    }

    public static float rsqrt(float value) {
        return value == 0.0f ? 1.0f : (float) (1.0 / Math.sqrt(value));
    }
}
