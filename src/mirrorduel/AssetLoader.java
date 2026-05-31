package mirrorduel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AssetLoader {
    private static final Map<String, BufferedImage> cache = new HashMap<>();

    public static BufferedImage load(String name) {
        if (cache.containsKey(name)) return cache.get(name);
        try {
            InputStream is = AssetLoader.class.getResourceAsStream("/assets/" + name);
            if (is == null) {
                System.err.println("Asset not found: " + name);
                return null;
            }
            BufferedImage img = ImageIO.read(is);
            cache.put(name, img);
            return img;
        } catch (Exception e) {
            System.err.println("Failed to load asset: " + name + " — " + e.getMessage());
            return null;
        }
    }

    public static BufferedImage scaled(String name, int w, int h) {
        String key = name + "_" + w + "x" + h;
        if (cache.containsKey(key)) return cache.get(key);
        BufferedImage src = load(name);
        if (src == null) return null;
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        cache.put(key, scaled);
        return scaled;
    }
}
