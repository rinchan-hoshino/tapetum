package net.irisshaders.iris.vertices;

import com.mojang.renderpearl.api.vertex.VertexFormat;

public class IrisVertexFormats {
    public static final String ENTITY_ATTRIBUTE = "mc_Entity";
    public static final String ENTITY_ID_ATTRIBUTE = "iris_Entity";
    public static final String MID_TEXTURE_ATTRIBUTE = "mc_midTexCoord";
    public static final String TANGENT_ATTRIBUTE = "at_tangent";
    public static final VertexFormat ENTITY = null;
    public static int getOffset(VertexFormat format, String attribute) { return -1; }
}
