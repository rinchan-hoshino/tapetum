package dev.rinchan.tapetum.mixin.azurelib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.geometry.BakedQuadMesh;
import dev.rinchan.tapetum.geometry.BoneMeshAccess;
import dev.rinchan.tapetum.geometry.MeshCacheRegistry;
import dev.rinchan.tapetum.vertex.EntityVertexLayout;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import java.util.List;
import mod.azure.azurelib.common.cache.object.GeoCube;
import mod.azure.azurelib.common.cache.object.GeoQuad;
import mod.azure.azurelib.common.cache.object.GeoVertex;
import mod.azure.azurelib.common.model.AzBone;
import mod.azure.azurelib.common.render.AzModelRenderer;
import mod.azure.azurelib.common.render.AzRendererPipelineContext;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Layer 2 AzureLib pre-baked 54B/36B off-heap fast path for {@link AzModelRenderer#renderCubesOfBone}.
 */
@Pseudo
@Mixin(value = AzModelRenderer.class, remap = false)
public abstract class AzModelRendererFastMixin<K, T> {
    @Inject(
            method =
                    "renderCubesOfBone(Lmod/azure/azurelib/common/render/AzRendererPipelineContext;Lmod/azure/azurelib/common/model/AzBone;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void tapetum$renderCubesOfBoneFast(
            AzRendererPipelineContext<K, T> context, AzBone bone, CallbackInfo ci) {
        if (!TapetumConfig.isAzurelibFast54B()
                || !(bone instanceof BoneMeshAccess access)
                || context.quadFilter() != null
                || !bone.getMeshes().isEmpty()
                || bone.getSkinBindings().length != 0) {
            return;
        }
        VertexConsumer buffer = context.vertexConsumer();
        if (FastEntityVertexWriter.resolveMode(buffer) == FastEntityVertexWriter.Mode.FALLBACK_CONSUMER) {
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
        PoseStack poseStack = context.poseStack();
        FastEntityVertexWriter.emitQuads(
                mesh,
                poseStack.last(),
                buffer,
                context.packedLight(),
                context.packedOverlay(),
                EntityVertexLayout.argbToAbgr(context.renderColor()));
        ci.cancel();
    }

    @Unique
    private static BakedQuadMesh tapetum$bakeBone(AzBone bone) {
        List<GeoCube> cubes = bone.getCubes();
        if (cubes == null || cubes.isEmpty()) {
            return BakedQuadMesh.EMPTY;
        }
        BakedQuadMesh.Builder builder = BakedQuadMesh.plainBuilder();
        for (GeoCube cube : cubes) {
            GeoQuad[] quads = cube.quads();
            if (quads == null || quads.length == 0) {
                continue;
            }
            GeoCube.Transform transform = cube.transform();
            boolean identity = transform.identity();
            Matrix4fc mat = transform.pose();
            Matrix3fc normMat = transform.normal();
            for (GeoQuad quad : quads) {
                if (quad == null) {
                    continue;
                }
                GeoVertex[] verts = quad.vertices();
                if (verts == null || verts.length < 4) {
                    continue;
                }
                Vector3f n = quad.normal();
                float nx = identity
                        ? n.x()
                        : normMat.m00() * n.x() + normMat.m10() * n.y() + normMat.m20() * n.z();
                float ny = identity
                        ? n.y()
                        : normMat.m01() * n.x() + normMat.m11() * n.y() + normMat.m21() * n.z();
                float nz = identity
                        ? n.z()
                        : normMat.m02() * n.x() + normMat.m12() * n.y() + normMat.m22() * n.z();

                Vector3f p0 = verts[0].position();
                Vector3f p1 = verts[1].position();
                Vector3f p2 = verts[2].position();
                Vector3f p3 = verts[3].position();
                if (identity) {
                    builder.addQuad(
                            p0.x(), p0.y(), p0.z(), verts[0].texU(), verts[0].texV(),
                            p1.x(), p1.y(), p1.z(), verts[1].texU(), verts[1].texV(),
                            p2.x(), p2.y(), p2.z(), verts[2].texU(), verts[2].texV(),
                            p3.x(), p3.y(), p3.z(), verts[3].texU(), verts[3].texV(),
                            nx, ny, nz);
                } else {
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
            }
        }
        return builder.build();
    }
}
