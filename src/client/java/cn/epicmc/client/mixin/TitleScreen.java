package cn.epicmc.client.mixin;

import cn.epicmc.BlockFront;
import cn.epicmc.client.screen.ModMultiplayerScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.realms.gui.screen.RealmsNotificationsScreen;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URISyntaxException;

@Mixin(value = net.minecraft.client.gui.screen.TitleScreen.class)
public class TitleScreen {
    @Shadow
    @Nullable
    //~ if >=1.19 'Screen ' -> 'RealmsNotificationsScreen '
    private RealmsNotificationsScreen realmsNotificationGui;

    // remove realms screen
    @Inject(method = "init", at = @At("TAIL"))
    public void removeRealmsScreen(CallbackInfo ci) {
        realmsNotificationGui = null;
    }
    // remove realms notifications
    @Inject(method = "isRealmsNotificationsGuiDisplayed", at = @At("RETURN"), cancellable = true)
    public void realmsNotificationsEnabled(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
    @Redirect(method = "initWidgetsNormal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;"))
    private Element initWidgetsNormal(net.minecraft.client.gui.screen.TitleScreen instance, Element element) {
        //MinecraftClient.getInstance().setScreen(new ServerLinksScreen(null, new ServerLinks(List.of(ServerLinks.Entry.create(Text.empty(), new URI("mc.epicmc.cn"))))));
        MinecraftClient.getInstance().setScreen(new ModMultiplayerScreen(null));
        MinecraftClient.getInstance().getWindow().setTitle("Eastline");
        return element;
    }
    // remove realms notifications

}
