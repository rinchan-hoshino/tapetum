package dev.rinchan.tapetum.fabric.mixin;

import dev.rinchan.tapetum.hygiene.FrameClock;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererFrameMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void tapetum$advanceFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        FrameClock.advanceFrame();
    }
}
