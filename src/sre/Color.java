package sre;

public final class Color {

    private Color() {}

    // ── Packing / Unpacking ──────────────────────────────────────────

    public static int rgba(int r, int g, int b, int a) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int alpha(int color) {
        return (color >> 24) & 0xFF;
    }

    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    private static int clamp(int v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }

    // ── Porter-Duff Source-Over Alpha Blending ───────────────────────

    public static int blend(int src, int dst) {
        int srcA = alpha(src);
        if (srcA == 255) return src;
        if (srcA == 0)   return dst;

        int dstA = alpha(dst);
        int outA = srcA + (dstA * (255 - srcA) / 255);
        if (outA == 0) return 0;

        int srcR = red(src),   srcG = green(src),   srcB = blue(src);
        int dstR = red(dst),   dstG = green(dst),   dstB = blue(dst);

        int invSrcA = 255 - srcA;
        int outR = (srcR * srcA + dstR * dstA * invSrcA / 255) / outA;
        int outG = (srcG * srcA + dstG * dstA * invSrcA / 255) / outA;
        int outB = (srcB * srcA + dstB * dstA * invSrcA / 255) / outA;

        return (clamp(outA) << 24) | (clamp(outR) << 16) | (clamp(outG) << 8) | clamp(outB);
    }

    // ── Linear Gradient ──────────────────────────────────────────────

    public static int lerpColor(int c0, int c1, float t) {
        float tt = t < 0 ? 0 : (t > 1 ? 1 : t);
        int a = (int) (alpha(c0) + (alpha(c1) - alpha(c0)) * tt);
        int r = (int) (red(c0)   + (red(c1)   - red(c0))   * tt);
        int g = (int) (green(c0) + (green(c1) - green(c0)) * tt);
        int b = (int) (blue(c0)  + (blue(c1)  - blue(c0))  * tt);
        return rgba(r, g, b, a);
    }

    public static void fillGradientHorizontal(int[] pixels, int screenW, int screenH,
            int x, int y, int w, int h, int leftColor, int rightColor) {
        int rowEnd = Math.min(y + h, screenH);
        int colEnd = Math.min(x + w, screenW);
        for (int row = Math.max(y, 0); row < rowEnd; row++) {
            int base = row * screenW;
            for (int col = Math.max(x, 0); col < colEnd; col++) {
                float t = (float) (col - x) / Math.max(w - 1, 1);
                int idx = base + col;
                pixels[idx] = blend(lerpColor(leftColor, rightColor, t), pixels[idx]);
            }
        }
    }

    public static void fillGradientVertical(int[] pixels, int screenW, int screenH,
            int x, int y, int w, int h, int topColor, int bottomColor) {
        int rowEnd = Math.min(y + h, screenH);
        int colEnd = Math.min(x + w, screenW);
        for (int row = Math.max(y, 0); row < rowEnd; row++) {
            float t = (float) (row - y) / Math.max(h - 1, 1);
            int blended = lerpColor(topColor, bottomColor, t);
            int base = row * screenW;
            for (int col = Math.max(x, 0); col < colEnd; col++) {
                pixels[base + col] = blend(blended, pixels[base + col]);
            }
        }
    }
}
