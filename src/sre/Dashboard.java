package sre;

public class Dashboard implements Engine.RenderCallback {

    private final Engine engine;
    private final Node root;

    private final int[][] equalizerBars;
    private static final int EQ_BARS = 20;
    private static final int ANIM_SPEED = 32;

    private int animGradientR, animGradientG, animGradientB;
    private int animEq1, animEq2, animEq3, animEq4;

    public Dashboard() {
        engine = new Engine(960, 600, "Spatial Raster Engine - Audio Equalizer Dashboard");
        root = buildLayout();
        equalizerBars = new int[4][EQ_BARS];

        var a = engine.anim();
        int mid = 128;
        animGradientR = a.animate(mid, 0.08f, v -> animGradientR = (int) v);
        animGradientG = a.animate(mid, 0.08f, v -> animGradientG = (int) v);
        animGradientB = a.animate(mid, 0.08f, v -> animGradientB = (int) v);
        animEq1 = a.animate(mid, 0.06f, v -> animEq1 = (int) v);
        animEq2 = a.animate(mid, 0.06f, v -> animEq2 = (int) v);
        animEq3 = a.animate(mid, 0.06f, v -> animEq3 = (int) v);
        animEq4 = a.animate(mid, 0.06f, v -> animEq4 = (int) v);
    }

    // ── Layout Tree Construction ─────────────────────────────────────

    private Node buildLayout() {
        Node root = new Node();
        root.direction = Node.Direction.HORIZONTAL;

        // ── Sidebar (fixed 200px) ──
        Node sidebar = new Node();
        sidebar.prefWidth = 200;
        sidebar.bgColor = Color.rgba(18, 22, 30, 255);
        sidebar.borderColor = Color.rgba(40, 44, 52, 255);
        sidebar.borderWidth = 1;
        sidebar.paddingLeft = 12;
        sidebar.paddingRight = 12;
        sidebar.paddingTop = 16;
        sidebar.paddingBottom = 16;

        Node navTitle = new Node();
        navTitle.prefHeight = 32;
        navTitle.text = "SRE ENGINE";
        navTitle.textColor = Color.rgba(100, 140, 255, 255);
        sidebar.children.add(navTitle);

        String[] navItems = {"DASHBOARD", "EQUALIZER", "SPECTRUM", "SETTINGS"};
        for (String item : navItems) {
            Node navItem = new Node();
            navItem.prefHeight = 28;
            navItem.text = item;
            navItem.textColor = Color.rgba(180, 190, 210, 255);
            sidebar.children.add(navItem);
        }

        Node spacer = new Node();
        spacer.flexGrow = 1;
        sidebar.children.add(spacer);

        Node statusText = new Node();
        statusText.prefHeight = 28;
        statusText.text = "60 FPS TICKER";
        statusText.textColor = Color.rgba(80, 90, 110, 255);
        sidebar.children.add(statusText);

        root.children.add(sidebar);

        // ── Main Content (flexible) ──
        Node main = new Node();
        main.flexGrow = 1;
        main.direction = Node.Direction.VERTICAL;
        main.bgColor = Color.rgba(12, 14, 20, 255);
        main.paddingLeft = 8;
        main.paddingRight = 8;
        main.paddingTop = 8;
        main.paddingBottom = 8;

        // Card 0 — text block
        Node textCard = card("TYPOGRAPHIC MATRIX",
            "The quick brown fox jumps over the lazy dog. " +
            "Every letter is rasterized from raw bitmask glyphs " +
            "stored as 8x8 primitive arrays. Word wrapping is " +
            "calculated character by character using kerning " +
            "matrices and proportional advance widths.",
            1, Color.rgba(25, 30, 45, 200), Color.rgba(100, 180, 140, 255));
        main.children.add(textCard);

        // Card 1 — equalizer visualization
        Node eqCard = new Node();
        eqCard.prefHeight = 140;
        eqCard.flexGrow = 1;
        eqCard.bgColor = Color.rgba(25, 30, 45, 200);
        eqCard.borderColor = Color.rgba(40, 44, 52, 255);
        eqCard.borderWidth = 1;
        eqCard.paddingLeft = 12;
        eqCard.paddingTop = 8;

        Node eqLabel = new Node();
        eqLabel.prefHeight = 20;
        eqLabel.text = "EQUALIZER SPECTRUM";
        eqLabel.textColor = Color.rgba(255, 180, 80, 255);
        eqCard.children.add(eqLabel);

        main.children.add(eqCard);

        // Card 2 — color pipeline
        Node colorCard = card("COLOR SPACE PIPELINE",
            "ARGB packing: 32-bit integer slots split via bit " +
            "shifting. Porter-Duff source-over compositing with " +
            "fractional alpha. Linear gradient interpolation " +
            "between two arbitrary coordinate points.",
            1, Color.rgba(35, 25, 55, 200), Color.rgba(200, 140, 255, 255));
        main.children.add(colorCard);

        // Card 3 — layout engine
        Node layoutCard = card("SPATIAL BOX ENGINE",
            "Flexbox constraint cascading. Fractional space " +
            "allocation: Remaining = ParentW - Sum(Fixed). " +
            "SpacePerFlex = Remaining / Sum(FlexGrow). " +
            "FinalW = FlexGrow * SpacePerFlex.",
            1, Color.rgba(25, 35, 40, 200), Color.rgba(100, 200, 220, 255));
        main.children.add(layoutCard);

        root.children.add(main);
        return root;
    }

