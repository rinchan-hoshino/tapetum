package dev.rinchan.tapetum.fabric;

import dev.rinchan.tapetum.Tapetum;
import net.fabricmc.api.ClientModInitializer;

public final class TapetumFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Tapetum.LOGGER.debug("{} client hooks ready on Fabric", Tapetum.MOD_NAME);
    }
}
