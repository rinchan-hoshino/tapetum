package dev.rinchan.tapetum.mixin.vanilla;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.rinchan.tapetum.config.TapetumConfig;
import dev.rinchan.tapetum.vertex.FastEntityVertexWriter;
import dev.rinchan.tapetum.vertex.FormatAwareWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
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
    public VertexFormatDescription tapetum$getTargetFormatDesc() {
        if (!TapetumConfig.isEnchantedGlintFastWriter()) {
            return null;
        }
        VertexFormatDescription f1 = FastEntityVertexWriter.extractFormatDesc(this.first);
        VertexFormatDescription f2 = FastEntityVertexWriter.extractFormatDesc(this.second);
        return (f1 != null && f1 == f2) ? f1 : null;
    }
}
