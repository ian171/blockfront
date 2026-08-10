package cn.epicmc.client;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
public class TestContext {
    public void test(DrawContext ctx) {
        ctx.drawGuiTexture(Identifier.of("test"), 0, 0, 10, 10, 0xFFFFFF);
    }
}