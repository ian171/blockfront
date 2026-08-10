package cn.epicmc.client.hud;

import cn.epicmc.client.deployment.DeploymentManager;
import cn.epicmc.client.downed.DownedManager;
import cn.epicmc.client.hud.HudDataManager.CapturePoint;
import cn.epicmc.client.hud.HudDataManager.CaptureState;
import cn.epicmc.client.hud.HudDataManager.KillFeedEntry;
import cn.epicmc.client.hud.HudDataManager.StatusMessage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A full-screen Battlefield-inspired HUD for Shanghai 1937 Operation.
 * Data comes from HudDataManager; rendering is deliberately client-only and frame-driven.
 */
public final class BattlefieldHudRenderer {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final HudDataManager DATA = HudDataManager.getInstance();
    private static final Map<String, OperationHudAnimation> CAPTURE_ANIMATIONS = new HashMap<>();
    private static final OperationHudAnimation HEALTH_ANIMATION = new OperationHudAnimation();
    private static final OperationHudAnimation TICKET_ANIMATION = new OperationHudAnimation();
    private static final Identifier PANEL_TEXTURE = Identifier.of("blockfront", "panel");
    private static final Identifier BAR_BG_TEXTURE = Identifier.of("blockfront", "bar_bg");
    private static final Identifier BAR_FG_TEXTURE = Identifier.of("blockfront", "bar_fg");
    private static final Identifier ICON_FLAG = Identifier.of("blockfront", "icon_flag");
    private static final Identifier ICON_SHIELD = Identifier.of("blockfront", "icon_shield");
    private static final Identifier ICON_SOLDIER = Identifier.of("blockfront", "icon_soldier");
    private static final Identifier ICON_ARMOR = Identifier.of("blockfront", "icon_armor");
    private static final Identifier ICON_CROSS = Identifier.of("blockfront", "icon_cross");
    private static final Identifier PUHUITI_FONT = Identifier.of("blockfront", "puhuiti");
    private static final Style PUHUITI_STYLE = Style.EMPTY.withFont(PUHUITI_FONT);

    private static Text text(String string) {
        return net.minecraft.text.Text.literal(string).setStyle(PUHUITI_STYLE);
    }
    private static float pulse;

    private BattlefieldHudRenderer() { }

    public static void render(DrawContext context, float tickDelta) {
        if (CLIENT.player == null || CLIENT.options.hudHidden) return;
        syncLocalPlayerData();
        DATA.updateKillFeed();
        pulse = (pulse + tickDelta * 0.055f) % 1.0f;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        
        float topScale = 0.85f;
        context.getMatrices().push();
        context.getMatrices().scale(topScale, topScale, 1.0f);
        int scaledWidth = Math.round(width / topScale);
        renderBattleHeader(context, scaledWidth);
        renderCaptureStrip(context, scaledWidth);
        context.getMatrices().pop();
        
        renderKillFeed(context, width, height);
        renderPlayerPanel(context, width, height);
        // 武器面板已移除，全权交由 TACZ 渲染
        renderObjectiveOverlay(context, width, height);
        renderStatusMessage(context, width, height);
    }

    private static void syncLocalPlayerData() {
        if (DownedManager.isDowned()) DATA.setHealth(0.0f, CLIENT.player.getMaxHealth());
        else DATA.setHealth(CLIENT.player.getHealth(), CLIENT.player.getMaxHealth());
        DATA.setArmor(CLIENT.player.getArmor());
        ItemStack stack = CLIENT.player.getMainHandStack();
        DATA.setWeaponName(stack.isEmpty() ? "徒手" : stack.getName().getString());
    }

