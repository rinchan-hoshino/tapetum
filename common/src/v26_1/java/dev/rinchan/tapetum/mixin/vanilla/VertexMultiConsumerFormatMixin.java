package dev.rinchan.tapetum.mixin.vanilla;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import dev.rinchan.tapetum.vertex.FormatAwareWriter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "com.mojang.blaze3d.vertex.VertexMultiConsumer$Double")
public abstract class VertexMultiConsumerFormatMixin implements FormatAwareWriter {
    @Shadow
    @Final
    private VertexConsumer first;

    @Shadow
    @Final
    private VertexConsumer second;

    @Override
    public VertexFormat tapetum$getTargetFormat() {
        if (!TapetumConfig.isEnchantedGlintFastWriter()) {
            return null;
        }
        VertexFormat f1 = FastEntityVertexWriter.extractFormat(this.first);
        VertexFormat f2 = FastEntityVertexWriter.extractFormat(this.second);
        return (f1 != null && f1 == f2) ? f1 : null;
    }
}
