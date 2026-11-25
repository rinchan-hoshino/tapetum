package dev.rinchan.tapetum.vertex;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import dev.rinchan.tapetum.Tapetum;
import java.util.List;
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

        private static int offsetOf(VertexFormat fmt, VertexFormatElement element) {
            int offset = 0;
            for (VertexFormatElement e : fmt.getElements()) {
                if (e == element) {
                    return offset;
                }
                offset += e.getByteSize();
            }
            return -1;
        }

        private static VertexFormat verifyLayout() {
            try {
                VertexFormat fmt = IrisVertexFormats.ENTITY;
                if (fmt == null) {
                    return null;
                }
                boolean matches = fmt.getVertexSize() == EntityVertexLayout.STRIDE_54B
                        && offsetOf(fmt, DefaultVertexFormat.ELEMENT_POSITION) == EntityVertexLayout.OFFSET_POSITION
                        && offsetOf(fmt, DefaultVertexFormat.ELEMENT_COLOR) == EntityVertexLayout.OFFSET_COLOR
                        && offsetOf(fmt, DefaultVertexFormat.ELEMENT_UV0) == EntityVertexLayout.OFFSET_UV0
                        && offsetOf(fmt, DefaultVertexFormat.ELEMENT_UV1) == EntityVertexLayout.OFFSET_UV1
                        && offsetOf(fmt, DefaultVertexFormat.ELEMENT_UV2) == EntityVertexLayout.OFFSET_UV2
                        && offsetOf(fmt, DefaultVertexFormat.ELEMENT_NORMAL) == EntityVertexLayout.OFFSET_NORMAL
                        && offsetOf(fmt, IrisVertexFormats.ENTITY_ID_ELEMENT) == EntityVertexLayout.OFFSET_IRIS_ENTITY_X
                        && offsetOf(fmt, IrisVertexFormats.MID_TEXTURE_ELEMENT) == EntityVertexLayout.OFFSET_MID_UV
                        && offsetOf(fmt, IrisVertexFormats.TANGENT_ELEMENT) == EntityVertexLayout.OFFSET_TANGENT;
                if (!matches) {
                    Tapetum.LOGGER.warn(
                            "Iris/Oculus ENTITY layout changed (size={}); falling back to safe VertexConsumer path",
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
