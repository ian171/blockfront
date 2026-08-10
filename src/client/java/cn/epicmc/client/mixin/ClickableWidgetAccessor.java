package cn.epicmc.client.mixin;

import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * ClickableWidget 访问器 - 用于访问 protected 字段
 */
@Mixin(ClickableWidget.class)
public interface ClickableWidgetAccessor {

    @Accessor("alpha")
    float getAlpha();

    @Accessor("width")
    int getWidthField();

    @Accessor("height")
    int getHeightField();
}
