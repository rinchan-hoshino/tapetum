package dev.rinchan.tapetum.geometry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import java.util.IdentityHashMap;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pre-bakes static {@link BlockState} models displayed by Kaleidoscope Doll entities into
 * {@link BakedQuadMesh} so each frame emits directly through {@link FastEntityVertexWriter}.
 */
public final class DollBlockMeshCache {
    private static final Direction[] ALL_DIRECTIONS = Direction.values();
    private static final CachedBlock INELIGIBLE = new CachedBlock(false, BakedQuadMesh.EMPTY, BakedQuadMesh.EMPTY, -1);
    private static final IdentityHashMap<BlockState, CachedBlock> CACHE = new IdentityHashMap<>();
    private static int cachedEpoch = -1;

    static {
        MeshCacheRegistry.register(DollBlockMeshCache::clear);
    }

    private DollBlockMeshCache() {}

    public static void clear() {
        CACHE.clear();
        cachedEpoch = -1;
    }

    public static boolean tryRenderFast(
            Entity doll,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight) {
        if (doll == null || state == null || state.isAir()) {
            return false;
        }
        int epoch = MeshCacheRegistry.reloadEpoch();
        if (cachedEpoch != epoch) {
            CACHE.clear();
            cachedEpoch = epoch;
        }
        CachedBlock entry = CACHE.get(state);
        if (entry == null) {
            entry = bake(state);
            CACHE.put(state, entry);
        }
        if (!entry.eligible) {
            return false;
        }
        VertexConsumer consumer = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        if (!entry.untintedMesh.isEmpty()) {
            FastEntityVertexWriter.emitQuads(
                    entry.untintedMesh,
                    pose.pose(),
                    pose.normal(),
                    consumer,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    -1);
        }
        if (!entry.tintedMesh.isEmpty()) {
            int rgb = Minecraft.getInstance()
                    .getBlockColors()
                    .getColor(state, doll.level(), doll.blockPosition(), entry.tintIndex);
            int abgr = rgb == -1 ? -1 : rgbToAbgr(rgb);
            FastEntityVertexWriter.emitQuads(
                    entry.tintedMesh,
                    pose.pose(),
                    pose.normal(),
                    consumer,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    abgr);
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private static CachedBlock bake(BlockState state) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(state);
        if (model == null || model.isCustomRenderer()) {
            return INELIGIBLE;
        }
        RandomSource random = RandomSource.create(42L);
        BakedQuadMesh.Builder untinted = BakedQuadMesh.plainBuilder();
        BakedQuadMesh.Builder tinted = BakedQuadMesh.plainBuilder();
        int tintIndex = -1;

        for (Direction dir : ALL_DIRECTIONS) {
            random.setSeed(42L);
            tintIndex = appendQuads(model.getQuads(state, dir, random), untinted, tinted, tintIndex);
        }
        random.setSeed(42L);
        tintIndex = appendQuads(model.getQuads(state, null, random), untinted, tinted, tintIndex);

        BakedQuadMesh untintedMesh = untinted.build();
        BakedQuadMesh tintedMesh = tinted.build();
        if (untintedMesh.isEmpty() && tintedMesh.isEmpty()) {
            return INELIGIBLE;
        }
        return new CachedBlock(true, untintedMesh, tintedMesh, tintIndex);
    }

    private static int appendQuads(
            List<BakedQuad> quads,
            BakedQuadMesh.Builder untinted,
            BakedQuadMesh.Builder tinted,
            int currentTintIndex) {
        if (quads == null || quads.isEmpty()) {
            return currentTintIndex;
        }
        for (BakedQuad quad : quads) {
            if (quad == null) {
                continue;
            }
            int[] data = quad.getVertices();
            if (data.length < 32) {
                continue;
            }
            BakedQuadMesh.Builder target = quad.isTinted() ? tinted : untinted;
            if (quad.isTinted() && currentTintIndex == -1) {
                currentTintIndex = quad.getTintIndex();
            }
            Vec3i norm = quad.getDirection().getNormal();
            target.addQuad(
                    Float.intBitsToFloat(data[0]), Float.intBitsToFloat(data[1]), Float.intBitsToFloat(data[2]),
                    Float.intBitsToFloat(data[4]), Float.intBitsToFloat(data[5]),
                    Float.intBitsToFloat(data[8]), Float.intBitsToFloat(data[9]), Float.intBitsToFloat(data[10]),
                    Float.intBitsToFloat(data[12]), Float.intBitsToFloat(data[13]),
                    Float.intBitsToFloat(data[16]), Float.intBitsToFloat(data[17]), Float.intBitsToFloat(data[18]),
                    Float.intBitsToFloat(data[20]), Float.intBitsToFloat(data[21]),
                    Float.intBitsToFloat(data[24]), Float.intBitsToFloat(data[25]), Float.intBitsToFloat(data[26]),
                    Float.intBitsToFloat(data[28]), Float.intBitsToFloat(data[29]),
                    norm.getX(), norm.getY(), norm.getZ());
        }
        return currentTintIndex;
    }

    private static int rgbToAbgr(int rgb) {
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }

    private record CachedBlock(
            boolean eligible, BakedQuadMesh untintedMesh, BakedQuadMesh tintedMesh, int tintIndex) {}
}
