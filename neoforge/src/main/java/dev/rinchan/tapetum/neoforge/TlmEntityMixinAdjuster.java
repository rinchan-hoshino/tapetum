package dev.rinchan.tapetum.neoforge;

import com.bawnorton.mixinsquared.adjuster.tools.AdjustableAnnotationNode;
import com.bawnorton.mixinsquared.api.MixinAnnotationAdjuster;
import java.util.List;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Inject;

public final class TlmEntityMixinAdjuster implements MixinAnnotationAdjuster {
    private static final String TARGET_MIXIN =
            "com.github.tartaricacid.touhoulittlemaid.mixin.EntityMixin";

    @Override
    public AdjustableAnnotationNode adjust(
            List<String> targets,
            String mixin,
            MethodNode method,
            AdjustableAnnotationNode annotation) {
        if (!TARGET_MIXIN.equals(mixin) || annotation == null || !annotation.is(Inject.class)) {
            return annotation;
        }
        return switch (method.name) {
            case "onGetBoundingBox", "beforeCollide", "afterCollide" -> null;
            default -> annotation;
        };
    }
}
