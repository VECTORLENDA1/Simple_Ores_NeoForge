package com.exoticmoss.simpleores.mixin;

import com.exoticmoss.simpleores.gravitycraft.CorePicking;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// After Minecraft works out what the player is looking at, the Gravity Cores can still be hit on their whole
/// sphere (it is bigger than the block). See CorePicking.
@Mixin(Minecraft.class)
public abstract class MinecraftPickMixin {
    @Inject(method = "pick(F)V", at = @At("TAIL"))
    private void simpleores$pickCores(float partialTick, CallbackInfo ci) {
        CorePicking.pick((Minecraft) (Object) this, partialTick);
    }
}
