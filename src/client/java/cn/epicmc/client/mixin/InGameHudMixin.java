package cn.epicmc.client.mixin;

import cn.epicmc.client.hud.BattlefieldHudRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        // 渲染战地风格 HUD
        BattlefieldHudRenderer.render(context, tickCounter.getTickDelta(false));
    }

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void hideVanillaStatusBars(DrawContext context, CallbackInfo ci) {
        // 自定义 HUD 已显示生命值与护甲，因此不再渲染原版生命、饥饿和护甲栏。
        ci.cancel();
    }
}
