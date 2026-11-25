package mods.flammpfeil.slashblade.client.renderer.model;

import com.google.common.cache.LoadingCache;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import net.minecraft.resources.ResourceLocation;

public class BladeModelManager {
    public LoadingCache<ResourceLocation, WavefrontObject> cache;
    public WavefrontObject defaultModel;
}
