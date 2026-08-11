package cn.epicmc.client.mixin;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
@Mixin(value = ClickableWidget.class)
public abstract class ClickWeigh {
    @Shadow
    protected float alpha;

    @Shadow
    protected int width;

    @Shadow
    protected int height;

    @Shadow
    public boolean active;

    // 从父类 ClickableWidget 继承的方法
    @Shadow
    public abstract int getX();

    @Shadow
    public abstract int getY();

    @Shadow
    public abstract boolean isSelected();

    @Shadow
    public abstract Text getMessage();
}
