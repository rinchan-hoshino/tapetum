package dev.rinchan.tapetum.fabric;

import dev.rinchan.tapetum.Tapetum;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class TapetumFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Tapetum.init(new FabricBridge());
    }

    private static final class FabricBridge implements Tapetum.PlatformBridge {
        @Override
        public Path configDir() {
            return FabricLoader.getInstance().getConfigDir();
        }

        @Override
        public String loaderName() {
            return "Fabric";
        }

        @Override
        public boolean isModLoaded(String modId) {
            return FabricLoader.getInstance().isModLoaded(modId);
        }
    }
}
