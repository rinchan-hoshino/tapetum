package dev.rinchan.tapetum.mixin.vanilla;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import dev.rinchan.tapetum.vertex.FormatAwareWriter;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "com.mojang.blaze3d.vertex.VertexMultiConsumer$Double")
public abstract class VertexMultiConsumerFastMixin implements VertexBufferWriter, FormatAwareWriter {
    @Shadow
    @Final
    private VertexConsumer first;

    @Shadow
    @Final
    private VertexConsumer second;

    @Override
    public boolean canUseIntrinsics() {
        if (!TapetumConfig.isEnchantedGlintFastWriter()) {
            return false;
        }
        VertexBufferWriter w1 = VertexBufferWriter.tryOf(this.first);
        VertexBufferWriter w2 = VertexBufferWriter.tryOf(this.second);
        return w1 != null && w2 != null;
    }

    @Override
    public VertexFormat tapetum$getTargetFormat() {
        VertexFormat f1 = FastEntityVertexWriter.extractFormat(this.first);
        VertexFormat f2 = FastEntityVertexWriter.extractFormat(this.second);
        return (f1 != null && f1 == f2) ? f1 : null;
    }

    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormat format) {
        VertexBufferWriter.of(this.first).push(stack, ptr, count, format);
        VertexBufferWriter.of(this.second).push(stack, ptr, count, format);
    }
}
