package dev.rinchan.tapetum.fabric.mixin;

import dev.rinchan.tapetum.geometry.MeshCacheRegistry;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftLifecycleMixin {
    @Inject(method = "reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
    private void tapetum$clearOnReload(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        MeshCacheRegistry.clearAll();
    }

    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    private void tapetum$clearOnDisconnect(Screen nextScreen, CallbackInfo ci) {
        MeshCacheRegistry.clearAll();
    }
}
