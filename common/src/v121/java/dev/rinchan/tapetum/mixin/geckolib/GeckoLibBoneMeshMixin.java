package dev.rinchan.tapetum.mixin.geckolib;

import dev.rinchan.tapetum.geometry.BakedQuadMesh;
import dev.rinchan.tapetum.geometry.BoneMeshAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.cache.object.GeoBone;

@Pseudo
@Mixin(value = GeoBone.class, remap = false)
public abstract class GeckoLibBoneMeshMixin implements BoneMeshAccess {
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
