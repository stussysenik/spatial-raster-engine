package sre;

import java.util.ArrayList;
import java.util.List;

public final class TextLayout {

    public static final class Line {
        public final String text;
        public final int x, y;
        Line(String text, int x, int y) { this.text = text; this.x = x; this.y = y; }
    }

    private TextLayout() {}

    // ── Word-boundary-aware wrap into measured lines ─────────────────

    public static List<Line> wrap(String text, int maxWidth, int startX, int startY) {
        List<Line> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int cursorY = startY;

        for (String word : words) {
            String trial = line.length() == 0 ? word : line + " " + word;
            int trialW = Font.measureWidth(trial);

            if (trialW <= maxWidth) {
                if (line.length() > 0) line.append(' ');
                line.append(word);
            } else {
                if (line.length() > 0) {
                    lines.add(new Line(line.toString(), startX, cursorY));
                    cursorY += Font.lineHeight();
                    line.setLength(0);
                }
                if (Font.measureWidth(word) > maxWidth) {
                    String remainder = forceBreakWord(word, maxWidth, lines, startX, cursorY);
                    while (!remainder.isEmpty()) {
                        cursorY += Font.lineHeight();
                        remainder = forceBreakWord(remainder, maxWidth, lines, startX, cursorY);
                    }
                } else {
                    line.append(word);
                }
            }
        }
        if (line.length() > 0) {
            lines.add(new Line(line.toString(), startX, cursorY));
        }
        return lines;
    }

    // ── Hard break for words wider than the container ────────────────

    private static String forceBreakWord(String word, int maxWidth,
            List<Line> lines, int x, int y) {
        StringBuilder chunk = new StringBuilder();
        int w = 0;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            int cw = (c == ' ') ? 4 : (Font.kerning(c) + 1);
            if (w + cw > maxWidth && chunk.length() > 0) {
                lines.add(new Line(chunk.toString(), x, y));
                return word.substring(i);
            }
            chunk.append(c);
            w += cw;
        }
        if (chunk.length() > 0) {
            lines.add(new Line(chunk.toString(), x, y));
        }
        return "";
    }

    // ── Render wrapped lines to the pixel buffer ─────────────────────

    public static void drawLines(int[] pixels, int screenW, int screenH,
            List<Line> lines, int color) {
        for (Line line : lines) {
            Font.drawString(pixels, screenW, screenH, line.text, line.x, line.y, color);
        }
    }
}
