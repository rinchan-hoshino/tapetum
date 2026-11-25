package dev.rinchan.tapetum.mixin.tlm;

import dev.rinchan.tapetum.geometry.BakedQuadMesh;
import dev.rinchan.tapetum.geometry.BoneMeshAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(
        targets = "com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoBone",
        remap = false)
public abstract class TlmGeoBoneMeshMixin implements BoneMeshAccess {
    @Unique
    private BakedQuadMesh tapetum$bakedMesh;

    @Unique
    private int tapetum$bakedEpoch = -1;

    @Override
    public BakedQuadMesh tapetum$getBakedMesh() {
        return this.tapetum$bakedMesh;
    }

    @Override
    public void tapetum$setBakedMesh(BakedQuadMesh mesh, int epoch) {
        this.tapetum$bakedMesh = mesh;
        this.tapetum$bakedEpoch = epoch;
    }

    @Override
    public int tapetum$getBakedEpoch() {
        return this.tapetum$bakedEpoch;
    }
}
