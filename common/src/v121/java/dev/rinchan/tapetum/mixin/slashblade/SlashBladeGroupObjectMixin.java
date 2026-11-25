package dev.rinchan.tapetum.mixin.slashblade;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.geometry.MeshCacheRegistry;
import dev.rinchan.tapetum.slashblade.BakedSlashBladeMesh;
import java.util.List;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Face;
import mods.flammpfeil.slashblade.client.renderer.model.obj.GroupObject;
import mods.flammpfeil.slashblade.client.renderer.model.obj.TextureCoordinate;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Vertex;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GroupObject.class, remap = false)
public abstract class SlashBladeGroupObjectMixin {
    @Unique
    private static final float DEFAULT_TEXTURE_OFFSET = 5.0E-4f;

    @Shadow
    public List<Face> faces;

    @Unique
    private BakedSlashBladeMesh tapetum$bakedMesh;

    @Unique
    private int tapetum$bakedEpoch = -1;

    @Inject(
            method =
                    "render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack;II)V",
            at = @At("HEAD"),
            cancellable = true)
    private void tapetum$renderBaked(
            VertexConsumer tessellator, PoseStack matrixStack, int light, int color, CallbackInfo ci) {
        if (!TapetumConfig.isSlashBladeFastRender()) {
            return;
        }
        if (this.faces == null || this.faces.isEmpty()) {
            ci.cancel();
            return;
        }
        int epoch = MeshCacheRegistry.reloadEpoch();
        BakedSlashBladeMesh baked = this.tapetum$bakedMesh;
        if (baked == null || this.tapetum$bakedEpoch != epoch) {
            baked = tapetum$bakeGroup(this.faces);
            this.tapetum$bakedMesh = baked;
            this.tapetum$bakedEpoch = epoch;
        }
        PoseStack.Pose last = matrixStack.last();
        Vector4f uvOp = Face.uvOperator;
        boolean alphaOverrideYzz = Face.alphaOverride == Face.alphaOverrideYZZ
                || (Face.alphaOverride != null && Face.alphaOverride.equals(Face.alphaOverrideYZZ));
        baked.render(
                tessellator,
                last.pose(),
                last.normal(),
                light,
                color,
                uvOp.x(),
                uvOp.y(),
                uvOp.z(),
                uvOp.w(),
                alphaOverrideYzz);
        ci.cancel();
    }

    @Unique
    private static BakedSlashBladeMesh tapetum$bakeGroup(List<Face> faceList) {
        int totalVertices = 0;
        boolean allTriangles = true;
        for (Face face : faceList) {
            if (face != null && face.vertices != null && face.vertices.length > 0) {
                totalVertices += face.vertices.length;
                if (face.vertices.length != 3) {
                    allTriangles = false;
                }
            }
        }
        float[] pos = new float[totalVertices * 3];
        float[] uv = new float[totalVertices * 2];
        float[] uvOffset = new float[totalVertices * 2];
        float[] normal = new float[totalVertices * 3];
        byte[] flags = new byte[totalVertices];

        int idx = 0;
        for (Face face : faceList) {
            if (face == null || face.vertices == null || face.vertices.length == 0) {
                continue;
            }
            if (face.faceNormal == null && face.vertices.length >= 3) {
                face.faceNormal = face.calculateFaceNormal();
            }
            Vertex[] verts = face.vertices;
            Vertex[] vNormals = face.vertexNormals;
            Vertex fNormal = face.faceNormal;
            TextureCoordinate[] texCoords = face.textureCoordinates;
            boolean hasUv = texCoords != null && texCoords.length > 0;
            boolean[] uSign = face.texUSign;
            boolean[] vSign = face.texVSign;

            for (int i = 0; i < verts.length; i++) {
                Vertex v = verts[i];
                int pBase = idx * 3;
                int uvBase = idx * 2;
                pos[pBase] = v.x;
                pos[pBase + 1] = v.y;
                pos[pBase + 2] = v.z;

                byte flag = 0;
                if (v.y == 0.0f) {
                    flag |= BakedSlashBladeMesh.FLAG_Y_IS_ZERO;
                }
                if (hasUv && i < texCoords.length) {
                    TextureCoordinate tc = texCoords[i];
                    uv[uvBase] = tc.u;
                    uv[uvBase + 1] = tc.v;
                    uvOffset[uvBase] = (uSign != null && i < uSign.length && uSign[i])
                            ? -DEFAULT_TEXTURE_OFFSET
                            : DEFAULT_TEXTURE_OFFSET;
                    uvOffset[uvBase + 1] = (vSign != null && i < vSign.length && vSign[i])
                            ? -DEFAULT_TEXTURE_OFFSET
                            : DEFAULT_TEXTURE_OFFSET;
                    flag |= BakedSlashBladeMesh.FLAG_HAS_UV;
                }

                Vertex n = (vNormals != null && i < vNormals.length) ? vNormals[i] : fNormal;
                if (n != null) {
                    float nx = n.x, ny = n.y, nz = n.z;
                    float lenSq = nx * nx + ny * ny + nz * nz;
                    if (lenSq > 1e-12f && Math.abs(lenSq - 1.0f) > 1e-4f) {
                        float inv = (float) (1.0 / Math.sqrt(lenSq));
                        nx *= inv;
                        ny *= inv;
                        nz *= inv;
                    }
                    normal[pBase] = nx;
                    normal[pBase + 1] = ny;
                    normal[pBase + 2] = nz;
                    flag |= BakedSlashBladeMesh.FLAG_HAS_NORMAL;
                }
                flags[idx++] = flag;
            }
        }
        return new BakedSlashBladeMesh(idx, allTriangles, pos, uv, uvOffset, normal, flags);
    }
}
