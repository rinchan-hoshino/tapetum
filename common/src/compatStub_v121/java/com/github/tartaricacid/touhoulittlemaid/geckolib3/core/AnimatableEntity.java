package com.github.tartaricacid.touhoulittlemaid.geckolib3.core;

import net.minecraft.world.entity.Entity;

public interface AnimatableEntity<T extends Entity> {
    T getEntity();
}
