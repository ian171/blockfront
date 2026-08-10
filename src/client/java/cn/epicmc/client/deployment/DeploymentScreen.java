package cn.epicmc.client.deployment;

import cn.epicmc.client.network.DeployRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

/** Transparent screen layered over the live spectator world for selecting a deployment point. */
public final class DeploymentScreen extends Screen {
    private static final int PANEL = 0xD7141A20;
    private static final int TEXT = 0xFFF2F0E7;
    private static final int DIM = 0xFF9DA4A5;
    private static final int ATTACK = 0xFF84B9E8;
    private static final int ACTIVE = 0xFFE1B34C;
    private String selectedId;

    public DeploymentScreen() { super(Text.literal("部署")); }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        context.fill(0, 0, width, 38, 0x9C070A0D);
        centered(context, "战术部署", width / 2, 11, TEXT, true);
        centered(context, DeploymentManager.descending() ? "正在投入战场" : "选择可部署位置", width / 2, 24, DIM, false);
        if (DeploymentManager.descending()) {
            int alpha = Math.min(150, (int) (DeploymentManager.elapsedMillis() * 0.4));
            context.fill(0, 0, width, height, alpha << 24);
            centered(context, "镜头正在下降", width / 2, height / 2 - 5, TEXT, true);
            return;
        }
        List<DeploymentManager.DeploymentTarget> targets = DeploymentManager.targets();
        int listWidth = Math.min(width - 28, Math.max(258, targets.size() * 124 + Math.max(0, targets.size() - 1) * 8));
        int x = (width - listWidth) / 2;
        int y = height - 76;
        context.fill(x, y, x + listWidth, y + 60, PANEL);
        context.fill(x, y, x + listWidth, y + 1, ATTACK);
        int cardWidth = Math.max(116, (listWidth - 16 - Math.max(0, targets.size() - 1) * 8) / Math.max(1, targets.size()));
        for (int i = 0; i < targets.size(); i++) {
            DeploymentManager.DeploymentTarget target = targets.get(i);
            int cardX = x + 8 + i * (cardWidth + 8);
            boolean selected = target.id().equals(selectedId);
            int border = selected ? ACTIVE : ATTACK;
            context.fill(cardX, y + 8, cardX + cardWidth, y + 51, selected ? 0xD528333A : 0x9C0C1015);
            context.fill(cardX, y + 8, cardX + cardWidth, y + 9, border);
            drawFlag(context, cardX + 10, y + 19, border);
            context.drawText(textRenderer, Text.literal(target.name()), cardX + 28, y + 16, TEXT, true);
            String position = Math.round(target.position().x) + ", " + Math.round(target.position().z);
            context.drawText(textRenderer, Text.literal(position), cardX + 28, y + 30, DIM, false);
            if (selected) context.drawText(textRenderer, Text.literal("点击确认"), cardX + 28, y + 41, ACTIVE, false);
        }
        if (targets.isEmpty()) centered(context, "等待服务器提供可部署位置", width / 2, height / 2, DIM, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !DeploymentManager.selecting()) return true;
        List<DeploymentManager.DeploymentTarget> targets = DeploymentManager.targets();
        int listWidth = Math.min(width - 28, Math.max(258, targets.size() * 124 + Math.max(0, targets.size() - 1) * 8));
        int startX = (width - listWidth) / 2 + 8;
        int cardWidth = Math.max(116, (listWidth - 16 - Math.max(0, targets.size() - 1) * 8) / Math.max(1, targets.size()));
        int y = height - 76;
        for (int i = 0; i < targets.size(); i++) {
            int cardX = startX + i * (cardWidth + 8);
            if (mouseX >= cardX && mouseX <= cardX + cardWidth && mouseY >= y + 8 && mouseY <= y + 51) {
                String id = targets.get(i).id();
                if (id.equals(selectedId)) ClientPlayNetworking.send(new DeployRequestPayload(id)); else selectedId = id;
                return true;
            }
        }
        return true;
    }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }

    private void centered(DrawContext context, String value, int x, int y, int color, boolean shadow) {
        context.drawText(textRenderer, Text.literal(value), x - textRenderer.getWidth(value) / 2, y, color, shadow);
    }
    private static void drawFlag(DrawContext context, int x, int y, int color) {
        context.fill(x, y, x + 2, y + 15, color);
        context.fill(x + 2, y + 1, x + 14, y + 5, color);
        context.fill(x + 8, y + 5, x + 14, y + 7, color);
    }
}
