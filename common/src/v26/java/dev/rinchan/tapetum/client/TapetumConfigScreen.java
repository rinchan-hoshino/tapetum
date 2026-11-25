package dev.rinchan.tapetum.client;

import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.geometry.MeshCacheRegistry;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import dev.rinchan.tapetum.vertex.IrisVertexBridge;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class TapetumConfigScreen extends Screen {
    private static final double MIN_CULL_DISTANCE = 8.0;
    private static final double MAX_CULL_DISTANCE = 128.0;

    private final Screen parent;
    private TapetumConfig.Data draft;

    public TapetumConfigScreen(Screen parent) {
        super(Component.translatable("tapetum.config.title"));
        this.parent = parent;
        this.draft = TapetumConfig.get().copy();
    }

    @Override
    protected void init() {
        int btnWidth = Math.min(280, this.width - 40);
        int left = (this.width - btnWidth) / 2;
        int y = 78;
        int step = 22;

        addToggle(left, y, btnWidth, "tapetum.config.vanillaModelPart54B",
                () -> draft.vanillaModelPart54B, v -> draft.vanillaModelPart54B = v);
        y += step;

        addToggle(left, y, btnWidth, "tapetum.config.enchantedGlintFastWriter",
                () -> draft.enchantedGlintFastWriter, v -> draft.enchantedGlintFastWriter = v);
        y += step;

        addToggle(left, y, btnWidth, "tapetum.config.smartShadowCulling",
                () -> draft.smartShadowCulling, v -> draft.smartShadowCulling = v);
        y += step;

        addRenderableWidget(new DistanceSlider(left, y, btnWidth, 20, draft.shadowCullDistance));

        int bottomY = this.height - 28;
        int halfWidth = (btnWidth - 8) / 2;
        addRenderableWidget(Button.builder(
                        Component.translatable("tapetum.config.reset"),
                        btn -> {
                            this.draft = new TapetumConfig.Data();
                            this.rebuildWidgets();
                        })
                .bounds(left, bottomY, halfWidth, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("tapetum.config.saveAndDone"),
                        btn -> {
                            TapetumConfig.applyAndSave(draft);
                            onClose();
                        })
                .bounds(left + halfWidth + 8, bottomY, halfWidth, 20)
                .build());
    }

    private void addToggle(int x, int y, int width, String key, BooleanGetter getter, BooleanSetter setter) {
        addRenderableWidget(Button.builder(
                        formatToggleLabel(key, getter.get()),
                        btn -> {
                            boolean next = !getter.get();
                            setter.set(next);
                            btn.setMessage(formatToggleLabel(key, next));
                        })
                .bounds(x, y, width, 20)
                .build());
    }

    private static Component formatToggleLabel(String key, boolean value) {
        return Component.translatable(key)
                .append(": ")
                .append(value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.centeredText(this.font, this.title, this.width / 2, 10, 0xFFEAF6F6);

        int cardWidth = Math.min(480, this.width - 24);
        int cardX = (this.width - cardWidth) / 2;
        int cardY = 24;
        int cardH = 48;

        graphics.fill(cardX, cardY, cardX + cardWidth, cardY + cardH, 0xAA0D131A);
        graphics.fill(cardX, cardY, cardX + cardWidth, cardY + 1, 0xFF3A7B7C);
        graphics.fill(cardX, cardY + cardH - 1, cardX + cardWidth, cardY + cardH, 0xFF3A7B7C);

        boolean active54B = FastEntityVertexWriter.isSodiumPresent() && IrisVertexBridge.isIris54BVerified();
        Component writerStatus = Component.translatable(
                active54B ? "tapetum.config.status.active" : "tapetum.config.status.standby");
        Component writerLine = Component.translatable("tapetum.config.status.writer", writerStatus);
        graphics.text(
                this.font, writerLine, cardX + 8, cardY + 6, active54B ? 0xFF66FFAA : 0xFFFFC857);

        List<String> adapters = MeshCacheRegistry.detectedAdapters();
        String adapterText = String.join(", ", adapters);
        Component adaptersLine = Component.translatable("tapetum.config.status.adapters", adapterText);
        String adaptersStr = adaptersLine.getString();
        int maxTextWidth = cardWidth - 16;
        if (this.font.width(adaptersStr) > maxTextWidth) {
            adaptersStr = this.font.plainSubstrByWidth(adaptersStr, Math.max(0, maxTextWidth - this.font.width("..."))) + "...";
        }
        graphics.text(this.font, adaptersStr, cardX + 8, cardY + 20, 0xFFCFE8E8);

        Component meshesLine = Component.translatable(
                "tapetum.config.status.meshes", MeshCacheRegistry.preBakedMeshCount());
        graphics.text(this.font, meshesLine, cardX + 8, cardY + 34, 0xFF9BE3DE);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    @FunctionalInterface
    private interface BooleanGetter {
        boolean get();
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }

    private final class DistanceSlider extends AbstractSliderButton {
        DistanceSlider(int x, int y, int width, int height, double currentDistance) {
            super(
                    x, y, width, height, Component.empty(),
                    (TapetumConfig.clampDistance(currentDistance) - MIN_CULL_DISTANCE)
                            / (MAX_CULL_DISTANCE - MIN_CULL_DISTANCE));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int blocks = (int) Math.round(draft.shadowCullDistance);
            setMessage(Component.translatable("tapetum.config.shadowCullDistance", blocks));
        }

        @Override
        protected void applyValue() {
            double raw = MIN_CULL_DISTANCE + this.value * (MAX_CULL_DISTANCE - MIN_CULL_DISTANCE);
            draft.shadowCullDistance = Math.round(TapetumConfig.clampDistance(raw));
        }
    }
}
