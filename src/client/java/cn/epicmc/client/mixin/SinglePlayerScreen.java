package cn.epicmc.client.mixin;

import cn.epicmc.client.config.ClientConfig;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SelectWorldScreen.class)
public class SinglePlayerScreen {
    @Inject(method = "init", at = @At(value = "TAIL"))
    public void init(CallbackInfo ci) throws IllegalAccessException {
        if (!ClientConfig.getInstance().isAllowSingleplayer()) {
            throw new IllegalAccessException("Singleplayer is disabled by server configuration");
        }
    }
}
