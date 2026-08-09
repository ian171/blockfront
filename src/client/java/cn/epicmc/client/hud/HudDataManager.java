package cn.epicmc.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * HUD 数据管理器
 * 管理从服务器接收的战场数据
 */
public class HudDataManager {
    private static HudDataManager instance;

    // 玩家数据
    private float health = 100.0f;              // 玩家血量
    private float maxHealth = 100.0f;           // 最大血量
    private float armor = 0.0f;                 // 护甲值
    private int ammo = 30;                      // 当前弹药
    private int ammoReserve = 120;              // 备用弹药
    private String weaponName = "";             // 武器名称

    // 队伍数据
    private TeamData friendlyTeam = new TeamData("我方", 0x4A90E2);  // 蓝色
    private TeamData enemyTeam = new TeamData("敌方", 0xE74C3C);     // 红色

    // 占点数据
    private List<CapturePoint> capturePoints = new CopyOnWriteArrayList<>();

    // 击杀反馈
    private List<KillFeedEntry> killFeed = new CopyOnWriteArrayList<>();
    private static final int MAX_KILL_FEED = 5;

    // 状态消息
    private StatusMessage statusMessage = null;
    private long statusMessageTime = 0;

    // 游戏状态
    private String gameMode = "";
    private int remainingTime = 0;  // 剩余时间（秒）
    private int score = 0;          // 个人分数

    private HudDataManager() {}

    public static HudDataManager getInstance() {
        if (instance == null) {
            instance = new HudDataManager();
        }
        return instance;
    }

    // ========== 玩家数据 ==========

    public void setHealth(float health, float maxHealth) {
        this.health = Math.max(0, Math.min(health, maxHealth));
        this.maxHealth = maxHealth;
    }

