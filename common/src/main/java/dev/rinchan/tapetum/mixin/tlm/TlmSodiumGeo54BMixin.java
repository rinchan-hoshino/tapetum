package dev.rinchan.tapetum.mixin.tlm;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoMesh;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.geometry.BakedQuadMesh;
import dev.rinchan.tapetum.geometry.BoneMeshAccess;
import dev.rinchan.tapetum.geometry.MeshCacheRegistry;
import dev.rinchan.tapetum.geometry.TlmGeoBakeGuard;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "com.github.tartaricacid.touhoulittlemaid.compat.sodium.SodiumGeoRenderer",
        remap = false)
public abstract class TlmSodiumGeo54BMixin {
    @Unique
    private static final ThreadLocal<Boolean> TAPETUM$BAKING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Shadow
    static boolean renderCubesOfBone(
            AnimatedGeoBone bone,
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha) {
        return false;
    }

    @Inject(
            method =
                    "renderCubesOfBone(Lcom/github/tartaricacid/touhoulittlemaid/geckolib3/geo/animated/AnimatedGeoBone;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)Z",
            at = @At("HEAD"),
            cancellable = true)
    private static void tapetum$renderCubesFast(
            AnimatedGeoBone bone,
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha,
            CallbackInfoReturnable<Boolean> cir) {
        if (TAPETUM$BAKING.get() || !TapetumConfig.isMaidAndBedrockFastRender()) {
            return;
        }
        if (bone == null || bone.isHidden() || bone.cubesAreHidden()) {
            cir.setReturnValue(true);
            return;
        }
        FastEntityVertexWriter.Mode mode = FastEntityVertexWriter.resolveMode(buffer);
        if (mode == FastEntityVertexWriter.Mode.FALLBACK_CONSUMER) {
            return;
        }
        GeoBone geoBone = bone.geoBone();
        if (geoBone == null) {
            cir.setReturnValue(true);
            return;
        }
        if (!((Object) geoBone instanceof BoneMeshAccess meshAccess)) {
            return;
        }
        int epoch = MeshCacheRegistry.reloadEpoch();
        BakedQuadMesh mesh = meshAccess.tapetum$getBakedMesh();
        if (mesh == null || meshAccess.tapetum$getBakedEpoch() != epoch) {
            GeoMesh cubes = geoBone.cubes();
            if (cubes == null || cubes.getCubeCount() == 0) {
                mesh = BakedQuadMesh.EMPTY;
            } else {
                BakedQuadMesh.Builder builder = BakedQuadMesh.builder();
                TAPETUM$BAKING.set(Boolean.TRUE);
                try {
                    TlmGeoBakeGuard.runUnculled(
                            RenderSystem.getModelViewMatrix(),
                            () -> renderCubesOfBone(bone, new PoseStack(), builder, 0, 0, 1.0f, 1.0f, 1.0f, 1.0f));
                } finally {
                    TAPETUM$BAKING.set(Boolean.FALSE);
                }
                mesh = builder.build();
            }
            meshAccess.tapetum$setBakedMesh(mesh, epoch);
        }
        if (!mesh.isEmpty()) {
            FastEntityVertexWriter.emitQuads(
                    mesh, poseStack.last(), buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
        cir.setReturnValue(true);
    }
}
