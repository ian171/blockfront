package cn.epicmc.client.mixin;

import cn.epicmc.client.screen.CustomOptionsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截选项屏幕的初始化，替换为自定义的 owo-ui 版本
 */
@Mixin(OptionsScreen.class)
public abstract class GameOptionScreen extends Screen {

    @Shadow
    @Final
    private Screen parent;

    protected GameOptionScreen() {
        super(null);
    }

    @Inject(method = "init", at = @At("RETURN"))
    public void replaceWithCustomScreen(CallbackInfo ci) {
        // 在 init 完成后立即替换为自定义选项屏幕
        if (this.client != null) {
            this.client.setScreen(new CustomOptionsScreen(this.parent));
        }
    }
}