    public float getHealth() {
        return health;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public float getHealthPercentage() {
        return maxHealth > 0 ? health / maxHealth : 0;
    }

    public void setArmor(float armor) {
        this.armor = Math.max(0, armor);
    }

    public float getArmor() {
        return armor;
    }

    public void setAmmo(int current, int reserve) {
        this.ammo = Math.max(0, current);
        this.ammoReserve = Math.max(0, reserve);
    }

    public int getAmmo() {
        return ammo;
    }

    public int getAmmoReserve() {
        return ammoReserve;
    }

    public void setWeaponName(String weaponName) {
        this.weaponName = weaponName;
    }

    public String getWeaponName() {
        return weaponName;
    }

    // ========== 队伍数据 ==========

    public TeamData getFriendlyTeam() {
        return friendlyTeam;
    }

    public TeamData getEnemyTeam() {
        return enemyTeam;
    }

    public void setTeamData(boolean isFriendly, String name, int playerCount, int maxPlayers, int tickets) {
        TeamData team = isFriendly ? friendlyTeam : enemyTeam;
        team.setName(name);
        team.setPlayerCount(playerCount);
        team.setMaxPlayers(maxPlayers);
        team.setTickets(tickets);
    }

    // ========== 占点数据 ==========

    public List<CapturePoint> getCapturePoints() {
        return new ArrayList<>(capturePoints);
    }

    public void setCapturePoints(List<CapturePoint> points) {
        this.capturePoints.clear();
        this.capturePoints.addAll(points);
    }

    public void updateCapturePoint(String id, String name, float progress, CaptureState state, int capturingPlayers) {
        CapturePoint point = capturePoints.stream()
            .filter(p -> p.getId().equals(id))
            .findFirst()
            .orElse(null);

        if (point == null) {
            point = new CapturePoint(id, name);
            capturePoints.add(point);
        }

        point.setProgress(progress);
        point.setState(state);
        point.setCapturingPlayers(capturingPlayers);
    }

    public void removeCapturePoint(String id) {
        capturePoints.removeIf(p -> p.getId().equals(id));
    }

    // ========== 击杀反馈 ==========

    public List<KillFeedEntry> getKillFeed() {
        return new ArrayList<>(killFeed);
    }

    public void addKillFeedEntry(String killer, String victim, String weapon, boolean isHeadshot, boolean isFriendly) {
        KillFeedEntry entry = new KillFeedEntry(killer, victim, weapon, isHeadshot, isFriendly);
        killFeed.add(0, entry);

        // 限制数量
        while (killFeed.size() > MAX_KILL_FEED) {
            killFeed.remove(killFeed.size() - 1);
        }
    }

    public void updateKillFeed() {
        long currentTime = System.currentTimeMillis();
        killFeed.removeIf(entry -> currentTime - entry.getTimestamp() > 5000); // 5秒后移除
    }

    // ========== 状态消息 ==========

    public void setStatusMessage(String message, StatusMessageType type, int durationMs) {
        this.statusMessage = new StatusMessage(message, type);
        this.statusMessageTime = System.currentTimeMillis() + durationMs;
    }

    public StatusMessage getStatusMessage() {
        if (statusMessage != null && System.currentTimeMillis() > statusMessageTime) {
            statusMessage = null;
        }
        return statusMessage;
    }

    // ========== 游戏状态 ==========

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setRemainingTime(int seconds) {
        this.remainingTime = Math.max(0, seconds);
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public String getFormattedTime() {
        int minutes = remainingTime / 60;
        int seconds = remainingTime % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    // ========== 内部类 ==========

    public static class TeamData {
        private String name;
        private int playerCount;
        private int maxPlayers;
        private int tickets;  // 票数（类似战地）
        private final int color;

        public TeamData(String name, int color) {
            this.name = name;
            this.color = color;
            this.playerCount = 0;
            this.maxPlayers = 32;
            this.tickets = 100;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getPlayerCount() { return playerCount; }
        public void setPlayerCount(int count) { this.playerCount = Math.max(0, count); }

        public int getMaxPlayers() { return maxPlayers; }
        public void setMaxPlayers(int max) { this.maxPlayers = Math.max(1, max); }

        public int getTickets() { return tickets; }
        public void setTickets(int tickets) { this.tickets = Math.max(0, tickets); }

        public int getColor() { return color; }
    }

    public static class CapturePoint {
        private final String id;
        private String name;
        private float progress;  // 0.0 - 1.0
        private CaptureState state;
        private int capturingPlayers;
        private long lastUpdateTime;

        public CapturePoint(String id, String name) {
            this.id = id;
            this.name = name;
            this.progress = 0.5f;
            this.state = CaptureState.NEUTRAL;
            this.capturingPlayers = 0;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public float getProgress() { return progress; }
        public void setProgress(float progress) {
            this.progress = Math.max(0, Math.min(1, progress));
            this.lastUpdateTime = System.currentTimeMillis();
        }

        public CaptureState getState() { return state; }
        public void setState(CaptureState state) { this.state = state; }

        public int getCapturingPlayers() { return capturingPlayers; }
        public void setCapturingPlayers(int count) { this.capturingPlayers = count; }

        public long getLastUpdateTime() { return lastUpdateTime; }
    }

    public enum CaptureState {
        FRIENDLY_OWNED(0x4A90E2),    // 蓝色
        ENEMY_OWNED(0xE74C3C),       // 红色
        NEUTRAL(0xBDC3C7),           // 灰色
        FRIENDLY_CAPTURING(0x3498DB),// 亮蓝色
        ENEMY_CAPTURING(0xFF6B6B);   // 亮红色

        private final int color;

        CaptureState(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }

    public static class KillFeedEntry {
        private final String killer;
        private final String victim;
        private final String weapon;
        private final boolean isHeadshot;
        private final boolean isFriendly;
        private final long timestamp;

        public KillFeedEntry(String killer, String victim, String weapon, boolean isHeadshot, boolean isFriendly) {
            this.killer = killer;
            this.victim = victim;
            this.weapon = weapon;
            this.isHeadshot = isHeadshot;
            this.isFriendly = isFriendly;
            this.timestamp = System.currentTimeMillis();
        }

        public String getKiller() { return killer; }
        public String getVictim() { return victim; }
        public String getWeapon() { return weapon; }
        public boolean isHeadshot() { return isHeadshot; }
        public boolean isFriendly() { return isFriendly; }
        public long getTimestamp() { return timestamp; }

        public float getAlpha() {
            long age = System.currentTimeMillis() - timestamp;
            if (age < 4000) return 1.0f;
            return 1.0f - ((age - 4000) / 1000.0f);
        }
    }

    public static class StatusMessage {
        private final String message;
        private final StatusMessageType type;

        public StatusMessage(String message, StatusMessageType type) {
            this.message = message;
            this.type = type;
        }

        public String getMessage() { return message; }
        public StatusMessageType getType() { return type; }
    }

    public enum StatusMessageType {
        INFO(0xFFFFFF),
        SUCCESS(0x2ECC71),
        WARNING(0xF39C12),
        DANGER(0xE74C3C);

        private final int color;

        StatusMessageType(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }
}
