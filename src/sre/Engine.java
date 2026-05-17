package sre;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Engine {

    private final JFrame frame;
    private final BufferedImage image;
    public final int[] pixels;
    public final int width, height;

    private final Animation animation;
    private RenderCallback renderCallback;
    private int mouseX, mouseY;
    private boolean mouseDown;

    public interface RenderCallback {
        void render(Engine engine);
    }

    public Engine(int w, int h, String title) {
        this.width = w;
        this.height = h;
        this.animation = new Animation(64);

        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            }
        };
        panel.setPreferredSize(new Dimension(w, h));
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e)   { updateMouse(e); }
            public void mouseDragged(MouseEvent e) { updateMouse(e); }
        });
        panel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e)  { mouseDown = true; updateMouse(e); }
            public void mouseReleased(MouseEvent e) { mouseDown = false; updateMouse(e); }
        });
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void updateMouse(MouseEvent e) {
        float sx = (float) width / frame.getContentPane().getWidth();
        float sy = (float) height / frame.getContentPane().getHeight();
        mouseX = (int) (e.getX() * sx);
        mouseY = (int) (e.getY() * sy);
    }

    // ── Public mouse state ───────────────────────────────────────────

    public int mouseX() { return mouseX; }
    public int mouseY() { return mouseY; }
    public boolean mouseDown() { return mouseDown; }

    // ── Pixel buffer access ──────────────────────────────────────────

    public void clear() {
        java.util.Arrays.fill(pixels, 0xFF000000);
    }

    public void clear(int color) {
        java.util.Arrays.fill(pixels, color);
    }

    public void setPixel(int x, int y, int color) {
        if (x < 0 || x >= width || y < 0 || y >= height) return;
        pixels[y * width + x] = Color.blend(color, pixels[y * width + x]);
    }

    public int getPixel(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return 0;
        return pixels[y * width + x];
    }

    // ── Shape drawing ────────────────────────────────────────────────

    public void fillRect(int x, int y, int w, int h, int color) {
        for (int row = y; row < y + h && row < height; row++) {
            if (row < 0) continue;
            int base = row * width;
            int end = Math.min(x + w, width);
            for (int col = Math.max(x, 0); col < end; col++) {
                pixels[base + col] = Color.blend(color, pixels[base + col]);
            }
        }
    }

    // ── Animation access ─────────────────────────────────────────────

    public Animation anim() { return animation; }

    // ── Start the 60Hz ticker ────────────────────────────────────────

    public void start(RenderCallback cb) {
        this.renderCallback = cb;
        new Timer(16, e -> {
            animation.tick();
            if (renderCallback != null) {
                renderCallback.render(this);
            }
            frame.repaint();
        }).start();
    }
}
