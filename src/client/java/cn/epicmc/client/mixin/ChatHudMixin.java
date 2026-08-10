package cn.epicmc.client.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 聊天栏 Mixin - 移动到左上角，背景透明
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Shadow
    private int scrolledLines;

    private static boolean isRendering = false;

    /**
     * 修改聊天栏背景透明度
     * 将背景设置为完全透明
     */
    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"
        ),
        index = 4
    )
    private int modifyBackgroundColor(int color) {
        // 将背景设置为完全透明
        return 0x00000000;
    }

    /**
     * 在渲染开始时应用坐标变换 - 移动到左上角
     */
    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    private void beforeRender(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (!isRendering) {
            isRendering = true;
            var matrices = context.getMatrices();
            matrices.push();

            // 将聊天栏从左下角移动到左上角
            // 计算需要向上移动的距离
            int screenHeight = context.getScaledWindowHeight();
            int chatHeight = 180; // 聊天栏大约高度（9行 * 20px）

            // 移动到顶部，距离顶部 10px
            matrices.translate(0, -screenHeight + chatHeight + 10, 0);
        }
    }

    /**
     * 在渲染结束后恢复坐标系统
     */
    @Inject(
        method = "render",
        at = @At("RETURN")
    )
    private void afterRender(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (isRendering) {
            context.getMatrices().pop();
            isRendering = false;
        }
    }
}
