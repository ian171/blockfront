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
import net.minecraft.text.Text;

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
    private static float pulse;

    private BattlefieldHudRenderer() { }

    public static void render(DrawContext context, float tickDelta) {
        if (CLIENT.player == null || CLIENT.options.hudHidden) return;
        syncLocalPlayerData();
        DATA.updateKillFeed();
        pulse = (pulse + tickDelta * 0.055f) % 1.0f;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        renderBattleHeader(context, width);
        renderCaptureStrip(context, width);
        renderKillFeed(context, width);
        renderPlayerPanel(context, height);
        renderWeaponPanel(context, width, height);
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

    private static void renderDownedHud(DrawContext context, int width, int height) {
        TextRenderer font = CLIENT.textRenderer;
        int panelWidth = 360;
        int x = (width - panelWidth) / 2;
        int y = height - 105;
        panel(context, x, y, panelWidth, 72, 0xDE170C0C, OperationHudTheme.DANGER);
        drawIcon(context, x + 14, y + 16, Icon.CROSS, OperationHudTheme.DANGER);
        context.drawText(font, Text.literal("你已倒地"), x + 38, y + 12, OperationHudTheme.TEXT, true);
        String killer = "击倒者  " + DownedManager.killerName();
        context.drawText(font, Text.literal(killer), x + 38, y + 28, OperationHudTheme.DANGER, true);
        String weapon = "使用 " + DownedManager.weaponName();
        context.drawText(font, Text.literal(weapon), x + 38, y + 43, OperationHudTheme.TEXT_DIM, false);
        String timer = DownedManager.remainingSeconds() + " 秒";
        context.drawText(font, Text.literal(timer), x + panelWidth - 16 - font.getWidth(timer), y + 14, OperationHudTheme.TEXT, true);
        context.drawText(font, Text.literal("等待医疗兵救援"), x + panelWidth - 16 - font.getWidth("等待医疗兵救援"), y + 30, OperationHudTheme.TEXT_DIM, false);
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
        String attack = "国军  " + tickets;
        String defense = "日军";
        context.drawText(font, Text.literal(attack), x + 33, y + 10, OperationHudTheme.ATTACK_BRIGHT, true);
        context.drawText(font, Text.literal(defense), x + width - 33 - font.getWidth(defense), y + 10, OperationHudTheme.DEFENSE_BRIGHT, true);
        //String phase = DATA.getGameMode().isBlank() ? "上海 1937 · 行动模式" : DATA.getGameMode();
        String phase;
        switch (DATA.getGameModeType()){
            case ACTION -> {
                phase = "上海 1937 · 行动模式";
            }
            case CONTEST -> {
                phase = "上海 1937 · 夺点模式";
            }
            default -> {
                phase = DATA.getGameModeType().getString();
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
            context.drawText(font, Text.literal(point.getId()), x + 23, y + 6, color, true);
            String shortName = abbreviate(point.getName(), 9);
            context.drawText(font, Text.literal(shortName), x + 7, y + 18, OperationHudTheme.TEXT_DIM, false);
            int barX = x + 7;
            int barY = y + 30;
            int barWidth = cardWidth - 14;
            float animated = CAPTURE_ANIMATIONS.computeIfAbsent(point.getId(), unused -> new OperationHudAnimation())
                    .update(point.getProgress(), 0.12f);
            context.fill(barX, barY, barX + barWidth, barY + 33, OperationHudTheme.PANEL_INSET);
            context.fill(barX, barY, barX + Math.round(barWidth * animated), barY + 33, withAlpha(color, 220));
            if (point.getCapturingPlayers() > 0) {
                String count = "+" + point.getCapturingPlayers();
                context.drawText(font, Text.literal(count), x + cardWidth - 7 - font.getWidth(count), y + 6, OperationHudTheme.TEXT, true);
            }
        }
    }

    private static void renderPlayerPanel(DrawContext context, int screenHeight) {
        TextRenderer font = CLIENT.textRenderer;
        int x = 14;
        int y = screenHeight - 92;
        int width = 210;
        panel(context, x, y, width, 76, OperationHudTheme.PANEL, OperationHudTheme.ATTACK);
        drawIcon(context, x + 11, y + 12, Icon.SOLDIER, OperationHudTheme.ATTACK_BRIGHT);
        context.drawText(font, Text.literal("国军 · 突击兵"), x + 34, y + 10, OperationHudTheme.TEXT, true);
        float health = HEALTH_ANIMATION.update(DATA.getHealthPercentage(), 0.14f);
        int healthColor = health > 0.55f ? OperationHudTheme.SUCCESS : health > 0.25f ? OperationHudTheme.CAPTURING : OperationHudTheme.DANGER;
        context.drawText(font, Text.literal("生命"), x + 11, y + 34, OperationHudTheme.TEXT_DIM, false);
        String healthLabel = Math.round(DATA.getHealth()) + " / " + Math.round(DATA.getMaxHealth());
        context.drawText(font, Text.literal(healthLabel), x + width - 12 - font.getWidth(healthLabel), y + 34, OperationHudTheme.TEXT, true);
        progressBar(context, x + 11, y + 47, width - 22, 9, health, healthColor);
        if (DATA.getArmor() > 0) {
            drawIcon(context, x + 11, y + 61, Icon.ARMOR, OperationHudTheme.TEXT_DIM);
            context.drawText(font, Text.literal("护甲 " + Math.round(DATA.getArmor())), x + 26, y + 61, OperationHudTheme.TEXT_DIM, false);
        }
    }

    private static void renderWeaponPanel(DrawContext context, int screenWidth, int screenHeight) {
        TextRenderer font = CLIENT.textRenderer;
        int width = 218;
        int x = screenWidth - width - 14;
        int y = screenHeight - 92;
        panel(context, x, y, width, 76, OperationHudTheme.PANEL, OperationHudTheme.ATTACK);
        drawIcon(context, x + 12, y + 12, Icon.RIFLE, OperationHudTheme.TEXT);
        context.drawText(font, Text.literal(abbreviate(DATA.getWeaponName(), 20)), x + 37, y + 11, OperationHudTheme.TEXT, true);
        context.drawText(font, Text.literal("主武器"), x + 37, y + 24, OperationHudTheme.TEXT_DIM, false);
        int ammo = DATA.getAmmo();
        int reserve = DATA.getAmmoReserve();
        if (ammo > 0 || reserve > 0) {
            int ammoColor = ammo < 8 ? OperationHudTheme.DANGER : OperationHudTheme.TEXT;
            context.getMatrices().push();
            context.getMatrices().translate(x + 15, y + 43, 0);
            context.getMatrices().scale(1.55f, 1.55f, 1.0f);
            context.drawText(font, Text.literal(String.valueOf(ammo)), 0, 0, ammoColor, true);
            context.getMatrices().pop();
            context.drawText(font, Text.literal("/ " + reserve), x + 72, y + 51, OperationHudTheme.TEXT_DIM, true);
        } else {
            context.drawText(font, Text.literal("标准配备"), x + 12, y + 50, OperationHudTheme.TEXT_DIM, false);
        }
        context.fill(x + 12, y + 66, x + width - 12, y + 67, 0x50FFFFFF);
    }

    private static void renderKillFeed(DrawContext context, int screenWidth) {
        TextRenderer font = CLIENT.textRenderer;
        int y = 116;
        for (KillFeedEntry entry : DATA.getKillFeed()) {
            float alpha = entry.getAlpha();
            if (alpha <= 0.02f) continue;
            float age = (System.currentTimeMillis() - entry.getTimestamp()) / 220.0f;
            int slide = Math.round((1.0f - OperationHudAnimation.easeOutCubic(age)) * 35);
            String killer = entry.getKiller();
            String victim = entry.getVictim();
            String weapon = entry.isHeadshot() ? "★" : "✦";
            int width = Math.max(174, font.getWidth(killer) + font.getWidth(victim) + 52);
            int x = screenWidth - width - 14 + slide;
            int alphaByte = Math.round(alpha * 185) << 24;
            context.fill(x, y, x + width, y + 19, alphaByte | 0x10161B);
            int killerColor = entry.isFriendly() ? OperationHudTheme.ATTACK_BRIGHT : OperationHudTheme.DEFENSE_BRIGHT;
            context.drawText(font, Text.literal(killer), x + 7, y + 6, withAlpha(killerColor, Math.round(alpha * 255)), true);
            centered(context, font, weapon, x + width / 2, y + 6, withAlpha(OperationHudTheme.TEXT, Math.round(alpha * 255)), false);
            int victimColor = entry.isFriendly() ? OperationHudTheme.DEFENSE_BRIGHT : OperationHudTheme.ATTACK_BRIGHT;
            context.drawText(font, Text.literal(victim), x + width - 7 - font.getWidth(victim), y + 6, withAlpha(victimColor, Math.round(alpha * 255)), true);
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
        int width = Math.max(230, font.getWidth(text) + 50);
        int x = (screenWidth - width) / 2;
        int y = screenHeight / 2 - 104;
        panel(context, x, y, width, 30, 0xE00E1419, message.getType().getColor());
        drawIcon(context, x + 12, y + 8, Icon.FLAG, message.getType().getColor());
        centered(context, font, text, screenWidth / 2 + 8, y + 10, OperationHudTheme.TEXT, true);
    }

    private static void panel(DrawContext context, int x, int y, int width, int height, int background, int accent) {
        context.fill(x, y, x + width, y + height, background);
        context.fill(x, y, x + width, y + 1, withAlpha(accent, 210));
        context.fill(x, y + height - 1, x + width, y + height, 0x4CFFFFFF);
        context.fill(x, y, x + 1, y + height, 0x4CFFFFFF);
        context.fill(x + width - 1, y, x + width, y + height, 0x4CFFFFFF);
    }

    private static void progressBar(DrawContext context, int x, int y, int width, int height, float value, int color) {
        context.fill(x, y, x + width, y + height, OperationHudTheme.PANEL_INSET);
        context.fill(x, y, x + Math.round(width * Math.clamp(value, 0f, 1f)), y + height, color);
        context.fill(x, y + height - 1, x + width, y + height, 0x52FFFFFF);
    }

    private static void centered(DrawContext context, TextRenderer font, String text, int x, int y, int color, boolean shadow) {
        context.drawText(font, Text.literal(text), x - font.getWidth(text) / 2, y, color, shadow);
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static String abbreviate(String value, int maximum) {
        if (value == null || value.isBlank()) return "未装备";
        return value.length() <= maximum ? value : value.substring(0, Math.max(1, maximum - 1)) + "…";
    }

    private static void drawIcon(DrawContext context, int x, int y, Icon icon, int color) {
        switch (icon) {
            case FLAG -> { context.fill(x + 2, y, x + 3, y + 13, color); context.fill(x + 3, y + 1, x + 11, y + 5, color); context.fill(x + 7, y + 5, x + 11, y + 7, color); }
            case SHIELD -> { context.fill(x + 2, y, x + 12, y + 3, color); context.fill(x + 3, y + 3, x + 11, y + 10, color); context.fill(x + 5, y + 10, x + 9, y + 13, color); }
            case SOLDIER -> { context.fill(x + 5, y, x + 10, y + 5, color); context.fill(x + 3, y + 5, x + 12, y + 10, color); context.fill(x + 1, y + 10, x + 14, y + 13, color); }
            case ARMOR -> { context.fill(x + 3, y, x + 11, y + 3, color); context.fill(x + 2, y + 3, x + 12, y + 11, color); context.fill(x + 4, y + 11, x + 10, y + 14, color); }
            case RIFLE -> { context.fill(x, y + 6, x + 15, y + 9, color); context.fill(x + 3, y + 3, x + 8, y + 6, color); context.fill(x + 11, y + 9, x + 13, y + 14, color); }
            case CROSS -> { context.fill(x + 6, y, x + 9, y + 15, color); context.fill(x, y + 6, x + 15, y + 9, color); }
        }
    }

    private enum Icon { FLAG, SHIELD, SOLDIER, ARMOR, RIFLE, CROSS }
}
