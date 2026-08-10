package cn.epicmc.client.screen;

import cn.epicmc.client.config.ClientConfig;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.*;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.KeyCodes;
import net.minecraft.client.network.*;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.ServerList;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.List;

@Environment(value = EnvType.CLIENT)
public class ModMultiplayerScreen extends Screen {
    public static final int field_41849 = 308;
    public static final int field_41850 = 100;
    public static final int field_41851 = 74;
    public static final int field_41852 = 64;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final MultiplayerServerListPinger serverListPinger = new MultiplayerServerListPinger();
    private final Screen parent;
    protected ModMultiplayerServerListWidget serverListWidget;
    private ServerList serverList;
    private ButtonWidget buttonEdit;
    private ButtonWidget buttonJoin;
    private ButtonWidget buttonDelete;
    private ServerInfo selectedEntry;
    private LanServerQueryManager.LanServerEntryList lanServers;
    @Nullable
    private LanServerQueryManager.LanServerDetector lanServerDetector;
    private boolean initialized;
    private boolean syncingConfig = false;
    private Text statusMessage = Text.empty();

    public ModMultiplayerScreen(Screen parent) {
        super(Text.translatable("multiplayer.title"));
        this.parent = parent;
    }
    @Override
    protected void init() {
        ClientConfig config = ClientConfig.getInstance();

        if (this.initialized) {
            this.serverListWidget.setDimensionsAndPosition(this.width, this.height - 64 - 32, 0, 32);
        } else {
            this.initialized = true;
            this.serverList = new ServerList(this.client);
            this.serverList.loadFile();

            // 从配置加载服务器列表
            loadServersFromConfig();

            this.lanServers = new LanServerQueryManager.LanServerEntryList();

            try {
                this.lanServerDetector = new LanServerQueryManager.LanServerDetector(this.lanServers);
                this.lanServerDetector.start();
            } catch (Exception exception) {
                LOGGER.warn("Unable to start LAN server detection: {}", exception.getMessage());
            }

            this.serverListWidget = new ModMultiplayerServerListWidget(this, this.client, this.width, this.height - 64 - 32, 32, 36);
            this.serverListWidget.setServers(this.serverList);
        }

        this.addDrawableChild(this.serverListWidget);
        this.buttonJoin = this.addDrawableChild(ButtonWidget.builder(Text.translatable("selectServer.select"), button -> this.connect()).width(0).build());

        // 根据配置决定编辑按钮宽度
        int editButtonWidth = config.isAllowEditServers() ? 74 : 0;
        this.buttonEdit = this.addDrawableChild(ButtonWidget.builder(Text.translatable("selectServer.edit"), button -> {
            MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
            if (entry instanceof MultiplayerServerListWidget.ServerEntry) {
                ServerInfo serverInfo = ((MultiplayerServerListWidget.ServerEntry)entry).getServer();
                this.selectedEntry = new ServerInfo(serverInfo.name, serverInfo.address, ServerInfo.ServerType.OTHER);
                this.selectedEntry.copyWithSettingsFrom(serverInfo);
                //this.client.setScreen(new AddServerScreen(this, this::editEntry, this.selectedEntry));
            }
        }).width(editButtonWidth).build());

        // 根据配置决定删除按钮宽度
        int deleteButtonWidth = config.isAllowDeleteServers() ? 74 : 0;
        this.buttonDelete = this.addDrawableChild(ButtonWidget.builder(Text.translatable("selectServer.delete"), button -> {
            MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
            if (entry instanceof MultiplayerServerListWidget.ServerEntry) {
                String string = ((MultiplayerServerListWidget.ServerEntry)entry).getServer().name;
                if (string != null) {
                    Text text = Text.translatable("selectServer.deleteQuestion");
                    Text text2 = Text.translatable("selectServer.deleteWarning", string);
                    Text text3 = Text.translatable("selectServer.deleteButton");
                    Text text4 = ScreenTexts.CANCEL;
                    //this.client.setScreen(new ConfirmScreen(this::removeEntry, text, text2, text3, text4));
                }
            }
        }).width(deleteButtonWidth).build());
        ButtonWidget buttonWidget3 = this.addDrawableChild(
                ButtonWidget.builder(Text.translatable("selectServer.refresh"), button -> this.refresh()).width(74).build()
        );

        // 同步配置按钮
        ButtonWidget syncButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("同步配置"), button -> this.syncConfig()).width(74).build()
        );

        ButtonWidget option = this.addDrawableChild(
                ButtonWidget.builder(Text.translatable("options.title"),button -> this.client.setScreen(new OptionsScreen(this, MinecraftClient.getInstance().options))).build()
        );
        DirectionalLayoutWidget directionalLayoutWidget = DirectionalLayoutWidget.vertical();

        // 第一行：连接、编辑、删除按钮
        AxisGridWidget axisGridWidget = directionalLayoutWidget.add(new AxisGridWidget(308, 20, AxisGridWidget.DisplayAxis.HORIZONTAL));
        axisGridWidget.add(this.buttonJoin);
        if (editButtonWidth > 0) {
            axisGridWidget.add(this.buttonEdit);
        }
        if (deleteButtonWidth > 0) {
            axisGridWidget.add(this.buttonDelete);
        }

        directionalLayoutWidget.add(EmptyWidget.ofHeight(4));

        // 第二行：刷新和同步配置按钮
        AxisGridWidget axisGridWidget2 = directionalLayoutWidget.add(new AxisGridWidget(308, 20, AxisGridWidget.DisplayAxis.HORIZONTAL));
        axisGridWidget2.add(buttonWidget3);
        axisGridWidget2.add(syncButton);

        // 第三行：选项按钮
        AxisGridWidget axisGridWidget3 = directionalLayoutWidget.add(new AxisGridWidget(308, 20, AxisGridWidget.DisplayAxis.VERTICAL));
        axisGridWidget2.add(option);

        directionalLayoutWidget.refreshPositions();
        SimplePositioningWidget.setPos(directionalLayoutWidget, 0, this.height - 64, this.width, 64);
        this.updateButtonActivationStates();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void tick() {
        super.tick();
        List<LanServerInfo> list = this.lanServers.getEntriesIfUpdated();
        if (list != null) {
            this.serverListWidget.setLanServers(list);
        }
        this.serverListPinger.tick();
    }

    @Override
    public void removed() {
        if (this.lanServerDetector != null) {
            this.lanServerDetector.interrupt();
            this.lanServerDetector = null;
        }

        this.serverListPinger.cancel();
        this.serverListWidget.onRemoved();
    }

    private void refresh() {
        this.client.setScreen(new ModMultiplayerScreen(this.parent));
    }

    /**
     * 同步配置
     */
    private void syncConfig() {
        syncingConfig = true;
        statusMessage = Text.literal("正在同步配置...");

        cn.epicmc.client.network.ConfigClient.getInstance().syncAll().thenAccept(success -> {
            this.client.execute(() -> {
                syncingConfig = false;
                if (success) {
                    statusMessage = Text.literal("配置同步成功！");
                    // 重新加载服务器列表
                    loadServersFromConfig();
                    this.serverListWidget.setServers(this.serverList);
                } else {
                    statusMessage = Text.literal("配置同步失败！");
                }

                // 3秒后清除状态消息
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        this.client.execute(() -> statusMessage = Text.empty());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            });
        });
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_F5) {
            this.refresh();
            return true;
        }

        if (this.serverListWidget.getSelectedOrNull() != null) {
            if (KeyCodes.isToggle(keyCode)) {
                this.connect();
                return true;
            } else {
                return this.serverListWidget.keyPressed(keyCode, scanCode, modifiers);
            }
        } else {
            return false;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // 标题
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 16777215);

        // 显示欢迎消息（如果配置中有）
        String welcomeMessage = ClientConfig.getInstance().getWelcomeMessage();
        if (!welcomeMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(welcomeMessage),
                this.width / 2, this.height - 90, 0xFFD700);  // 金色
        }

        // 显示状态消息
        if (!statusMessage.getString().isEmpty()) {
            int color = syncingConfig ? 0xFFFF00 : 0x00FF00;  // 同步中黄色，完成绿色
            context.drawCenteredTextWithShadow(this.textRenderer, statusMessage,
                this.width / 2, this.height - 78, color);
        }

        // 显示服务器数量信息
        int serverCount = ClientConfig.getInstance().getAllowedServers().size();
        if (serverCount > 0) {
            Text serverInfo = Text.literal("可用服务器: " + serverCount);
            context.drawText(this.textRenderer, serverInfo, 10, this.height - 20, 0xAAAAAA, false);
        }
    }

    public void connect() {
        MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
        if (entry instanceof ModMultiplayerServerListWidget.ServerEntry) {
            this.connect(((ModMultiplayerServerListWidget.ServerEntry)entry).getServer());
        } else if (entry instanceof ModMultiplayerServerListWidget.LanServerEntry) {
            LanServerInfo lanServerInfo = ((ModMultiplayerServerListWidget.LanServerEntry)entry).getLanServerEntry();
            this.connect(new ServerInfo(lanServerInfo.getMotd(), lanServerInfo.getAddressPort(), ServerInfo.ServerType.LAN));
        }
    }

    private void connect(ServerInfo entry) {
        ConnectScreen.connect(this, this.client, ServerAddress.parse(entry.address), entry, false, null);
    }

    public void select(MultiplayerServerListWidget.Entry entry) {
        this.serverListWidget.setSelected(entry);
        this.updateButtonActivationStates();
    }

    protected void updateButtonActivationStates() {
        ClientConfig config = ClientConfig.getInstance();

        this.buttonJoin.active = false;
        this.buttonEdit.active = false;
        this.buttonDelete.active = false;

        MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
        if (entry != null && !(entry instanceof MultiplayerServerListWidget.ScanningEntry)) {
            // 允许连接到局域网服务器
            if (entry instanceof ModMultiplayerServerListWidget.LanServerEntry) {
                this.buttonJoin.active = true;
            }
            // 允许连接到配置列表中的服务器
            else if (entry instanceof MultiplayerServerListWidget.ServerEntry) {
                ServerInfo serverInfo = ((MultiplayerServerListWidget.ServerEntry)entry).getServer();
                // 检查服务器是否在允许列表中
                boolean isAllowed = isServerAllowed(serverInfo);
                this.buttonJoin.active = isAllowed;
                this.buttonEdit.active = isAllowed && config.isAllowEditServers();
                this.buttonDelete.active = isAllowed && config.isAllowDeleteServers();
            }
        }
    }

    /**
     * 检查服务器是否在允许列表中
     */
    private boolean isServerAllowed(ServerInfo serverInfo) {
        List<ClientConfig.ServerEntry> allowedServers = ClientConfig.getInstance().getAllowedServers();
        if (allowedServers.isEmpty()) {
            return true; // 如果没有配置列表，允许所有服务器
        }

        for (ClientConfig.ServerEntry allowed : allowedServers) {
            if (serverInfo.address.equals(allowed.getFullAddress())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从配置加载服务器列表
     */
    private void loadServersFromConfig() {
        List<ClientConfig.ServerEntry> configServers = ClientConfig.getInstance().getAllowedServers();

        // 如果配置中有服务器列表，添加到本地列表
        for (ClientConfig.ServerEntry configServer : configServers) {
            boolean exists = false;

            // 检查是否已存在
            for (int i = 0; i < this.serverList.size(); i++) {
                ServerInfo existing = this.serverList.get(i);
                if (existing.address.equals(configServer.getFullAddress())) {
                    exists = true;
                    break;
                }
            }

            // 如果不存在，添加到列表
            if (!exists) {
                ServerInfo serverInfo = new ServerInfo(
                    configServer.getName(),
                    configServer.getFullAddress(),
                    ServerInfo.ServerType.OTHER
                );
                this.serverList.add(serverInfo, false); // false = 不保存到文件
            }
        }
    }

    public MultiplayerServerListPinger getServerListPinger() {
        return this.serverListPinger;
    }

    public ServerList getServerList() {
        return this.serverList;
    }
}
