package cn.epicmc.client.hud;

import cn.epicmc.client.hud.HudDataManager.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * 战地风格 HUD 渲染器
 * 提供现代化、动态的战场界面
 */
public class BattlefieldHudRenderer {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final HudDataManager dataManager = HudDataManager.getInstance();

    // 颜色定义
    private static final int COLOR_FRIENDLY = 0x4A90E2;
    private static final int COLOR_ENEMY = 0xE74C3C;
    private static final int COLOR_NEUTRAL = 0xBDC3C7;
    private static final int COLOR_WHITE = 0xFFFFFF;
    private static final int COLOR_WARNING = 0xF39C12;
    private static final int COLOR_SUCCESS = 0x2ECC71;

    // 缩放比例
    private static final float HUD_SCALE = 0.75f;

    // 动画
    private static float pulseAnimation = 0f;
    private static float healthBarAnimation = 1f;

    /**
     * 渲染主 HUD
     */
    public static void render(DrawContext context, float tickDelta) {
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        updateAnimations(tickDelta);
        dataManager.updateKillFeed();

        // 同步玩家血量和弹药
        syncPlayerData();

        context.getMatrices().push();
        context.getMatrices().scale(HUD_SCALE, HUD_SCALE, 1.0f);

        int scaledWidth = (int) (screenWidth / HUD_SCALE);
        int scaledHeight = (int) (screenHeight / HUD_SCALE);

        // 移除 renderTopBar 和 renderTeamInfo
        renderCapturePoints(context, scaledWidth, scaledHeight);
        renderKillFeed(context, scaledWidth, scaledHeight);
        renderHealthBar(context, scaledWidth, scaledHeight);
        renderAmmoCounter(context, scaledWidth, scaledHeight);
        renderStatusMessage(context, scaledWidth, scaledHeight);

        context.getMatrices().pop();
    }

    /**
     * 同步玩家数据（从游戏内获取）
     */
    private static void syncPlayerData() {
        if (client.player == null) return;

        // 同步血量
        float health = client.player.getHealth();
        float maxHealth = client.player.getMaxHealth();
        dataManager.setHealth(health, maxHealth);

        // 同步护甲
        float armor = client.player.getArmor();
        dataManager.setArmor(armor);

        // 获取主手物品
        ItemStack mainHandStack = client.player.getMainHandStack();

        // 同步当前手持武器名称
        String weaponName = "";
        if (mainHandStack != null && !mainHandStack.isEmpty()) {
            weaponName = mainHandStack.getName().getString();
        }
        dataManager.setWeaponName(weaponName);

        // 普通武器不显示弹药
        dataManager.setAmmo(0, 0);
    }

    private static void updateAnimations(float tickDelta) {
        pulseAnimation += tickDelta * 0.1f;
        if (pulseAnimation > 2 * Math.PI) {
            pulseAnimation -= 2 * Math.PI;
        }
        float targetHealth = dataManager.getHealthPercentage();
        healthBarAnimation = MathHelper.lerp(0.1f, healthBarAnimation, targetHealth);
    }

    private static void renderTopBar(DrawContext context, int screenWidth, int screenHeight) {
        TextRenderer textRenderer = client.textRenderer;
        int barWidth = 250;
        int barHeight = 25;
        int x = (screenWidth - barWidth) / 2;
        int y = 5;

        drawTransparentRect(context, x, y, barWidth, barHeight, 0x80000000);
        drawBorder(context, x, y, barWidth, barHeight, COLOR_FRIENDLY, 1);

        String gameMode = dataManager.getGameMode();
        if (!gameMode.isEmpty()) {
            drawCenteredText(context, textRenderer, gameMode, screenWidth / 2, y + 4, COLOR_WHITE, true);
        }

        String time = dataManager.getFormattedTime();
        int timeColor = dataManager.getRemainingTime() < 60 ? COLOR_WARNING : COLOR_WHITE;
        drawCenteredText(context, textRenderer, time, screenWidth / 2, y + 14, timeColor, true);

        String scoreText = "分数: " + dataManager.getScore();
        context.drawText(textRenderer, Text.literal(scoreText), x + barWidth - 60, y + 9, COLOR_SUCCESS, true);
    }

    private static void renderTeamInfo(DrawContext context, int screenWidth, int screenHeight) {
        TextRenderer textRenderer = client.textRenderer;
        int x = 10;
        int y = 35;
        int width = 120;  // 缩小宽度
        int height = 40;  // 缩小高度

        drawTransparentRect(context, x, y, width, height, 0x90000000);

        TeamData friendly = dataManager.getFriendlyTeam();
        renderTeam(context, textRenderer, x + 5, y + 5, friendly);

        context.fill(x + 5, y + 20, x + width - 5, y + 21, 0x60FFFFFF);

        TeamData enemy = dataManager.getEnemyTeam();
        renderTeam(context, textRenderer, x + 5, y + 23, enemy);

        drawBorder(context, x, y, width, height, COLOR_FRIENDLY, 1);
    }

