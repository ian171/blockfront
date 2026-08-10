import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateIcons {
    public static void main(String[] args) throws Exception {
        File texDir = new File("src/main/resources/assets/blockfront/textures/gui/sprites");
        texDir.mkdirs();
        
        // Helper to create blank image
        java.util.function.Supplier<BufferedImage> create = () -> new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        
        // FLAG
        BufferedImage flag = create.get();
        Graphics2D g = flag.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(8, 4, 8, 28); // pole
        g.fillRoundRect(8, 4, 18, 12, 4, 4); // flag
        g.dispose();
        ImageIO.write(flag, "PNG", new File(texDir, "icon_flag.png"));
        
        // SHIELD
        BufferedImage shield = create.get();
        g = shield.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillPolygon(new int[]{4, 28, 28, 16, 4}, new int[]{4, 4, 16, 28, 16}, 5);
        g.dispose();
        ImageIO.write(shield, "PNG", new File(texDir, "icon_shield.png"));
        
        // SOLDIER (Helmet/Bust)
        BufferedImage soldier = create.get();
        g = soldier.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillArc(6, 6, 20, 20, 0, 180); // helmet top
        g.fillRoundRect(4, 15, 24, 4, 2, 2); // helmet brim
        g.fillRoundRect(10, 19, 12, 8, 4, 4); // face/neck
        g.fillRoundRect(6, 26, 20, 6, 4, 4); // shoulders
        g.dispose();
        ImageIO.write(soldier, "PNG", new File(texDir, "icon_soldier.png"));
        
        // ARMOR (Chestplate)
        BufferedImage armor = create.get();
        g = armor.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRoundRect(8, 6, 16, 22, 6, 6); // body
        g.fillRoundRect(4, 6, 6, 12, 4, 4); // left arm
        g.fillRoundRect(22, 6, 6, 12, 4, 4); // right arm
        g.setComposite(AlphaComposite.Clear);
        g.fillOval(10, 2, 12, 8); // neck hole
        g.dispose();
        ImageIO.write(armor, "PNG", new File(texDir, "icon_armor.png"));
        
        // CROSS (Medical/Death)
        BufferedImage cross = create.get();
        g = cross.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRoundRect(12, 4, 8, 24, 4, 4);
        g.fillRoundRect(4, 12, 24, 8, 4, 4);
        g.dispose();
        ImageIO.write(cross, "PNG", new File(texDir, "icon_cross.png"));
        
        System.out.println("Icons generated successfully!");
    }
}