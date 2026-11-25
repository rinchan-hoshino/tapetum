package mods.flammpfeil.slashblade.client.renderer.model.obj;

import java.awt.Color;
import java.util.function.BiFunction;
import org.joml.Vector4f;

public class Face {
    public static boolean isSmoothShade;
    public static int lightmap;
    public static Color col;
    public static Vector4f uvOperator;
    public static BiFunction<Vector4f, Integer, Integer> alphaOverride;
    public static final BiFunction<Vector4f, Integer, Integer> alphaOverrideYZZ = null;
    public Vertex[] vertices;
    public Vertex[] vertexNormals;
    public Vertex faceNormal;
    public TextureCoordinate[] textureCoordinates;
    public boolean[] texUSign;
    public boolean[] texVSign;
    public Vertex calculateFaceNormal() { return new Vertex(); }
}
