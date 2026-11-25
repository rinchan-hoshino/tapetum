package com.github.tartaricacid.touhoulittlemaid.entity.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public abstract class EntityBroom extends Entity {
    protected EntityBroom() { super(null, null); }
    public AABB getPhysicalBoundingBox() { return null; }
}
