package net.caffeinemc.mods.sodium.api.vertex.format;

public interface VertexFormatDescription {
    int id();
    int stride();
    boolean isSimpleFormat();
}
