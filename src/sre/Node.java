package sre;

import java.util.ArrayList;
import java.util.List;

public class Node {

    public enum Direction { HORIZONTAL, VERTICAL }

    // Computed absolute layout (set by layout pass)
    public int x, y, width, height;

    // Declared constraints
    public int prefWidth, prefHeight;
    public int flexGrow;
    public int marginLeft, marginRight, marginTop, marginBottom;
    public int paddingLeft, paddingRight, paddingTop, paddingBottom;

    // Drawing state
    public int bgColor;
    public int borderColor;
    public int borderWidth;
    public String text;
    public int textColor;

    public Direction direction = Direction.VERTICAL;
    public final List<Node> children = new ArrayList<>();

    public Node() {
        this.prefWidth = 0;
        this.prefHeight = 0;
        this.flexGrow = 0;
        this.bgColor = 0x00000000;
        this.borderColor = 0x00000000;
        this.textColor = 0xFFFFFFFF;
    }

    public Node child(Node n) { children.add(n); return this; }

    // ── Layout Pass ──────────────────────────────────────────────────

    public void layout(int parentX, int parentY, int parentW, int parentH) {
        x = parentX + marginLeft;
        y = parentY + marginTop;
        int availW = parentW - marginLeft - marginRight;
        int availH = parentH - marginTop - marginBottom;

        if (direction == Direction.HORIZONTAL) {
            layoutHorizontal(x, y, availW, availH);
        } else {
            layoutVertical(x, y, availW, availH);
        }
    }

    private void layoutHorizontal(int sx, int sy, int availW, int availH) {
        width = (flexGrow == 0) ? Math.max(prefWidth, availW) : availW;
        height = availH;

        int totalFlex = 0;
        int fixedTotal = 0;
        for (Node child : children) {
            fixedTotal += child.marginLeft + child.marginRight;
            if (child.flexGrow > 0) totalFlex += child.flexGrow;
            else fixedTotal += child.prefWidth;
        }

        int remaining = Math.max(0, availW - fixedTotal);
        int cursorX = sx;

        for (Node child : children) {
            int allocatedW = child.prefWidth;
            if (child.flexGrow > 0 && totalFlex > 0) {
                allocatedW = (child.flexGrow * remaining) / totalFlex;
            }
            child.layout(cursorX, sy, allocatedW, availH);
            cursorX += allocatedW + child.marginLeft + child.marginRight;
        }
    }

    private void layoutVertical(int sx, int sy, int availW, int availH) {
        width = availW;
        height = (flexGrow == 0) ? Math.max(prefHeight, availH) : availH;

        int totalFlex = 0;
        int fixedTotal = 0;
        for (Node child : children) {
            fixedTotal += child.marginTop + child.marginBottom;
            if (child.flexGrow > 0) totalFlex += child.flexGrow;
            else fixedTotal += child.prefHeight;
        }

        int remaining = Math.max(0, availH - fixedTotal);
        int cursorY = sy;

        for (Node child : children) {
            int allocatedH = child.prefHeight;
            if (child.flexGrow > 0 && totalFlex > 0) {
                allocatedH = (child.flexGrow * remaining) / totalFlex;
            }
            child.layout(sx, cursorY, availW, allocatedH);
            cursorY += allocatedH + child.marginTop + child.marginBottom;
        }
    }

    // ── Render ───────────────────────────────────────────────────────

    public void render(int[] pixels, int screenW, int screenH) {
        // Background fill
        int innerX = x + paddingLeft;
        int innerY = y + paddingTop;
        int innerW = width - paddingLeft - paddingRight;
        int innerH = height - paddingTop - paddingBottom;

        if (Color.alpha(bgColor) > 0) {
            for (int row = innerY; row < innerY + innerH && row < screenH; row++) {
                if (row < 0) continue;
                int base = row * screenW;
                for (int col = innerX; col < innerX + innerW && col < screenW; col++) {
                    if (col < 0) continue;
                    pixels[base + col] = Color.blend(bgColor, pixels[base + col]);
                }
            }
        }

        // Border (simple rect outline)
        if (Color.alpha(borderColor) > 0 && borderWidth > 0) {
            // top & bottom
            for (int bw = 0; bw < borderWidth; bw++) {
                int topRow = y + bw;
                int botRow = y + height - 1 - bw;
                if (topRow >= 0 && topRow < screenH) {
                    int base = topRow * screenW;
                    for (int col = x; col < x + width && col < screenW; col++) {
                        if (col >= 0) pixels[base + col] = Color.blend(borderColor, pixels[base + col]);
                    }
                }
                if (botRow >= 0 && botRow < screenH) {
                    int base = botRow * screenW;
                    for (int col = x; col < x + width && col < screenW; col++) {
                        if (col >= 0) pixels[base + col] = Color.blend(borderColor, pixels[base + col]);
                    }
                }
            }
            for (int bw = 0; bw < borderWidth; bw++) {
                int leftCol = x + bw;
                int rightCol = x + width - 1 - bw;
                for (int row = y; row < y + height && row < screenH; row++) {
                    if (row >= 0) {
                        int base = row * screenW;
                        if (leftCol >= 0 && leftCol < screenW)
                            pixels[base + leftCol] = Color.blend(borderColor, pixels[base + leftCol]);
                        if (rightCol >= 0 && rightCol < screenW)
                            pixels[base + rightCol] = Color.blend(borderColor, pixels[base + rightCol]);
                    }
                }
            }
        }

        // Text
        if (text != null && !text.isEmpty()) {
            int maxW = innerW;
            int textX = innerX + 2;
            int textY = innerY + 2;
            java.util.List<TextLayout.Line> lines = TextLayout.wrap(text, maxW, textX, textY);
            TextLayout.drawLines(pixels, screenW, screenH, lines, textColor);
        }

        // Children
        for (Node child : children) {
            child.render(pixels, screenW, screenH);
        }
    }

    // ── Hit Testing ──────────────────────────────────────────────────

    public Node hitTest(int px, int py) {
        if (px < x || px >= x + width || py < y || py >= y + height) return null;
        for (int i = children.size() - 1; i >= 0; i--) {
            Node hit = children.get(i).hitTest(px, py);
            if (hit != null) return hit;
        }
        return this;
    }
}
