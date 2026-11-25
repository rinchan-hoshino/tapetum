package mod.azure.azurelib.common.model;

import java.util.List;
import mod.azure.azurelib.common.cache.object.GeoCube;

public class AzBone {
    public boolean isHidden() { return false; }
    public List<GeoCube> getCubes() { return List.of(); }
    public List<?> getMeshes() { return List.of(); }
    public AzSkinBinding[] getSkinBindings() { return new AzSkinBinding[0]; }
}
