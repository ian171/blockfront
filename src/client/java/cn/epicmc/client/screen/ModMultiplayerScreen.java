package cn.epicmc.client.screen;

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
    public ModMultiplayerScreen(Screen parent) {
        super(Text.translatable("multiplayer.title"));
        this.parent = parent;
    }
    @Override
    protected void init() {
        if (this.initialized) {
            this.serverListWidget.setDimensionsAndPosition(this.width, this.height - 64 - 32, 0, 32);
        } else {
            this.initialized = true;
            this.serverList = new ServerList(this.client);
            this.serverList.loadFile();
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
        this.buttonJoin = this.addDrawableChild(ButtonWidget.builder(Text.translatable("selectServer.select"), button -> this.connect()).width(100).build());
        this.buttonEdit = this.addDrawableChild(ButtonWidget.builder(Text.translatable("selectServer.edit"), button -> {
            MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
            if (entry instanceof MultiplayerServerListWidget.ServerEntry) {
                ServerInfo serverInfo = ((MultiplayerServerListWidget.ServerEntry)entry).getServer();
                this.selectedEntry = new ServerInfo(serverInfo.name, serverInfo.address, ServerInfo.ServerType.OTHER);
                this.selectedEntry.copyWithSettingsFrom(serverInfo);
                //this.client.setScreen(new AddServerScreen(this, this::editEntry, this.selectedEntry));
            }
        }).width(0).build());
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
        }).width(0).build());
        ButtonWidget buttonWidget3 = this.addDrawableChild(
                ButtonWidget.builder(Text.translatable("selectServer.refresh"), button -> this.refresh()).width(74).build()
        );
        ButtonWidget option = this.addDrawableChild(
                ButtonWidget.builder(Text.translatable("options.title"),button -> this.client.setScreen(new OptionsScreen(this, MinecraftClient.getInstance().options))).build()
        );
        DirectionalLayoutWidget directionalLayoutWidget = DirectionalLayoutWidget.vertical();
        AxisGridWidget axisGridWidget = directionalLayoutWidget.add(new AxisGridWidget(308, 20, AxisGridWidget.DisplayAxis.HORIZONTAL));
        axisGridWidget.add(this.buttonJoin);


        directionalLayoutWidget.add(EmptyWidget.ofHeight(4));
        AxisGridWidget axisGridWidget2 = directionalLayoutWidget.add(new AxisGridWidget(308, 20, AxisGridWidget.DisplayAxis.HORIZONTAL));

        axisGridWidget2.add(buttonWidget3);
        AxisGridWidget axisGridWidget3 = directionalLayoutWidget.add(new AxisGridWidget(308, 20, AxisGridWidget.DisplayAxis.VERTICAL));
        axisGridWidget3.add(option);
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
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 16777215);
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
        this.buttonJoin.active = false;
        this.buttonEdit.active = false;
        this.buttonDelete.active = false;
        MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
        if (entry != null && !(entry instanceof MultiplayerServerListWidget.ScanningEntry)) {
            this.buttonJoin.active = true;
            if (entry instanceof MultiplayerServerListWidget.ServerEntry) {
                this.buttonJoin.active = false;
                this.buttonEdit.active = false;
                this.buttonDelete.active = false;
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