    private static void renderTeam(DrawContext context, TextRenderer textRenderer, int x, int y, TeamData team) {
        int color = team.getColor();
        context.fill(x, y, x + 2, y + 14, color | 0xFF000000);
        context.drawText(textRenderer, Text.literal(team.getName()), x + 6, y, color, true);
        String playerText = team.getPlayerCount() + "/" + team.getMaxPlayers();
        context.drawText(textRenderer, Text.literal(playerText), x + 6, y + 8, 0xFFCCCCCC, true);
        // 移除票数显示
    }

    private static void renderCapturePoints(DrawContext context, int screenWidth, int screenHeight) {
        List<CapturePoint> points = dataManager.getCapturePoints();
        if (points.isEmpty()) return;

        TextRenderer textRenderer = client.textRenderer;
        int totalWidth = points.size() * 70 + (points.size() - 1) * 8;
        int startX = (screenWidth - totalWidth) / 2;
        int y = 90;

        for (int i = 0; i < points.size(); i++) {
            CapturePoint point = points.get(i);
            int x = startX + i * 78;
            renderCapturePoint(context, textRenderer, x, y, point);
        }
    }

    private static void renderCapturePoint(DrawContext context, TextRenderer textRenderer, int x, int y, CapturePoint point) {
        int width = 70;
        int height = 40;

        drawTransparentRect(context, x, y, width, height, 0x80000000);

        String name = point.getName();
        int nameColor = point.getState().getColor();
        drawCenteredText(context, textRenderer, name, x + width / 2, y + 4, nameColor, true);

        int barX = x + 8;
        int barY = y + 16;
        int barWidth = width - 16;
        int barHeight = 12;

        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0x802C3E50);

        int fillWidth = (int) (barWidth * point.getProgress());
        int fillColor = point.getState().getColor() | 0xCC000000;
        if (fillWidth > 0) {
            context.fill(barX, barY, barX + fillWidth, barY + barHeight, fillColor);
        }

        drawBorder(context, barX, barY, barWidth, barHeight, 0x60FFFFFF, 1);

        if (point.getCapturingPlayers() > 0) {
            String capText = "+" + point.getCapturingPlayers();
            drawCenteredText(context, textRenderer, capText, x + width / 2, y + 30, 0xFFFFFFFF, true);
        }

