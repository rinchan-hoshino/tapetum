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

public final class TapetumMixinPlugin implements IMixinConfigPlugin {
    private static final String SODIUM_WRITER = "net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter";
    private static final String SODIUM_MULTI_CONSUMER =
            "net.caffeinemc.mods.sodium.mixin.core.render.immediate.consumer.VertexMultiConsumerMixin$DoubleMixin";
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
        if (!isClientSide()) {
            return mixins;
        }
        if (hasAll(SODIUM_WRITER)) {
            mixins.add("vanilla.BufferBuilderFormatAccessor");
            mixins.add("vanilla.VanillaCubeFastMixin");
            mixins.add("vanilla.VertexMultiConsumerFormatMixin");
            if (!hasAll(SODIUM_MULTI_CONSUMER)) {
                mixins.add("vanilla.VertexMultiConsumerFastMixin");
            }
        }
        if (hasAll(IRIS_SHADOW)) {
            mixins.add("shadow.DecorativeEntityShadowMixin");
        }
        return mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

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
