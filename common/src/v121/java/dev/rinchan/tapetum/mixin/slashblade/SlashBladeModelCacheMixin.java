package dev.rinchan.tapetum.mixin.slashblade;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rinchan.tapetum.geometry.MeshCacheRegistry;
import java.util.concurrent.TimeUnit;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BladeModelManager.class, remap = false)
public abstract class SlashBladeModelCacheMixin {
    @Shadow
    public LoadingCache<ResourceLocation, WavefrontObject> cache;

    @WrapOperation(
            method = "<init>()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/cache/CacheBuilder;maximumSize(J)Lcom/google/common/cache/CacheBuilder;"))
    private CacheBuilder<Object, Object> tapetum$boundAndSoftenModelCache(
            CacheBuilder<Object, Object> builder,
            long originalSize,
            Operation<CacheBuilder<Object, Object>> original) {
        return original.call(builder, 64L)
                .expireAfterAccess(5L, TimeUnit.MINUTES)
                .softValues();
    }

    @Inject(method = "<init>()V", at = @At("TAIL"))
    private void tapetum$registerCacheInvalidation(CallbackInfo ci) {
        MeshCacheRegistry.register(() -> {
            LoadingCache<ResourceLocation, WavefrontObject> current = this.cache;
            if (current != null) {
                current.invalidateAll();
            }
        });
    }
}
