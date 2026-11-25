package mod.azure.azurelib.common.cache.object;

import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.joml.Vector3d;

public class GeoCube {
    public GeoQuad[] quads() { return new GeoQuad[0]; }
    public Vector3d pivot() { return new Vector3d(); }
    public Vector3d rotation() { return new Vector3d(); }
    public Transform transform() { return null; }

    public static class Transform {
        public boolean identity() { return true; }
        public Matrix4fc pose() { return null; }
        public Matrix3fc normal() { return null; }
    }
}
