package com.exoticmoss.simpleores.mixin.client;

import com.exoticmoss.simpleores.screen.CoreSelectionBar;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// Right-click on the selection bar above a Gravity Core (see CoreSelectionBar).
/// Replaces NeoForge's InputEvent.InteractionKeyMappingTriggered: when the click is for the bar,
/// the item in hand / the block behind the bar is not used and the hand doesn't swing.
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "startUseItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z"),
            cancellable = true)
    private void simpleores$useOnCoreBar(CallbackInfo ci) {
        if (CoreSelectionBar.onUse()) {
            ci.cancel();
        }
    }
}
