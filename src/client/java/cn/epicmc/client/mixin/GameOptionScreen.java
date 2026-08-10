package cn.epicmc.client.mixin;

import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OptionsScreen.class)
public class GameOptionScreen {
    @Inject(method = "close", at = @At(value = "HEAD"))
    public void close(CallbackInfo ci) {

    }
}
