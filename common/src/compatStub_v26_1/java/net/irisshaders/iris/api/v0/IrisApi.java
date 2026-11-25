package net.irisshaders.iris.api.v0;

public interface IrisApi {
    static IrisApi getInstance() { throw new UnsupportedOperationException(); }
    boolean isShaderPackInUse();
    boolean isRenderingShadowPass();
}
