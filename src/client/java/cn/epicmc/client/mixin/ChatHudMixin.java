package cn.epicmc.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    private static final float CHAT_SCALE = 0.72f;
    private static final float CHAT_LEFT_MARGIN = 8.0f;
    private static final float CHAT_TOP_MARGIN = 34.0f;

    @Inject(method = "render", at = @At("HEAD"))
    private void moveAndScaleChat(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        context.getMatrices().push();
        context.getMatrices().translate(
                CHAT_LEFT_MARGIN,
                CHAT_TOP_MARGIN - context.getScaledWindowHeight() * CHAT_SCALE,
                0.0f
        );
        context.getMatrices().scale(CHAT_SCALE, CHAT_SCALE, 1.0f);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void restoreChatMatrix(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        context.getMatrices().pop();
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"
            )
    )
    private void renderTransparentChatBackground(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        int alpha = ((color >>> 24) & 0xFF) * 28 / 100;
        context.fill(x1, y1, x2, y2, (color & 0x00FFFFFF) | (alpha << 24));
    }
}
