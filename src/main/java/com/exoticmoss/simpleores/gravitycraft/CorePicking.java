package com.exoticmoss.simpleores.gravitycraft;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/// The sphere of a Gravity Core is bigger than its block, but Minecraft only knows blocks: looking at the part
/// of the sphere outside the block would miss it. After Minecraft picks what the player looks at
/// (MinecraftPickMixin), this checks the spheres of the cores nearby: if the look hits one first, the player is
/// looking at that core (so it can be clicked and broken on its whole sphere).
public final class CorePicking {
    private CorePicking() {
    }

    public static void pick(Minecraft minecraft, float partialTick) {
        if (minecraft.player == null || minecraft.level == null) return;
        Vec3 eye = minecraft.player.getEyePosition(partialTick);
        Vec3 look = minecraft.player.getViewVector(partialTick);
        double reach = minecraft.player.blockInteractionRange();
        HitResult current = minecraft.hitResult;
        double best = current != null && current.getType() != HitResult.Type.MISS ? current.getLocation().distanceTo(eye) : reach;

        GravityCoreBlockEntity found = null;
        int chunkX = minecraft.player.blockPosition().getX() >> 4, chunkZ = minecraft.player.blockPosition().getZ() >> 4;
        for (int cx = chunkX - 1; cx <= chunkX + 1; cx++) {
            for (int cz = chunkZ - 1; cz <= chunkZ + 1; cz++) {
                LevelChunk chunk = minecraft.level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : List.copyOf(chunk.getBlockEntities().values())) {
                    if (!(blockEntity instanceof GravityCoreBlockEntity core)) continue;
                    double radius = core.getTier().coreSize / 2;
                    Vec3 center = new Vec3(core.getBlockPos().getX() + 0.5, core.getBlockPos().getY() + radius, core.getBlockPos().getZ() + 0.5);
                    double distance = hit(eye, look, center, radius);
                    if (distance >= 0 && distance < best) {
                        best = distance;
                        found = core;
                    }
                }
            }
        }
        if (found == null) return;
        // The server only accepts a hit point inside the block: the point is moved inside it, towards the hit
        Vec3 blockCenter = Vec3.atCenterOf(found.getBlockPos());
        Vec3 point = eye.add(look.scale(best));
        Vec3 inside = blockCenter.add(point.subtract(blockCenter).normalize().scale(0.45));
        minecraft.hitResult = new BlockHitResult(inside, Direction.UP, found.getBlockPos(), false);
        minecraft.crosshairPickEntity = null;
    }

    /// Distance along the look (from the eye) to where it first touches the sphere, or -1 if it doesn't.
    private static double hit(Vec3 eye, Vec3 look, Vec3 center, double radius) {
        Vec3 toEye = eye.subtract(center);
        double b = toEye.dot(look);
        double c = toEye.dot(toEye) - radius * radius;
        double disc = b * b - c;
        if (disc < 0) return -1;
        double t = -b - Math.sqrt(disc);
        if (t < 0) t = -b + Math.sqrt(disc);
        return t;
    }
}
