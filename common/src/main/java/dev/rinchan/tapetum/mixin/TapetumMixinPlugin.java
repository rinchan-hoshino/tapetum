package dev.rinchan.tapetum.mixin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/**
 * Dynamic Mixin plugin for Tapetum.
 *
 * <p>Strictly enforces side isolation (client-only rendering mixins never load on Dedicated Server)
 * and probes optional mod classes via ASM bytecode lookup before enabling each adapter.
 */
public final class TapetumMixinPlugin implements IMixinConfigPlugin {
    private static final String SODIUM_WRITER = "net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter";
    private static final String IRIS_SHADOW = "net.irisshaders.iris.shadows.ShadowRenderer";

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();

        // Dual-side broom bbox optimization (only when TLM + MixinSquared are present)
        if (hasAll(
                "com.github.tartaricacid.touhoulittlemaid.entity.item.EntityBroom",
                "com.github.tartaricacid.touhoulittlemaid.mixin.EntityMixin",
                "com.bawnorton.mixinsquared.api.MixinAnnotationAdjuster")) {
            mixins.add("tlm.TlmEntityBroomBboxMixin");
        }

        if (!isClientSide()) {
            return mixins;
        }

        boolean hasSodium = hasAll(SODIUM_WRITER);
        boolean hasIris = hasAll(IRIS_SHADOW);

        // Layer 1: Universal Vanilla 54B/36B fast paths
        if (hasSodium) {
            mixins.add("vanilla.VanillaCubeFastMixin");
            mixins.add("vanilla.VertexMultiConsumerFastMixin");
        }

        // Layer 1: Wrapper fast-path routers
        if (hasSodium && hasAll("com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.capture.LightPassVertexRouter")) {
            mixins.add("vertex.LightPassVertexRouterFastMixin");
        }
        if (hasSodium && hasAll("com.tacz.guns.client.model.bedrock.UnshadedVertexConsumer")) {
            mixins.add("vertex.UnshadedVertexConsumerFastMixin");
        }

        // Layer 2: GeckoLib 4
        if (hasSodium && hasAll(
                "software.bernie.geckolib.renderer.GeoRenderer",
                "software.bernie.geckolib.cache.object.GeoBone")) {
            mixins.add("geckolib.GeckoLibBoneMeshMixin");
            mixins.add("geckolib.GeckoLibRendererFastMixin");
        }

        // Layer 2: AzureLib
        if (hasSodium && hasAll(
                "mod.azure.azurelib.common.render.AzModelRenderer",
                "mod.azure.azurelib.common.model.AzBone")) {
            mixins.add("azurelib.AzBoneMeshMixin");
            mixins.add("azurelib.AzModelRendererFastMixin");
        }

        // Layer 2: TaCZ
        if (hasSodium && hasAll(
                "com.tacz.guns.client.model.bedrock.BedrockPart",
                "com.tacz.guns.client.model.bedrock.BedrockModel",
                "com.tacz.guns.client.renderer.block.GunSmithTableRenderer",
                "com.tacz.guns.client.renderer.scope.ScopeThermalRenderer")) {
            mixins.add("tacz.GunSmithTableFastRenderMixin");
            mixins.add("tacz.TaCZBedrockPartFastMixin");
            mixins.add("tacz.TaCZBedrockModelBatchMixin");
            mixins.add("tacz.TaCZThermalFastExitMixin");
        }
        if (hasAll(
                "com.tacz.guns.compat.iris.IrisCompat",
                "net.neoforged.fml.ModList",
                "dev.rinchan.tapetum.mixin.tacz.TaCZIrisCompatCacheMixin")) {
            mixins.add("tacz.TaCZIrisCompatCacheMixin");
        }

        // Layer 2: Touhou Little Maid + SimpleBedrockModel v1 + Kaleidoscope Doll
        if (hasSodium && hasAll(
                "com.github.tartaricacid.touhoulittlemaid.compat.sodium.SodiumGeoRenderer",
                "com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoBone",
                "com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.AnimationProcessor")) {
            mixins.add("tlm.TlmGeoBoneMeshMixin");
            mixins.add("tlm.TlmSodiumGeo54BMixin");
            mixins.add("tlm.TlmAnimationFrameCacheMixin");
        }
        if (hasSodium && hasAll("com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart")) {
            mixins.add("tlm.SbmBedrockPartFastMixin");
        }
        if (hasSodium && hasAll(
                "com.github.ysbbbbbb.kaleidoscopedoll.client.bedrock.model.BedrockPart",
                "com.github.ysbbbbbb.kaleidoscopedoll.client.render.DollEntityRender")) {
            mixins.add("tlm.KaleidoscopeDollFastMixin");
            mixins.add("tlm.DollEntityRenderFastMixin");
        }

        // Layer 2: SlashBlade Resharped
        if (hasSodium && hasAll(
                "mods.flammpfeil.slashblade.client.renderer.model.obj.GroupObject",
                "mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager",
                "mods.flammpfeil.slashblade.client.renderer.event.ModelResourceLoader")) {
            mixins.add("slashblade.SlashBladeGroupObjectMixin");
            mixins.add("slashblade.SlashBladeModelCacheMixin");
            mixins.add("slashblade.SlashBladeLazyModelLoaderMixin");
        }

        // Layer 4: Smart Shadow Culling
        if (hasIris) {
            mixins.add("shadow.DecorativeEntityShadowMixin");
            if (hasAll(
                    "com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer",
                    "com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.chatbubble.ChatBubbleRenderer")) {
                mixins.add("shadow.TlmShadowContractMixin$ChatBubble");
                mixins.add("shadow.TlmShadowContractMixin$MaidDetailLayers");
                mixins.add("shadow.TlmShadowContractMixin$MaidBody");
            }
            if (hasAll(
                    "com.tacz.guns.client.renderer.block.GunDisplayCaseRenderer",
                    "com.tacz.guns.client.renderer.block.GunPegboardRenderer",
                    "com.tacz.guns.client.renderer.block.StatueRenderer",
                    "com.tacz.guns.client.renderer.block.TargetRenderer")) {
                mixins.add("shadow.TaCZBlockShadowMixin$DisplayBlocks");
                mixins.add("shadow.TaCZBlockShadowMixin$StaticBlocks");
            }
            if (hasAll(
                    "mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState",
                    "mods.flammpfeil.slashblade.client.renderer.entity.BladeStandEntityRenderer",
                    "mods.flammpfeil.slashblade.client.renderer.layers.LayerSlashBlade")) {
                mixins.add("shadow.SlashBladeShadowMixin$Luminous");
                mixins.add("shadow.SlashBladeShadowMixin$BladeStand");
                mixins.add("shadow.SlashBladeShadowMixin$HotbarBlade");
            }
        }

        return mixins;
    }

    @Override
    public void preApply(
            String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(
            String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    private static boolean isClientSide() {
        return MixinEnvironment.getCurrentEnvironment().getSide() == MixinEnvironment.Side.CLIENT;
    }

    private static boolean hasAll(String... classNames) {
        var bytecode = MixinService.getService().getBytecodeProvider();
        for (String name : classNames) {
            try {
                bytecode.getClassNode(name);
            } catch (ClassNotFoundException | IOException ignored) {
                return false;
            }
        }
        return true;
    }
}
