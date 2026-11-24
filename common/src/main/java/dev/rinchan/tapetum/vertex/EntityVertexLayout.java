package dev.rinchan.tapetum.vertex;

import net.minecraft.util.Mth;

/**
 * Byte offsets and packing helpers for Sodium 36B {@code NEW_ENTITY} and Iris 54B {@code ENTITY}
 * vertex formats (little-endian).
 */
public final class EntityVertexLayout {
    public static final int STRIDE_36B = 36;
    public static final int STRIDE_54B = 54;

    public static final long OFFSET_POSITION = 0L;
    public static final long OFFSET_COLOR = 12L;
    public static final long OFFSET_UV0 = 16L;
    public static final long OFFSET_UV1 = 24L;
    public static final long OFFSET_UV2 = 28L;
    public static final long OFFSET_NORMAL = 32L;

    public static final long OFFSET_IRIS_ENTITY_X = 36L;
    public static final long OFFSET_IRIS_ENTITY_Y = 38L;
    public static final long OFFSET_IRIS_ENTITY_Z = 40L;
    public static final long OFFSET_MID_UV = 42L;
    public static final long OFFSET_TANGENT = 50L;

    /** Sentinel emitted by Iris {@code NormalHelper.computeTangent} for degenerate UV/geometry. */
    public static final int DEGENERATE_TANGENT = -1;

    private EntityVertexLayout() {}

    /**
     * Packs a unit vector into 3 signed bytes matching Sodium/Iris {@code NormI8.pack(x, y, z, 0)}.
     */
    public static int packNormI8(float x, float y, float z) {
        int ix = ((int) (Mth.clamp(x, -1.0f, 1.0f) * 127.0f)) & 0xFF;
        int iy = ((int) (Mth.clamp(y, -1.0f, 1.0f) * 127.0f)) & 0xFF;
        int iz = ((int) (Mth.clamp(z, -1.0f, 1.0f) * 127.0f)) & 0xFF;
        return ix | (iy << 8) | (iz << 16);
    }

    /**
     * Packs a unit tangent + handedness into 4 signed bytes matching Iris {@code NormI8.pack(x, y, z, w)}.
     */
    public static int packNormI8(float x, float y, float z, float w) {
        int ix = ((int) (Mth.clamp(x, -1.0f, 1.0f) * 127.0f)) & 0xFF;
        int iy = ((int) (Mth.clamp(y, -1.0f, 1.0f) * 127.0f)) & 0xFF;
        int iz = ((int) (Mth.clamp(z, -1.0f, 1.0f) * 127.0f)) & 0xFF;
        int iw = ((int) (Mth.clamp(w, -1.0f, 1.0f) * 127.0f)) & 0xFF;
        return ix | (iy << 8) | (iz << 16) | (iw << 24);
    }

    public static float unpackNormX(int packed) {
        return ((byte) (packed & 0xFF)) / 127.0f;
    }

    public static float unpackNormY(int packed) {
        return ((byte) ((packed >>> 8) & 0xFF)) / 127.0f;
    }

    public static float unpackNormZ(int packed) {
        return ((byte) ((packed >>> 16) & 0xFF)) / 127.0f;
    }

    /**
     * Packs normalized RGBA floats in [0, 1] into little-endian ABGR int used by Sodium/MC buffers.
     */
    public static int packAbgr(float r, float g, float b, float a) {
        int ri = ((int) (Mth.clamp(r, 0.0f, 1.0f) * 255.0f)) & 0xFF;
        int gi = ((int) (Mth.clamp(g, 0.0f, 1.0f) * 255.0f)) & 0xFF;
        int bi = ((int) (Mth.clamp(b, 0.0f, 1.0f) * 255.0f)) & 0xFF;
        int ai = ((int) (Mth.clamp(a, 0.0f, 1.0f) * 255.0f)) & 0xFF;
        return ri | (gi << 8) | (bi << 16) | (ai << 24);
    }

    /**
     * Converts {@code 0xAARRGGBB} ({@code FastColor.ARGB32}) to little-endian {@code 0xAABBGGRR}.
     */
    public static int argbToAbgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb & 0x00FF0000) >>> 16) | ((argb & 0x000000FF) << 16);
    }

    public static int replaceAbgrAlpha(int abgr, int alphaByte) {
        return (abgr & 0x00FFFFFF) | ((alphaByte & 0xFF) << 24);
    }
}
