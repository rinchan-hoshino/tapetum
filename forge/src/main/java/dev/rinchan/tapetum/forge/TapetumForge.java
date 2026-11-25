package dev.rinchan.tapetum.forge;

import dev.rinchan.tapetum.Tapetum;
import java.nio.file.Path;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(Tapetum.MOD_ID)
public final class TapetumForge {
    public TapetumForge() {
        Tapetum.init(new ForgeBridge());
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> TapetumForgeClient::register);
    }

    private static final class ForgeBridge implements Tapetum.PlatformBridge {
        @Override
        public Path configDir() {
            return FMLPaths.CONFIGDIR.get();
        }

        @Override
        public String loaderName() {
            return "Forge";
        }

        @Override
        public boolean isModLoaded(String modId) {
            return ModList.get().isLoaded(modId);
        }
    }
}
