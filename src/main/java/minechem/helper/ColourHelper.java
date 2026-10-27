package minechem.helper;

/**
 * Colour arithmetic. Pure Java.
 */
public class ColourHelper {
    public static int RGB(int red, int green, int blue) {
        return RGBA(red, green, blue, 255);
    }

    /**
     * The cast binds before the multiply, so for inputs in 0..1 this yields 0 or 255 only.
     */
    public static int RGB(float red, float green, float blue) {
        return RGBA((int) red * 255, (int) green * 255, (int) blue * 255, 255);
    }

    public static int RGB(String colour) {
        if (!colour.startsWith("#") || !(colour.length() == 7)) {
            throw new IllegalArgumentException("Use #RRGGBB format");
        }
        return RGB(Integer.parseInt(colour.substring(1, 3), 16), Integer.parseInt(colour.substring(3, 5), 16), Integer.parseInt(colour.substring(5, 7), 16));
    }

    public static int RGBA(int red, int green, int blue, int alpha) {
        return (alpha << 24) | ((red & 255) << 16) | ((green & 255) << 8) | ((blue & 255));
    }

    public static int blend(int... colours) {
        if (colours.length < 1) {
            throw new IllegalArgumentException();
        }
        int[] alphas = new int[colours.length];
        int[] reds = new int[colours.length];
        int[] greens = new int[colours.length];
        int[] blues = new int[colours.length];
        for (int i = 0; i < colours.length; i++) {
            alphas[i] = (colours[i] >> 24 & 0xff);
            reds[i] = ((colours[i] & 0xff0000) >> 16);
            greens[i] = ((colours[i] & 0xff00) >> 8);
            blues[i] = (colours[i] & 0xff);
        }
        float a, r, g, b;
        a = r = g = b = 0;
        float ratio = 1.0F / colours.length;
        for (int alpha : alphas) a += alpha * ratio;
        for (int red : reds) r += red * ratio;
        for (int green : greens) g += green * ratio;
        for (int blue : blues) b += blue * ratio;
        return (((int) a) << 24 | ((int) r) << 16 | ((int) g) << 8 | ((int) b));
    }

    public static int tone(int colour, float scale) {
        float r = (colour >> 16) & 255;
        float g = (colour >> 8) & 255;
        float b = colour & 255;
        return RGB(r * scale, g * scale, b * scale);
    }
}
