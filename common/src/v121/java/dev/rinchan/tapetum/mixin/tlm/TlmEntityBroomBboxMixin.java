package dev.rinchan.tapetum.mixin.tlm;

import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityBroom;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class TlmEntityBroomBboxMixin {
    @Shadow
    private AABB bb;

    @WrapOperation(
            method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 tapetum$useBroomPhysicalBbox(Entity self, Vec3 vec, Operation<Vec3> original) {
        if (self instanceof EntityBroom broom) {
            AABB saved = this.bb;
            AABB physical = broom.getPhysicalBoundingBox();
            if (physical != null) {
                this.bb = physical;
            }
            try {
                return original.call(self, vec);
            } finally {
                this.bb = saved;
            }
        }
        return original.call(self, vec);
    }
}
