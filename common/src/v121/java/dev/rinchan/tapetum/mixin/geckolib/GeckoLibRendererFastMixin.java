package dev.rinchan.tapetum.mixin.geckolib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.geometry.BakedQuadMesh;
import dev.rinchan.tapetum.geometry.BoneMeshAccess;
import dev.rinchan.tapetum.geometry.MeshCacheRegistry;
import dev.rinchan.tapetum.vertex.EntityVertexLayout;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import java.util.List;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.RenderUtil;

/**
 * Layer 2 GeckoLib 4 pre-baked 54B/36B off-heap fast path for {@link GeoRenderer#renderCubesOfBone}.
 */
@Pseudo
@Mixin(value = GeoRenderer.class, remap = false)
public interface GeckoLibRendererFastMixin {
    @Inject(
            method =
                    "renderCubesOfBone(Lcom/mojang/blaze3d/vertex/PoseStack;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD"),
            cancellable = true)
    private void tapetum$renderCubesOfBoneFast(
            PoseStack poseStack,
            GeoBone bone,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            int colour,
            CallbackInfo ci) {
        if (!TapetumConfig.isGeckolibFast54B()
                || !(bone instanceof BoneMeshAccess access)
                || FastEntityVertexWriter.resolveMode(buffer) == FastEntityVertexWriter.Mode.FALLBACK_CONSUMER) {
            return;
        }
        if (bone.isHidden()) {
            ci.cancel();
            return;
        }
        int epoch = MeshCacheRegistry.reloadEpoch();
        BakedQuadMesh mesh = access.tapetum$getBakedMesh();
        if (mesh == null || access.tapetum$getBakedEpoch() != epoch) {
            mesh = tapetum$bakeBone(bone);
            access.tapetum$setBakedMesh(mesh, epoch);
        }
        if (mesh.isEmpty()) {
            ci.cancel();
            return;
        }
        FastEntityVertexWriter.emitQuads(
                mesh, poseStack.last(), buffer, packedLight, packedOverlay, EntityVertexLayout.argbToAbgr(colour));
        ci.cancel();
    }

    private static BakedQuadMesh tapetum$bakeBone(GeoBone bone) {
        List<GeoCube> cubes = bone.getCubes();
        if (cubes == null || cubes.isEmpty()) {
            return BakedQuadMesh.EMPTY;
        }
        BakedQuadMesh.Builder builder = BakedQuadMesh.plainBuilder();
        PoseStack scratch = new PoseStack();
        for (GeoCube cube : cubes) {
            GeoQuad[] quads = cube.quads();
            if (quads == null || quads.length == 0) {
                continue;
            }
            scratch.pushPose();
            RenderUtil.translateToPivotPoint(scratch, cube);
            RenderUtil.rotateMatrixAroundCube(scratch, cube);
            RenderUtil.translateAwayFromPivotPoint(scratch, cube);
            PoseStack.Pose last = scratch.last();
            Matrix4f mat = last.pose();
            Matrix3f normMat = last.normal();
            for (GeoQuad quad : quads) {
                if (quad == null) {
                    continue;
                }
                GeoVertex[] verts = quad.vertices();
                if (verts == null || verts.length < 4) {
                    continue;
                }
                Vector3f n = quad.normal();
                float nx = normMat.m00() * n.x() + normMat.m10() * n.y() + normMat.m20() * n.z();
                float ny = normMat.m01() * n.x() + normMat.m11() * n.y() + normMat.m21() * n.z();
                float nz = normMat.m02() * n.x() + normMat.m12() * n.y() + normMat.m22() * n.z();

                Vector3f p0 = verts[0].position();
                Vector3f p1 = verts[1].position();
                Vector3f p2 = verts[2].position();
                Vector3f p3 = verts[3].position();
                builder.addQuad(
                        mat.m00() * p0.x() + mat.m10() * p0.y() + mat.m20() * p0.z() + mat.m30(),
                        mat.m01() * p0.x() + mat.m11() * p0.y() + mat.m21() * p0.z() + mat.m31(),
                        mat.m02() * p0.x() + mat.m12() * p0.y() + mat.m22() * p0.z() + mat.m32(),
                        verts[0].texU(), verts[0].texV(),
                        mat.m00() * p1.x() + mat.m10() * p1.y() + mat.m20() * p1.z() + mat.m30(),
                        mat.m01() * p1.x() + mat.m11() * p1.y() + mat.m21() * p1.z() + mat.m31(),
                        mat.m02() * p1.x() + mat.m12() * p1.y() + mat.m22() * p1.z() + mat.m32(),
                        verts[1].texU(), verts[1].texV(),
                        mat.m00() * p2.x() + mat.m10() * p2.y() + mat.m20() * p2.z() + mat.m30(),
                        mat.m01() * p2.x() + mat.m11() * p2.y() + mat.m21() * p2.z() + mat.m31(),
                        mat.m02() * p2.x() + mat.m12() * p2.y() + mat.m22() * p2.z() + mat.m32(),
                        verts[2].texU(), verts[2].texV(),
                        mat.m00() * p3.x() + mat.m10() * p3.y() + mat.m20() * p3.z() + mat.m30(),
                        mat.m01() * p3.x() + mat.m11() * p3.y() + mat.m21() * p3.z() + mat.m31(),
                        mat.m02() * p3.x() + mat.m12() * p3.y() + mat.m22() * p3.z() + mat.m32(),
                        verts[3].texU(), verts[3].texV(),
                        nx, ny, nz);
            }
            scratch.popPose();
        }
        return builder.build();
    }
}
