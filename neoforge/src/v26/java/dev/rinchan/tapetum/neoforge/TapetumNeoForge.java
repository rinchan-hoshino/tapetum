package dev.rinchan.tapetum.neoforge;

import dev.rinchan.tapetum.Tapetum;
import dev.rinchan.tapetum.client.TapetumConfigScreen;
import dev.rinchan.tapetum.geometry.MeshCacheRegistry;
import dev.rinchan.tapetum.hygiene.FrameClock;
import java.nio.file.Path;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Tapetum.MOD_ID)
public final class TapetumNeoForge {
    public TapetumNeoForge(IEventBus modEventBus, ModContainer container, Dist dist) {
        Tapetum.init(new NeoForgeBridge());
        if (dist.isClient()) {
            ClientHooks.init(modEventBus, container);
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
        static void init(IEventBus modEventBus, ModContainer container) {
            container.registerExtensionPoint(
                    IConfigScreenFactory.class,
                    (mc, parent) -> new TapetumConfigScreen(parent));
            modEventBus.addListener(
                    AddClientReloadListenersEvent.class,
                    event -> event.addListener(
                            Identifier.fromNamespaceAndPath(Tapetum.MOD_ID, "mesh_cache_reset"),
                            (ResourceManagerReloadListener) rm -> MeshCacheRegistry.clearAll()));
            NeoForge.EVENT_BUS.addListener(
                    RenderFrameEvent.Pre.class, event -> FrameClock.advanceFrame());
            NeoForge.EVENT_BUS.addListener(
                    ClientPlayerNetworkEvent.LoggingOut.class,
                    event -> MeshCacheRegistry.clearAll());
        }
    }
}
