package dev.rinchan.tapetum.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.rinchan.tapetum.client.TapetumConfigScreen;

public final class TapetumModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TapetumConfigScreen::new;
    }
}
