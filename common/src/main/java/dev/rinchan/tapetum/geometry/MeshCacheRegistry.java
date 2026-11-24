package dev.rinchan.tapetum.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central invalidation and telemetry registry for pre-baked geometry caches.
 * Cleared on resource pack reload ({@code F3+T}) and world logout via loader hooks.
 */
public final class MeshCacheRegistry {
    private static final List<Runnable> CLEAR_HOOKS = new CopyOnWriteArrayList<>();
    private static final AtomicInteger RELOAD_EPOCH = new AtomicInteger(0);
    private static final AtomicInteger BAKED_MESH_COUNT = new AtomicInteger(0);

    private MeshCacheRegistry() {}

    public static int reloadEpoch() {
        return RELOAD_EPOCH.get();
    }

    public static int preBakedMeshCount() {
        return BAKED_MESH_COUNT.get();
    }

    public static void recordBakedMesh() {
        BAKED_MESH_COUNT.incrementAndGet();
    }

    public static void register(Runnable clearAction) {
        if (clearAction != null) {
            CLEAR_HOOKS.add(clearAction);
        }
    }

    public static void clearAll() {
        RELOAD_EPOCH.incrementAndGet();
        BAKED_MESH_COUNT.set(0);
        for (Runnable hook : CLEAR_HOOKS) {
            try {
                hook.run();
            } catch (Throwable ignored) {
                // Keep reload/logout teardown resilient
            }
        }
    }

    public static List<String> detectedAdapters() {
        List<String> adapters = new ArrayList<>();
        adapters.add("Vanilla");
        if (hasClass("software.bernie.geckolib.renderer.GeoRenderer")) {
            adapters.add("GeckoLib");
        }
        if (hasClass("mod.azure.azurelib.common.render.AzModelRenderer")) {
            adapters.add("AzureLib");
        }
        if (hasClass("com.tacz.guns.client.model.bedrock.BedrockPart")) {
            adapters.add("TaCZ");
        }
        if (hasClass("com.github.tartaricacid.touhoulittlemaid.compat.sodium.SodiumGeoRenderer")) {
            adapters.add("TLM");
        }
        if (hasClass("com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart")) {
            adapters.add("SimpleBedrock");
        }
        if (hasClass("com.github.ysbbbbbb.kaleidoscopedoll.client.bedrock.model.BedrockPart")) {
            adapters.add("KaleidoscopeDoll");
        }
        if (hasClass("mods.flammpfeil.slashblade.client.renderer.model.obj.GroupObject")) {
            adapters.add("SlashBlade");
        }
        return adapters;
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name, false, MeshCacheRegistry.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }
}