    /**
     * 计算 UI 缩放因子，根据屏幕高度自适应调整
     * 基准高度: 720p (720像素)
     * 最小缩放: 0.7 (适用于小窗口)
     * 最大缩放: 1.3 (适用于4K等高分辨率)
     */
    private static float calculateScale(int screenHeight) {
        final float BASE_HEIGHT = 720.0f;
        final float MIN_SCALE = 0.6f;
        final float MAX_SCALE = 1.1f;
        float scale = (screenHeight / BASE_HEIGHT) * 0.85f;
        return Math.clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    private static void renderDownedHud(DrawContext context, int width, int height) {
        TextRenderer font = CLIENT.textRenderer;
        int panelWidth = 360;
        int x = (width - panelWidth) / 2;
        int y = height - 105;
        panel(context, x, y, panelWidth, 72, 0xDE170C0C, OperationHudTheme.DANGER);
        drawIcon(context, x + 14, y + 16, Icon.CROSS, OperationHudTheme.DANGER);
        context.drawText(font, text("你已倒地"), x + 38, y + 12, OperationHudTheme.TEXT, true);
        String killer = "击倒者  " + DownedManager.killerName();
        context.drawText(font, text(killer), x + 38, y + 28, OperationHudTheme.DANGER, true);
        String weapon = "使用 " + DownedManager.weaponName();
        context.drawText(font, text(weapon), x + 38, y + 43, OperationHudTheme.TEXT_DIM, false);
        String timer = DownedManager.remainingSeconds() + " 秒";
        context.drawText(font, text(timer), x + panelWidth - 16 - font.getWidth(text(timer)), y + 14, OperationHudTheme.TEXT, true);
        context.drawText(font, text("等待医疗兵救援"), x + panelWidth - 16 - font.getWidth(text("等待医疗兵救援")), y + 30, OperationHudTheme.TEXT_DIM, false);
        int barX = x + 14;
        int barY = y + 58;
        progressBar(context, barX, barY, panelWidth - 28, 6, DownedManager.skipProgress(), OperationHudTheme.DANGER);
        centered(context, font, "长按 空格 跳过倒地并部署", width / 2, y + 61, OperationHudTheme.TEXT, true);
    }
    private static void renderDeploymentHud(DrawContext context, int width, int height) {
        TextRenderer font = CLIENT.textRenderer;
        String phase = DeploymentManager.descending() ? "正在部署" : "高空战术观察";
        centered(context, font, phase, width / 2, 48, OperationHudTheme.TEXT, true);
        if (DeploymentManager.selecting()) {
            centered(context, font, "选择下方出生点以投入战场", width / 2, 63, OperationHudTheme.TEXT_DIM, false);
            for (cn.epicmc.client.deployment.DeploymentManager.DeploymentTarget target : DeploymentManager.targets()) {
                int distance = (int) CLIENT.player.getPos().distanceTo(target.position());
                // Target cards in DeploymentScreen provide interaction; this is the persistent 3D-view label.
                centered(context, font, target.name() + "  " + distance + "m", width / 2, height - 103, OperationHudTheme.ATTACK_BRIGHT, true);
                break;
            }
        }
    }
    private static void renderBattleHeader(DrawContext context, int screenWidth) {
        TextRenderer font = CLIENT.textRenderer;
        int width = 388;
        int height = 47;
        int x = (screenWidth - width) / 2;
        int y = 10;
        panel(context, x, y, width, height, OperationHudTheme.PANEL, OperationHudTheme.NEUTRAL);
        context.fill(x, y, x + 5, y + height, OperationHudTheme.ATTACK);
        context.fill(x + width - 5, y, x + width, y + height, OperationHudTheme.DEFENSE);
        drawIcon(context, x + 18, y + 14, Icon.SHIELD, OperationHudTheme.ATTACK_BRIGHT);
        drawIcon(context, x + width - 28, y + 14, Icon.SHIELD, OperationHudTheme.DEFENSE_BRIGHT);

        int tickets = Math.round(TICKET_ANIMATION.update(DATA.getTickets(), 0.15f));
        String attack = "日军  " + tickets;
        String defense = "国军";
        context.drawText(font, text(attack), x + 33, y + 10, OperationHudTheme.ATTACK_BRIGHT, true);
        context.drawText(font, text(defense), x + width - 33 - font.getWidth(text(defense)), y + 10, OperationHudTheme.DEFENSE_BRIGHT, true);
        //String phase = DATA.getGameMode().isBlank() ? "上海 1937 · 行动模式" : DATA.getGameMode();
        String phase;
        GameModeType modeType = DATA.getGameModeType();
        if (modeType == null) {
            phase = "上海 1937 · 行动模式";  // 默认显示
        } else {
            switch (modeType) {
                case ACTION -> phase = "上海 1937 · 行动模式";
                case CONTEST -> phase = "上海 1937 · 夺点模式";
                default -> phase = modeType.getString();
            }
        }
        centered(context, font, phase, screenWidth / 2, y + 9, OperationHudTheme.TEXT, true);
        String wave = "第 " + DATA.getWave() + " 波  /  " + Math.max(1, DATA.getMaxWaves());
        centered(context, font, wave, screenWidth / 2, y + 29, OperationHudTheme.TEXT_DIM, false);
        context.fill(x + 12, y + 40, x + width - 12, y + 41, 0x5CFFFFFF);
    }

    private static void renderCaptureStrip(DrawContext context, int screenWidth) {
        List<CapturePoint> points = DATA.getCapturePoints().stream()
                .sorted(Comparator.comparing(CapturePoint::getId))
                .toList();
        if (points.isEmpty()) return;
        TextRenderer font = CLIENT.textRenderer;
        int cardWidth = 86;
        int gap = 7;
        int totalWidth = points.size() * cardWidth + (points.size() - 1) * gap;
        int startX = (screenWidth - totalWidth) / 2;
        int y = 65;
        for (int index = 0; index < points.size(); index++) {
            CapturePoint point = points.get(index);
            int x = startX + index * (cardWidth + gap);
            int color = point.getState().getColor() | 0xFF000000;
            boolean contested = point.getState() == CaptureState.FRIENDLY_CAPTURING || point.getState() == CaptureState.ENEMY_CAPTURING;
            if (contested && ((int) (pulse * 8) & 1) == 0) color = OperationHudTheme.CAPTURING;
            panel(context, x, y, cardWidth, 38, OperationHudTheme.PANEL_SOFT, color);
            drawIcon(context, x + 7, y + 7, Icon.FLAG, color);
            context.drawText(font, text(point.getId()), x + 23, y + 6, color, true);
            String shortName = abbreviate(point.getName(), 9);
            context.drawText(font, text(shortName), x + 7, y + 18, OperationHudTheme.TEXT_DIM, false);
            int barX = x + 7;
            int barY = y + 30;
            int barWidth = cardWidth - 14;
            float animated = CAPTURE_ANIMATIONS.computeIfAbsent(point.getId(), unused -> new OperationHudAnimation())
                    .update(point.getProgress(), 0.12f);
            context.fill(barX, barY, barX + barWidth, barY + 33, OperationHudTheme.PANEL_INSET);
            context.fill(barX, barY, barX + Math.round(barWidth * animated), barY + 33, withAlpha(color, 220));
            if (point.getCapturingPlayers() > 0) {
                String count = "+" + point.getCapturingPlayers();
                context.drawText(font, text(count), x + cardWidth - 7 - font.getWidth(text(count)), y + 6, OperationHudTheme.TEXT, true);
            }
        }
    }

    private static void renderPlayerPanel(DrawContext context, int screenWidth, int screenHeight) {
        TextRenderer font = CLIENT.textRenderer;
        // 根据屏幕高度计算缩放因子
        float scale = calculateScale(screenHeight);
        int baseWidth = 184;
        int baseHeight = 48;
        int baseMargin = 14;

        int width = Math.round(baseWidth * scale);
        int height = Math.round(baseHeight * scale);
        int margin = Math.round(baseMargin * scale);
        int x = margin;  // 左侧
        int y = screenHeight - height - margin;  // 底部（左下角）

        panel(context, x, y, width, height, OperationHudTheme.PANEL, OperationHudTheme.ATTACK);

        // 缩放内部元素
        int iconSize = Math.round(12 * scale);
        int padding = Math.round(9 * scale);
        drawIcon(context, x + padding, y + Math.round(8 * scale), Icon.SOLDIER, OperationHudTheme.ATTACK_BRIGHT);

        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0f);
        int scaledX = Math.round((x + padding + iconSize + Math.round(8 * scale)) / scale);
        int scaledY = Math.round((y + Math.round(7 * scale)) / scale);
        context.drawText(font, text("突击兵"), scaledX, scaledY, OperationHudTheme.TEXT, true);
        context.getMatrices().pop();

        float health = HEALTH_ANIMATION.update(DATA.getHealthPercentage(), 0.14f);
        int healthColor = health > 0.55f ? OperationHudTheme.SUCCESS : health > 0.25f ? OperationHudTheme.CAPTURING : OperationHudTheme.DANGER;

        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0f);
        int healthLabelY = Math.round((y + Math.round(21 * scale)) / scale);
        context.drawText(font, text("生命"), Math.round((x + padding) / scale), healthLabelY, OperationHudTheme.TEXT_DIM, false);
        String healthLabel = Math.round(DATA.getHealth()) + " / " + Math.round(DATA.getMaxHealth());
        context.drawText(font, text(healthLabel), Math.round((x + width - padding - font.getWidth(text(healthLabel)) * scale) / scale), healthLabelY, OperationHudTheme.TEXT, true);
        context.getMatrices().pop();

