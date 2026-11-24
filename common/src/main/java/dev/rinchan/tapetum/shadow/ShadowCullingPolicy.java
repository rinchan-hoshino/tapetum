package dev.rinchan.tapetum.shadow;

import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.hygiene.RenderDistanceGuard;
import dev.rinchan.tapetum.vertex.IrisVertexBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Unified shadow-pass culling policy for Tapetum.
 * Preserves full terrain/structure shadow distance while culling invisible, self-lit,
 * and micro-detail geometry during Iris shadow passes.
 */
public final class ShadowCullingPolicy {
    public static final double GUNSMITH_VIEW_DIST = 40.0;
    public static final double GUNSMITH_VIEW_DIST_SQR = GUNSMITH_VIEW_DIST * GUNSMITH_VIEW_DIST;

    private static final Class<?> DOLL_ENTITY_CLASS = findClass("com.github.ysbbbbbb.kaleidoscopedoll.entity.DollEntity");

    private ShadowCullingPolicy() {}

    private static double maidDistSqr() {
        double d = TapetumConfig.shadowCullDistance();
        return d * d;
    }

    private static double blockDistSqr() {
        double d = Math.min(16.0, TapetumConfig.shadowCullDistance());
        return d * d;
    }

    public static boolean shouldCullTaCZBlockShadow(BlockPos pos) {
        return TapetumConfig.isSmartShadowCulling()
                && IrisVertexBridge.isShadowPass()
                && RenderDistanceGuard.isBlockBeyondCamera(pos, blockDistSqr());
    }

    public static boolean shouldCullTaCZShowcaseShadow() {
        return TapetumConfig.isSmartShadowCulling() && IrisVertexBridge.isShadowPass();
    }

    public static boolean shouldCullTlmMaidBodyShadow(Entity maid) {
        return TapetumConfig.isSmartShadowCulling()
                && IrisVertexBridge.isShadowPass()
                && RenderDistanceGuard.isEntityBeyondCamera(maid, maidDistSqr());
    }

    public static boolean shouldCullTlmDecorationShadow() {
        return TapetumConfig.isSmartShadowCulling() && IrisVertexBridge.isShadowPass();
    }

    public static boolean shouldCullSlashBladeLuminousShadow() {
        return TapetumConfig.isSmartShadowCulling() && IrisVertexBridge.isShadowPass();
    }

    public static boolean shouldCullSlashBladeStandShadow(Entity stand) {
        return TapetumConfig.isSmartShadowCulling()
                && IrisVertexBridge.isShadowPass()
                && RenderDistanceGuard.isEntityBeyondCamera(stand, blockDistSqr());
    }

    public static boolean shouldCullSlashBladeHotbarShadow() {
        return TapetumConfig.isSmartShadowCulling() && IrisVertexBridge.isShadowPass();
    }

    public static boolean shouldCullDecorativeEntityShadow(Entity entity) {
        if (!TapetumConfig.isSmartShadowCulling() || !IrisVertexBridge.isShadowPass() || entity == null) {
            return false;
        }
        if (entity instanceof ArmorStand || entity instanceof HangingEntity || entity instanceof ItemEntity) {
            return true;
        }
        return DOLL_ENTITY_CLASS != null && DOLL_ENTITY_CLASS.isInstance(entity);
    }

    private static Class<?> findClass(String name) {
        try {
            return Class.forName(name, false, ShadowCullingPolicy.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }
}
