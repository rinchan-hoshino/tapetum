package dev.rinchan.tapetum.neoforge;

import dev.rinchan.tapetum.Tapetum;
import dev.rinchan.tapetum.client.TapetumConfigScreen;
import dev.rinchan.tapetum.geometry.MeshCacheRegistry;
import dev.rinchan.tapetum.hygiene.FrameClock;
import java.nio.file.Path;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Tapetum.MOD_ID)
public final class TapetumNeoForge {
    public TapetumNeoForge(IEventBus modEventBus, Dist dist) {
        Tapetum.init(new NeoForgeBridge());
        if (dist.isClient()) {
            ClientHooks.init(modEventBus);
        }
    }

    private static final class NeoForgeBridge implements Tapetum.PlatformBridge {
        @Override
        public Path configDir() {
            return FMLPaths.CONFIGDIR.get();
        }

        @Override
        public String loaderName() {
            return "NeoForge";
        }

        @Override
        public boolean isModLoaded(String modId) {
            return ModList.get().isLoaded(modId);
        }
    }

    private static final class ClientHooks {
        static void init(IEventBus modEventBus) {
            ModLoadingContext.get()
                    .registerExtensionPoint(
                            IConfigScreenFactory.class,
                            () -> (container, parent) -> new TapetumConfigScreen(parent));
            modEventBus.addListener(
                    RegisterClientReloadListenersEvent.class,
                    event -> event.registerReloadListener(
                            (ResourceManagerReloadListener) rm -> MeshCacheRegistry.clearAll()));
            NeoForge.EVENT_BUS.addListener(
                    RenderFrameEvent.Pre.class, event -> FrameClock.advanceFrame());
            NeoForge.EVENT_BUS.addListener(
                    ClientPlayerNetworkEvent.LoggingOut.class,
                    event -> MeshCacheRegistry.clearAll());
        }
    }
}
