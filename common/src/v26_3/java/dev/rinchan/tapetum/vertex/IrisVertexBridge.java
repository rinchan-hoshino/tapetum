package dev.rinchan.tapetum.vertex;

import com.mojang.renderpearl.api.vertex.VertexFormat;
import dev.rinchan.tapetum.Tapetum;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.IrisVertexFormats;

public final class IrisVertexBridge {
    private static final boolean IRIS_PRESENT = detectIrisFormats();

    private IrisVertexBridge() {}

    public static boolean isIrisPresent() {
        return IRIS_PRESENT;
    }

    public static boolean isIris54BVerified() {
        return IRIS_PRESENT && IrisAccess.VERIFIED_ENTITY_FORMAT != null;
    }

    public static boolean isShaderPackInUse() {
        return IRIS_PRESENT && IrisAccess.isPackInUse();
    }

    public static boolean isShadowPass() {
        return IRIS_PRESENT && IrisAccess.isShadowPass();
    }

    public static VertexFormat getVerifiedEntityFormat() {
        return IRIS_PRESENT ? IrisAccess.VERIFIED_ENTITY_FORMAT : null;
    }

    public static short currentEntityId() {
        return IRIS_PRESENT ? IrisAccess.entityId() : 0;
    }

    public static short currentBlockEntityId() {
        return IRIS_PRESENT ? IrisAccess.blockEntityId() : 0;
    }

    public static short currentItemId() {
        return IRIS_PRESENT ? IrisAccess.itemId() : 0;
    }

    private static boolean detectIrisFormats() {
        try {
            Class.forName(
                    "net.irisshaders.iris.vertices.IrisVertexFormats",
                    false,
                    IrisVertexBridge.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private static final class IrisAccess {
        static final VertexFormat VERIFIED_ENTITY_FORMAT = verifyLayout();

        private static VertexFormat verifyLayout() {
            try {
                VertexFormat fmt = IrisVertexFormats.ENTITY;
                if (fmt == null) {
                    return null;
                }

                boolean matches = fmt.getVertexSize() == EntityVertexLayout.STRIDE_54B
                        && IrisVertexFormats.getOffset(fmt, IrisVertexFormats.ENTITY_ID_ATTRIBUTE) == EntityVertexLayout.OFFSET_IRIS_ENTITY_X
                        && IrisVertexFormats.getOffset(fmt, IrisVertexFormats.MID_TEXTURE_ATTRIBUTE) == EntityVertexLayout.OFFSET_MID_UV
                        && IrisVertexFormats.getOffset(fmt, IrisVertexFormats.TANGENT_ATTRIBUTE) == EntityVertexLayout.OFFSET_TANGENT;
                if (!matches) {
                    Tapetum.LOGGER.warn(
                            "IrisVertexFormats.ENTITY layout changed (size={}); falling back to safe VertexConsumer path",
                            fmt.getVertexSize());
                    return null;
                }
                return fmt;
            } catch (Throwable t) {
                Tapetum.LOGGER.warn("Failed to inspect IrisVertexFormats.ENTITY; disabling 54B fast path", t);
                return null;
            }
        }

        static boolean isPackInUse() {
            return IrisApi.getInstance().isShaderPackInUse();
        }

        static boolean isShadowPass() {
            return ShadowRenderer.ACTIVE;
        }

        static short entityId() {
            return (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        }

        static short blockEntityId() {
            return (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity();
        }

        static short itemId() {
            return (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem();
        }
    }
}
