package cn.epicmc.client.screen;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.*;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

/**
 * 使用 owo-ui 重写的选项菜单
 * 移除：材质包、遥感、在线选项、鸣谢
 */
public class CustomOptionsScreen extends BaseOwoScreen<FlowLayout> {
    private final Screen parent;

    public CustomOptionsScreen(Screen parent) {
        this.parent = parent;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
            .surface(Surface.VANILLA_TRANSLUCENT)
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER)
            .padding(Insets.of(10));

        // 标题
        var title = Components.label(Text.translatable("options.title"));
        title.shadow(true);
        title.margins(Insets.bottom(20));
        rootComponent.child(title);

        // 主按钮容器
        var buttonContainer = Containers.verticalFlow(Sizing.content(), Sizing.content());
        buttonContainer.gap(4);

        // 第一列按钮
        var row1 = createButtonRow();
        row1.child(createButton("options.fov", () -> new VideoOptionsScreen(this, this.client,this.client.options)));
        row1.child(createButton("options.sounds", () -> new SoundOptionsScreen(this, this.client.options)));
        buttonContainer.child(row1);

        // 第二列按钮
        var row2 = createButtonRow();
        row2.child(createButton("options.controls", () -> new ControlsOptionsScreen(this, this.client.options)));
        row2.child(createButton("options.language", () -> new LanguageOptionsScreen(this, this.client.options, this.client.getLanguageManager())));
        buttonContainer.child(row2);

        // 第三列按钮
        var row3 = createButtonRow();
        row3.child(createButton("options.chat.title", () -> new ChatOptionsScreen(this, this.client.options)));
        row3.child(createButton("options.skinCustomisation", () -> new SkinOptionsScreen(this, this.client.options)));
        buttonContainer.child(row3);

        // 第四列按钮
        var row4 = createButtonRow();
        row4.child(createButton("options.accessibility.title", () -> new AccessibilityOptionsScreen(this, this.client.options)));
        buttonContainer.child(row4);

        rootComponent.child(buttonContainer);

        // 完成按钮
        var doneButton = Components.button(
            Text.translatable("gui.done"),
            button -> this.close()
        );
        doneButton.horizontalSizing(Sizing.fixed(200));
        doneButton.margins(Insets.top(20));
        rootComponent.child(doneButton);
    }

    private FlowLayout createButtonRow() {
        var row = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        row.gap(4);
        return row;
    }

    private ButtonComponent createButton(String translationKey, ScreenSupplier screenSupplier) {
        var button = Components.button(
            Text.translatable(translationKey),
            btn -> {
                if (this.client != null) {
                    this.client.setScreen(screenSupplier.createScreen());
                }
            }
        );
        button.horizontalSizing(Sizing.fixed(150));
        return button;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return super.shouldPause();
    }

    @FunctionalInterface
    private interface ScreenSupplier {
        Screen createScreen();
    }
}
