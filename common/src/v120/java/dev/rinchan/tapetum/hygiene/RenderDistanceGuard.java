package dev.rinchan.tapetum.hygiene;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class RenderDistanceGuard {
    private RenderDistanceGuard() {}

    public static boolean isBeyondDistSqr(
            double x, double y, double z, double camX, double camY, double camZ, double maxDistSqr) {
        double dx = x - camX;
        double dy = y - camY;
        double dz = z - camZ;
        return (dx * dx + dy * dy + dz * dz) > maxDistSqr;
    }

    public static boolean isBlockBeyondCamera(BlockPos pos, double maxDistSqr) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null || mc.gameRenderer.getMainCamera() == null) {
            return false;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        return isBeyondDistSqr(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, cam.x, cam.y, cam.z, maxDistSqr);
    }

    public static boolean isEntityBeyondCamera(Entity entity, double maxDistSqr) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null || mc.gameRenderer.getMainCamera() == null) {
            return false;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        return isBeyondDistSqr(entity.getX(), entity.getY(), entity.getZ(), cam.x, cam.y, cam.z, maxDistSqr);
    }
}
