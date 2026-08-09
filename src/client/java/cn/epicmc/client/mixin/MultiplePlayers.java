package cn.epicmc.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MultiplayerScreen.class)
public class MultiplePlayers {
    @Inject(method = "init", at = @At(value = "TAIL"))
    public void init(CallbackInfo callbackInfo) {
        MinecraftClient.getInstance().scheduleStop();
    }
}