        int barWidth = width - padding * 2;
        int barHeight = Math.max(3, Math.round(4 * scale));
        progressBar(context, x + padding, y + Math.round(35 * scale), barWidth, barHeight, health, healthColor);

    }

    private static void renderKillFeed(DrawContext context, int screenWidth, int screenHeight) {
        TextRenderer font = CLIENT.textRenderer;
        float scale = calculateScale(screenHeight);
        int y = Math.round(116 * scale);  // 从顶部开始，在占点条下方
        for (KillFeedEntry entry : DATA.getKillFeed()) {
            float alpha = entry.getAlpha();
            if (alpha <= 0.02f) continue;
            float age = (System.currentTimeMillis() - entry.getTimestamp()) / 220.0f;
            int slide = Math.round((1.0f - OperationHudAnimation.easeOutCubic(age)) * 35);
            String killer = entry.getKiller();
            String victim = entry.getVictim();
            String weapon = entry.isHeadshot() ? "★" : "✦";
            int width = Math.max(174, font.getWidth(text(killer)) + font.getWidth(text(victim)) + 52);
            int x = screenWidth - width - 14 + slide;
            int alphaByte = Math.round(alpha * 185) << 24;
            context.fill(x, y, x + width, y + 19, alphaByte | 0x10161B);
            int killerColor = entry.isFriendly() ? OperationHudTheme.ATTACK_BRIGHT : OperationHudTheme.DEFENSE_BRIGHT;
            context.drawText(font, text(killer), x + 7, y + 6, withAlpha(killerColor, Math.round(alpha * 255)), true);
            centered(context, font, weapon, x + width / 2, y + 6, withAlpha(OperationHudTheme.TEXT, Math.round(alpha * 255)), false);
            int victimColor = entry.isFriendly() ? OperationHudTheme.DEFENSE_BRIGHT : OperationHudTheme.ATTACK_BRIGHT;
            context.drawText(font, text(victim), x + width - 7 - font.getWidth(text(victim)), y + 6, withAlpha(victimColor, Math.round(alpha * 255)), true);
            context.fill(x, y, x + 2, y + 19, withAlpha(killerColor, Math.round(alpha * 255)));
            y += 23;
        }
    }

    private static void renderObjectiveOverlay(DrawContext context, int screenWidth, int screenHeight) {
        if (!CLIENT.player.isSpectator()) return;
        TextRenderer font = CLIENT.textRenderer;
        int width = 276;
        int x = (screenWidth - width) / 2;
        int y = screenHeight / 2 - 42;
        panel(context, x, y, width, 84, 0xDC0B0E12, OperationHudTheme.DANGER);
        drawIcon(context, x + width / 2 - 9, y + 12, Icon.CROSS, OperationHudTheme.DANGER);
        centered(context, font, "等待增援", screenWidth / 2, y + 33, OperationHudTheme.TEXT, true);
        centered(context, font, "复活后将自动回到前线", screenWidth / 2, y + 51, OperationHudTheme.TEXT_DIM, false);
        centered(context, font, "观察队友，准备下一次进攻", screenWidth / 2, y + 66, OperationHudTheme.TEXT_DIM, false);
    }

    private static void renderStatusMessage(DrawContext context, int screenWidth, int screenHeight) {
        StatusMessage message = DATA.getStatusMessage();
        if (message == null) return;
        TextRenderer font = CLIENT.textRenderer;
        String text = message.getMessage();
        int width = Math.max(230, font.getWidth(text(text)) + 50);
        int x = (screenWidth - width) / 2;
        int y = screenHeight / 2 - 104;
        panel(context, x, y, width, 30, 0xE00E1419, message.getType().getColor());
        drawIcon(context, x + 12, y + 8, Icon.FLAG, message.getType().getColor());
        centered(context, font, text, screenWidth / 2 + 8, y + 10, OperationHudTheme.TEXT, true);
    }

    private static void panel(DrawContext context, int x, int y, int width, int height, int background, int accent) {
        context.drawGuiTexture(PANEL_TEXTURE, x, y, width, height);
    }

    private static void progressBar(DrawContext context, int x, int y, int width, int height, float value, int color) {
        context.drawGuiTexture(BAR_BG_TEXTURE, x, y, width, height);
        int fillWidth = Math.round(width * Math.clamp(value, 0f, 1f));
        if (fillWidth > 0) {
            context.drawGuiTexture(BAR_FG_TEXTURE, x, y, fillWidth, height, color);
        }
    }
    private static void centered(DrawContext context, TextRenderer font, String text, int x, int y, int color, boolean shadow) {
        context.drawText(font, text(text), x - font.getWidth(text(text)) / 2, y, color, shadow);
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static String abbreviate(String value, int maximum) {
        if (value == null || value.isBlank()) return "未装备";
        return value.length() <= maximum ? value : value.substring(0, Math.max(1, maximum - 1)) + "…";
    }

    private static void drawIcon(DrawContext context, int x, int y, Icon icon, int color) {
        Identifier texture = switch (icon) {
            case FLAG -> ICON_FLAG;
            case SHIELD -> ICON_SHIELD;
            case SOLDIER -> ICON_SOLDIER;
            case ARMOR -> ICON_ARMOR;
            case CROSS -> ICON_CROSS;
        };
        // Draw 16x16 icon
        context.drawGuiTexture(texture, x, y, 16, 16, color);
    }

    private enum Icon { FLAG, SHIELD, SOLDIER, ARMOR, CROSS }
}
