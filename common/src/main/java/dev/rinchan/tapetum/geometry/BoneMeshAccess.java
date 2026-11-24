package dev.rinchan.tapetum.geometry;

public interface BoneMeshAccess {
    BakedQuadMesh tapetum$getBakedMesh();

    void tapetum$setBakedMesh(BakedQuadMesh mesh, int epoch);

    int tapetum$getBakedEpoch();
}