    private static Node card(String title, String body, int flex, int bg, int tc) {
        Node n = new Node();
        n.prefHeight = 120;
        n.flexGrow = flex;
        n.bgColor = bg;
        n.borderColor = Color.rgba(40, 44, 52, 255);
        n.borderWidth = 1;
        n.paddingLeft = 12;
        n.paddingRight = 12;
        n.paddingTop = 8;
        n.paddingBottom = 8;

        Node t = new Node();
        t.prefHeight = 20;
        t.text = title;
        t.textColor = tc;
        n.children.add(t);

        Node b = new Node();
        b.flexGrow = 1;
        b.text = body;
        b.textColor = Color.rgba(180, 190, 210, 255);
        n.children.add(b);

        return n;
    }

    // ── Render ───────────────────────────────────────────────────────

    @Override
    public void render(Engine e) {
        engine.clear();

        // Layout recalc — dynamic on every frame for responsiveness
        root.layout(0, 0, engine.width, engine.height);

        // Update gradient animation targets based on mouse position
        float mx = (float) engine.mouseX() / engine.width;
        float my = (float) engine.mouseY() / engine.height;
        var a = engine.anim();
        a.setTarget(animGradientR, 40  + mx * 80);
        a.setTarget(animGradientG, 20  + my * 60);
        a.setTarget(animGradientB, 100 + mx * 60 + my * 40);
        a.setTarget(animEq1, 40 + my * 180);
        a.setTarget(animEq2, 30 + mx * 200);
        a.setTarget(animEq3, 50 + (1 - my) * 170);
        a.setTarget(animEq4, 20 + (1 - mx) * 190);

        // Render the layout tree
        root.render(engine.pixels, engine.width, engine.height);

        // Gradient overlay over main content (Challenge 1 + 4)
        Node main = root.children.get(1);
        int glowR = animGradientR, glowG = animGradientG, glowB = animGradientB;
        Color.fillGradientVertical(engine.pixels, engine.width, engine.height,
            main.x, main.y, main.width, main.height / 2,
            Color.rgba(glowR, glowG, glowB, 15),
            Color.rgba(0, 0, 0, 0));
        Color.fillGradientVertical(engine.pixels, engine.width, engine.height,
            main.x, main.y + main.height / 2,
            main.width, main.height - main.height / 2,
            Color.rgba(0, 0, 0, 0),
            Color.rgba(glowR, glowG, glowB, 12));

        // Equalizer bars inside the EQ card (Challenge 4 LERP on heights)
        Node eqCard = main.children.get(1);
        int eqX = eqCard.x + eqCard.paddingLeft;
        int eqY = eqCard.y + eqCard.paddingTop + 24;
        int eqW = eqCard.width - eqCard.paddingLeft - eqCard.paddingRight;
        int eqH = eqCard.height - eqCard.paddingTop - eqCard.paddingBottom - 28;
        int barW = eqW / EQ_BARS - 2;

        for (int i = 0; i < EQ_BARS; i++) {
            float t = (float) i / EQ_BARS;
            float base = (float) (Math.sin(t * Math.PI * 4 + my * 8) * 0.5 + 0.5);
            float heightFrac = base * 0.6f + 0.15f;

            // Modulate by the four animated EQ values
            float mod;
            if (i < EQ_BARS / 4)       mod = animEq1 / 255f;
            else if (i < EQ_BARS / 2)  mod = animEq2 / 255f;
            else if (i < 3 * EQ_BARS / 4) mod = animEq3 / 255f;
            else                       mod = animEq4 / 255f;

            heightFrac *= (0.4f + 0.6f * mod);

            int barH = (int) (eqH * heightFrac);
            int bx = eqX + i * (barW + 2);
            int by = eqY + eqH - barH;

            int barColor = Color.lerpColor(
                Color.rgba(255, 180, 80, 220),
                Color.rgba(255, 80, 120, 220),
                t);
            engine.fillRect(bx, by, barW, barH, barColor);

            int glow = Color.rgba(255, 180, 80, 40);
            engine.fillRect(bx, by - 1, barW, 1, glow);
        }

        // Hit-test indicator under cursor
        Node hit = root.hitTest(engine.mouseX(), engine.mouseY());
        if (hit != null && hit != root && hit.bgColor != 0) {
            int hx = hit.x, hy = hit.y, hw = hit.width, hh = hit.height;
            int highlight = Color.rgba(255, 255, 255, 8);
            engine.fillRect(hx, hy, hw, 1, highlight);
            engine.fillRect(hx, hy + hh - 1, hw, 1, highlight);
            engine.fillRect(hx, hy, 1, hh, highlight);
            engine.fillRect(hx + hw - 1, hy, 1, hh, highlight);
        }
    }

    // ── Entry Point ──────────────────────────────────────────────────

    public static void main(String[] args) {
        Dashboard d = new Dashboard();
        d.engine.start(d);
    }
}
