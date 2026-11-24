package dev.rinchan.tapetum;

import dev.rinchan.tapetum.config.TapetumConfig;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Tapetum {
    public static final String MOD_ID = "tapetum";
    public static final String MOD_NAME = "Tapetum";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static volatile PlatformBridge platform;

    private Tapetum() {}

    public static void init(PlatformBridge bridge) {
        platform = bridge;
        if (bridge != null) {
            TapetumConfig.load(bridge.configDir().resolve(MOD_ID + ".json"));
        }
        LOGGER.info("{} initialized on {}", MOD_NAME, bridge != null ? bridge.loaderName() : "unknown");
    }

    public static PlatformBridge platform() {
        return platform;
    }

    public interface PlatformBridge {
        Path configDir();

        String loaderName();

        boolean isModLoaded(String modId);
    }
}
