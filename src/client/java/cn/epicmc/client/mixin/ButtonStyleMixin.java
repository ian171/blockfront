package cn.epicmc.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 按钮样式 Mixin - 半透明亚克力 ImGui 风格
 * 不使用 Shadow，通过对象引用访问所有属性
 */
@Mixin(PressableWidget.class)
public abstract class ButtonStyleMixin {

    @Inject(
        method = "renderWidget(Lnet/minecraft/client/gui/DrawContext;IIF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void renderAcrylicButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        PressableWidget widget = (PressableWidget) (Object) this;
        ClickableWidgetAccessor accessor = (ClickableWidgetAccessor) this;
        MinecraftClient client = MinecraftClient.getInstance();

        // 通过 getter 方法和 accessor 访问所有属性
        int x = widget.getX();
        int y = widget.getY();
        int width = widget.getWidth();
        int height = widget.getHeight();
        boolean hovered = widget.isSelected();
        boolean active = widget.active;
        float alpha = accessor.getAlpha();

        // 背景颜色 - 半透明亚克力风格
        int backgroundColor;
        int borderColor;
        int textColor;

        if (!active) {
            backgroundColor = 0x40808080;
            borderColor = 0x60404040;
            textColor = 10526880;
        } else if (hovered) {
            backgroundColor = 0x80FFFFFF;
            borderColor = 0xC0FFFFFF;
            textColor = 16777215;
        } else {
            backgroundColor = 0x60202020;
            borderColor = 0x80404040;
            textColor = 16777215;
        }

        // 应用 alpha
        int alphaValue = (int) (alpha * 255.0F);
        backgroundColor = (backgroundColor & 0x00FFFFFF) | (alphaValue << 24);
        borderColor = (borderColor & 0x00FFFFFF) | ((alphaValue * 3 / 4) << 24);

        // 启用混合和深度测试
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        // 渲染背景
        context.fill(x, y, x + width, y + height, backgroundColor);

        // 渲染边框
        context.fill(x, y, x + width, y + 1, borderColor);
        context.fill(x, y + height - 1, x + width, y + height, borderColor);
        context.fill(x, y, x + 1, y + height, borderColor);
        context.fill(x + width - 1, y, x + width, y + height, borderColor);

        // 悬停发光
        if (hovered && active) {
            int glowColor = 0x40FFFFFF | (alphaValue << 24);
            context.fill(x + 1, y + 1, x + width - 1, y + height - 1, glowColor);
        }

        // 渲染文字
        context.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        int finalTextColor = textColor | (alphaValue << 24);
        widget.drawMessage(context, client.textRenderer, finalTextColor);
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        ci.cancel();
    }
}
