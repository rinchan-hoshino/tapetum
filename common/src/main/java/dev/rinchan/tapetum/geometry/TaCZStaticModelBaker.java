package dev.rinchan.tapetum.geometry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.bedrock.BedrockCube;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Bakes an entire static TaCZ {@link BedrockModel} (such as a GunSmithTable) into two flat
 * {@link BakedQuadMesh} buckets — ordinary geometry and {@code illuminated} full-bright geometry.
 */
public final class TaCZStaticModelBaker {
    public record BakedTableModel(BakedQuadMesh ordinaryMesh, BakedQuadMesh illuminatedMesh) {
        public static final BakedTableModel EMPTY =
                new BakedTableModel(BakedQuadMesh.EMPTY, BakedQuadMesh.EMPTY);

        public boolean isEmpty() {
            return ordinaryMesh.isEmpty() && illuminatedMesh.isEmpty();
        }
    }

    private static final Map<BedrockModel, BakedTableModel> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    static {
        MeshCacheRegistry.register(CACHE::clear);
    }

    private TaCZStaticModelBaker() {}

    public static BakedTableModel getOrBake(BedrockModel model) {
        if (model == null) {
            return BakedTableModel.EMPTY;
        }
        BakedTableModel cached = CACHE.get(model);
        if (cached != null) {
            return cached;
        }
        BakedTableModel baked = bake(model);
        CACHE.put(model, baked);
        return baked;
    }

    public static BakedTableModel bake(BedrockModel model) {
        List<BedrockPart> roots = model.getShouldRender();
        if (roots == null || roots.isEmpty()) {
            return BakedTableModel.EMPTY;
        }
        BakedQuadMesh.Builder ordinary = BakedQuadMesh.builder();
        BakedQuadMesh.Builder illuminated = BakedQuadMesh.builder();
        PoseStack stack = new PoseStack();
        for (BedrockPart part : roots) {
            if (part != null) {
                bakePart(part, stack, false, ordinary, illuminated);
            }
        }
        return new BakedTableModel(ordinary.build(), illuminated.build());
    }

    private static void bakePart(
            BedrockPart part,
            PoseStack stack,
            boolean parentIlluminated,
            BakedQuadMesh.Builder ordinary,
            BakedQuadMesh.Builder illuminated) {
        boolean isIlluminated = parentIlluminated || part.illuminated;
        if (!part.visible) {
            return;
        }
        if (part.cubes.isEmpty() && part.children.isEmpty()) {
            return;
        }
        stack.pushPose();
        part.translateAndRotateAndScale(stack);
        if (!part.cubes.isEmpty()) {
            BakedQuadMesh.Builder target = isIlluminated ? illuminated : ordinary;
            PoseStack.Pose pose = stack.last();
            for (BedrockCube cube : part.cubes) {
                cube.compile(pose, target, 0, 0, 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
        for (BedrockPart child : part.children) {
            if (child != null) {
                bakePart(child, stack, isIlluminated, ordinary, illuminated);
            }
        }
        stack.popPose();
    }
}
