package dev.rinchan.tapetum.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.rinchan.tapetum.Tapetum;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TapetumConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile Path configPath;
    private static volatile Data current = new Data();

    private TapetumConfig() {}

    public static final class Data {
        public boolean vanillaModelPart54B = true;
        public boolean enchantedGlintFastWriter = true;
        public boolean geckolibFast54B = true;
        public boolean azurelibFast54B = true;
        public boolean taczFastRender = true;
        public boolean maidAndBedrockFastRender = true;
        public boolean slashBladeFastRender = true;
        public boolean smartShadowCulling = true;
        public double shadowCullDistance = 24.0;

        public Data copy() {
            Data d = new Data();
            d.vanillaModelPart54B = this.vanillaModelPart54B;
            d.enchantedGlintFastWriter = this.enchantedGlintFastWriter;
            d.geckolibFast54B = this.geckolibFast54B;
            d.azurelibFast54B = this.azurelibFast54B;
            d.taczFastRender = this.taczFastRender;
            d.maidAndBedrockFastRender = this.maidAndBedrockFastRender;
            d.slashBladeFastRender = this.slashBladeFastRender;
            d.smartShadowCulling = this.smartShadowCulling;
            d.shadowCullDistance = clampDistance(this.shadowCullDistance);
            return d;
        }
    }

    public static void load(Path path) {
        configPath = path;
        if (path != null && Files.exists(path)) {
            try {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                Data parsed = GSON.fromJson(json, Data.class);
                if (parsed != null) {
                    parsed.shadowCullDistance = clampDistance(parsed.shadowCullDistance);
                    current = parsed;
                    return;
                }
            } catch (Exception e) {
                Tapetum.LOGGER.warn("Failed to read config from {}, using defaults", path, e);
            }
        }
        current = new Data();
        save();
    }

    public static void save() {
        Path path = configPath;
        if (path == null) {
            return;
        }
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, GSON.toJson(current) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            Tapetum.LOGGER.warn("Failed to save config to {}", path, e);
        }
    }

    public static Data get() {
        return current;
    }

    public static void applyAndSave(Data updated) {
        current = updated.copy();
        save();
    }

    public static void resetDefaults() {
        current = new Data();
        save();
    }

    public static double clampDistance(double dist) {
        if (Double.isNaN(dist) || Double.isInfinite(dist)) {
            return 24.0;
        }
        return Math.max(8.0, Math.min(128.0, dist));
    }

    public static boolean isVanillaModelPart54B() {
        return current.vanillaModelPart54B;
    }

    public static boolean isEnchantedGlintFastWriter() {
        return current.enchantedGlintFastWriter;
    }

    public static boolean isGeckolibFast54B() {
        return current.geckolibFast54B;
    }

    public static boolean isAzurelibFast54B() {
        return current.azurelibFast54B;
    }

    public static boolean isTaczFastRender() {
        return current.taczFastRender;
    }

    public static boolean isMaidAndBedrockFastRender() {
        return current.maidAndBedrockFastRender;
    }

    public static boolean isSlashBladeFastRender() {
        return current.slashBladeFastRender;
    }

    public static boolean isSmartShadowCulling() {
        return current.smartShadowCulling;
    }

    public static double shadowCullDistance() {
        return current.shadowCullDistance;
    }
}
