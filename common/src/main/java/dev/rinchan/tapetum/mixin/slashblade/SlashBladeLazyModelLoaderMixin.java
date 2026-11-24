package dev.rinchan.tapetum.mixin.slashblade;

import mods.flammpfeil.slashblade.client.renderer.event.ModelResourceLoader;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModelResourceLoader.class, remap = false)
public abstract class SlashBladeLazyModelLoaderMixin {
    @Inject(
            method = "loadResources(Lnet/minecraft/server/packs/resources/ResourceManager;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/ResourceManager;listResources(Ljava/lang/String;Ljava/util/function/Predicate;)Ljava/util/Map;"),
            cancellable = true)
    private void tapetum$skipEagerObjPreload(ResourceManager resourceManager, CallbackInfo ci) {
        ci.cancel();
    }
}