        drawBorder(context, x, y, width, height, nameColor, 1);
    }

    private static void renderKillFeed(DrawContext context, int screenWidth, int screenHeight) {
        List<KillFeedEntry> entries = dataManager.getKillFeed();
        if (entries.isEmpty()) return;

        TextRenderer textRenderer = client.textRenderer;
        int x = screenWidth - 210;  // 缩小宽度
        int y = 35;

        for (KillFeedEntry entry : entries) {
            float alpha = entry.getAlpha();
            if (alpha <= 0) continue;

            long age = System.currentTimeMillis() - entry.getTimestamp();
            float slideProgress = Math.min(1.0f, age / 200.0f);
            int slideOffset = (int) ((1.0f - slideProgress) * 50);

            renderKillFeedEntry(context, textRenderer, x + slideOffset, y, entry, alpha);
            y += 18;  // 减小间距
        }
    }

    private static void renderKillFeedEntry(DrawContext context, TextRenderer textRenderer, int x, int y, KillFeedEntry entry, float alpha) {
        int width = 200;  // 缩小宽度
        int height = 15;  // 缩小高度

        int bgAlpha = (int) (alpha * 144);
        drawTransparentRect(context, x, y, width, height, (bgAlpha << 24) | 0x000000);

        int textAlpha = (int) (alpha * 255) << 24;

        int killerColor = entry.isFriendly() ? COLOR_FRIENDLY : COLOR_ENEMY;
        context.drawText(textRenderer, Text.literal(entry.getKiller()), x + 3, y + 4, (textAlpha | killerColor), false);

        String weapon = entry.getWeapon();
        if (entry.isHeadshot()) {
            weapon += " HS";
        }
        context.drawText(textRenderer, Text.literal(weapon), x + 75, y + 4, (textAlpha | 0xFFFFFF), false);

        int victimColor = entry.isFriendly() ? COLOR_ENEMY : COLOR_FRIENDLY;
        int victimX = x + width - textRenderer.getWidth(entry.getVictim()) - 3;
        context.drawText(textRenderer, Text.literal(entry.getVictim()), victimX, y + 4, (textAlpha | victimColor), false);

        int borderColor = entry.isFriendly() ? COLOR_FRIENDLY : COLOR_ENEMY;
        drawBorder(context, x, y, width, height, (textAlpha | borderColor), 1);
    }

    private static void renderHealthBar(DrawContext context, int screenWidth, int screenHeight) {
        TextRenderer textRenderer = client.textRenderer;
        int x = 10;
        int y = screenHeight - 65;
        int width = 160;
        int height = 50;

        drawTransparentRect(context, x, y, width, height, 0x90000000);
        context.drawText(textRenderer, Text.literal("生命值"), x + 8, y + 4, COLOR_WHITE, true);

        int barX = x + 8;
        int barY = y + 15;
        int barWidth = width - 16;
        int barHeight = 16;

        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0x802C3E50);

        int fillWidth = (int) (barWidth * healthBarAnimation);
        int healthColor = getHealthColor(healthBarAnimation);
        if (fillWidth > 0) {
            context.fill(barX, barY, barX + fillWidth, barY + barHeight, healthColor | 0xEE000000);
        }

        drawBorder(context, barX, barY, barWidth, barHeight, 0x60FFFFFF, 1);

        String healthText = String.format("%.0f / %.0f", dataManager.getHealth(), dataManager.getMaxHealth());
        drawCenteredText(context, textRenderer, healthText, barX + barWidth / 2, barY + 5, COLOR_WHITE, true);

        float armor = dataManager.getArmor();
        if (armor > 0) {
            String armorText = "护甲: " + String.format("%.0f", armor);
            context.drawText(textRenderer, Text.literal(armorText), x + 8, y + 35, 0xFFECF0F1, true);
        }

        int borderColor = COLOR_FRIENDLY;
        if (healthBarAnimation < 0.3f) {
            float pulse = (float) Math.sin(pulseAnimation * 4) * 0.5f + 0.5f;
            borderColor = lerpColor(COLOR_WARNING, COLOR_ENEMY, pulse);
        }
        drawBorder(context, x, y, width, height, borderColor, 1);
    }

    private static void renderAmmoCounter(DrawContext context, int screenWidth, int screenHeight) {
        TextRenderer textRenderer = client.textRenderer;
        int x = screenWidth - 170;
        int y = screenHeight - 65;
        int width = 160;
        int height = 50;

        drawTransparentRect(context, x, y, width, height, 0x90000000);

        String weaponName = dataManager.getWeaponName();
        if (!weaponName.isEmpty()) {
            context.drawText(textRenderer, Text.literal(weaponName), x + 8, y + 4, COLOR_WHITE, true);
        }

        int currentAmmo = dataManager.getAmmo();
        int reserveAmmo = dataManager.getAmmoReserve();

        // 只有当有弹药数据时才显示
        if (currentAmmo > 0 || reserveAmmo > 0) {
            String ammoText = String.valueOf(currentAmmo);
            int ammoColor = currentAmmo == 0 ? COLOR_ENEMY : COLOR_WHITE;

            context.getMatrices().push();
            context.getMatrices().translate(x + 25, y + 22, 0);
            context.getMatrices().scale(1.8f, 1.8f, 1.0f);
            context.drawText(textRenderer, Text.literal(ammoText), 0, 0, ammoColor, true);
            context.getMatrices().pop();

            String reserveText = "/ " + reserveAmmo;
            context.drawText(textRenderer, Text.literal(reserveText), x + 80, y + 30, COLOR_NEUTRAL, true);

            if (currentAmmo < 10 && currentAmmo > 0) {
                float pulse = (float) Math.sin(pulseAnimation * 4) * 0.5f + 0.5f;
                int warningAlpha = (int) (pulse * 80);
                context.fill(x, y, x + width, y + height, (warningAlpha << 24) | 0xFF0000);
            }
        }

        drawBorder(context, x, y, width, height, COLOR_FRIENDLY, 1);
    }

    private static void renderStatusMessage(DrawContext context, int screenWidth, int screenHeight) {
        StatusMessage message = dataManager.getStatusMessage();
        if (message == null) return;

        TextRenderer textRenderer = client.textRenderer;
        String text = message.getMessage();
        int textWidth = textRenderer.getWidth(text);

        int x = (screenWidth - textWidth) / 2 - 10;
        int y = screenHeight / 2 - 100;
        int width = textWidth + 20;
        int height = 25;

        int bgColor = message.getType().getColor();
        drawTransparentRect(context, x, y, width, height, 0xCC000000);
        drawCenteredText(context, textRenderer, text, screenWidth / 2, y + 8, bgColor, true);
        drawBorder(context, x, y, width, height, bgColor, 2);
    }

    private static void drawTransparentRect(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + height, color);
    }

    private static void drawBorder(DrawContext context, int x, int y, int width, int height, int color, int thickness) {
        context.fill(x, y, x + width, y + thickness, color | 0xFF000000);
        context.fill(x, y + height - thickness, x + width, y + height, color | 0xFF000000);
        context.fill(x, y, x + thickness, y + height, color | 0xFF000000);
        context.fill(x + width - thickness, y, x + width, y + height, color | 0xFF000000);
    }

    private static void drawCenteredText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow) {
        int textWidth = textRenderer.getWidth(text);
        context.drawText(textRenderer, Text.literal(text), x - textWidth / 2, y, color, shadow);
    }

    private static int getHealthColor(float percentage) {
        if (percentage > 0.6f) return COLOR_SUCCESS;
        if (percentage > 0.3f) return COLOR_WARNING;
        return COLOR_ENEMY;
    }

    private static int lerpColor(int color1, int color2, float t) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (r << 16) | (g << 8) | b;
    }
}
