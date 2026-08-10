import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

public class GenerateTextures {
    public static void main(String[] args) throws Exception {
        File texDir = new File("src/main/resources/assets/blockfront/textures/gui");
        texDir.mkdirs();
        File guiDir = new File("src/main/resources/assets/blockfront/gui");
        guiDir.mkdirs();
        
        // 1. Panel (9-slice compatible)
        BufferedImage panel = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = panel.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(25, 30, 25, 180)); // Dark olive translucent
        g.fillRoundRect(0, 0, 64, 64, 16, 16);
        g.setColor(new Color(200, 200, 200, 40)); // subtle border
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(1, 1, 62, 62, 14, 14);
        g.dispose();
        ImageIO.write(panel, "PNG", new File(texDir, "panel.png"));
        
        String panelJson = "{ \"textures\": { \"default\": \"blockfront:gui/panel\" }, \"scaling\": { \"type\": \"nine_slice\", \"width\": 64, \"height\": 64, \"border\": 8 } }";
        Files.write(Paths.get(guiDir.getPath(), "panel.json"), panelJson.getBytes());

        // 2. Bar Background
        BufferedImage barBg = new BufferedImage(64, 16, BufferedImage.TYPE_INT_ARGB);
        g = barBg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(10, 15, 10, 200));
        g.fillRoundRect(0, 0, 64, 16, 8, 8);
        g.dispose();
        ImageIO.write(barBg, "PNG", new File(texDir, "bar_bg.png"));
        
        String barBgJson = "{ \"textures\": { \"default\": \"blockfront:gui/bar_bg\" }, \"scaling\": { \"type\": \"nine_slice\", \"width\": 64, \"height\": 16, \"border\": 4 } }";
        Files.write(Paths.get(guiDir.getPath(), "bar_bg.json"), barBgJson.getBytes());

        // 3. Bar Foreground (White, to be tinted in code)
        BufferedImage barFg = new BufferedImage(64, 16, BufferedImage.TYPE_INT_ARGB);
        g = barFg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255, 255, 255, 255));
        g.fillRoundRect(0, 0, 64, 16, 8, 8);
        g.dispose();
        ImageIO.write(barFg, "PNG", new File(texDir, "bar_fg.png"));
        
        String barFgJson = "{ \"textures\": { \"default\": \"blockfront:gui/bar_fg\" }, \"scaling\": { \"type\": \"nine_slice\", \"width\": 64, \"height\": 16, \"border\": 4 } }";
        Files.write(Paths.get(guiDir.getPath(), "bar_fg.json"), barFgJson.getBytes());

        System.out.println("Textures and JSON generated successfully!");
    }
}