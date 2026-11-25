package dev.rinchan.tapetum.forge;

import dev.rinchan.tapetum.client.TapetumConfigScreen;
import dev.rinchan.tapetum.geometry.MeshCacheRegistry;
import dev.rinchan.tapetum.hygiene.FrameClock;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class TapetumForgeClient {
    private TapetumForgeClient() {}

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> new TapetumConfigScreen(parent)));
        FMLJavaModLoadingContext.get().getModEventBus().addListener(
                (RegisterClientReloadListenersEvent event) -> event.registerReloadListener(
                        (ResourceManagerReloadListener) rm -> MeshCacheRegistry.clearAll()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.RenderTickEvent event) -> {
            if (event.phase == TickEvent.Phase.START) {
                FrameClock.advanceFrame();
            }
        });
        MinecraftForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> MeshCacheRegistry.clearAll());
    }
}
