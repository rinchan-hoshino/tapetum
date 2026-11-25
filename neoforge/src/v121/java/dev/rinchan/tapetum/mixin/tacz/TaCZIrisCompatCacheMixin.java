package dev.rinchan.tapetum.mixin.tacz;

import com.tacz.guns.compat.iris.IrisCompat;
import dev.rinchan.tapetum.vertex.IrisVertexBridge;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = IrisCompat.class, remap = false)
public abstract class TaCZIrisCompatCacheMixin {
    @Unique
    private static final boolean TAPETUM$IRIS_LOADED = IrisVertexBridge.isIrisPresent();

    @Redirect(
            method = {
                "isPackInUseQuick()Z",
                "isRenderShadow()Z",
                "isUsingRenderPack()Z",
                "endBatch(Lnet/minecraft/client/renderer/MultiBufferSource;)Z",
                "getHandBufferSource()Lnet/minecraft/client/renderer/MultiBufferSource;"
            },
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/neoforged/fml/ModList;isLoaded(Ljava/lang/String;)Z"))
    private static boolean tapetum$cachedIrisLoaded(ModList modList, String modId) {
        return "iris".equals(modId) ? TAPETUM$IRIS_LOADED : modList.isLoaded(modId);
    }
}
