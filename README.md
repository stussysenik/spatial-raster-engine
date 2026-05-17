# Spatial Raster Engine (SRE)

> A zero-dependency, bare-metal GUI rendering and layout engine inside a raw `int[]` pixel buffer.  
> Color physics. Typography matrices. Flexbox constraint cascading. Affine transforms. All from absolute first principles.

---

## Architecture

```
[ JFrame Hardware Window ]
          │
          ▼
 [ 60 Hz Engine Ticker ] ── javax.swing.Timer at 16ms intervals
          │
          ▼
 [ int[] pixels ] ── 32-bit ARGB slots, 1D array mapped to 2D via y * width + x
          │
          ├── Color.java        Challenge 1: The Color Space Pipeline
          ├── Font.java         Challenge 2: The Typographic Matrix
          ├── TextLayout.java        "       Smart Word Wrap Engine
          ├── Node.java         Challenge 3: The Spatial Box Engine
          ├── Animation.java    Challenge 4: The Reactive UI Engine
          ├── Transform.java         "       Affine Transform Matrices
          └── Dashboard.java    Verification: Audio Equalizer Grid
```

The entire application renders into a single `int[width * height]` array. Each slot is a 32-bit ARGB integer — no Graphics2D, no component hierarchy, no layout managers. Everything you see on screen was placed there by bitwise operations on raw integers.

---

## Challenge 1: The Color Space Pipeline

**File:** [`src/sre/Color.java`](src/sre/Color.java)

### ARGB Packing & Unpacking

Each pixel is one `int`. The alpha, red, green, and blue channels live in adjacent 8-bit segments:

```
Bit layout:  AAAAAAAA RRRRRRRR GGGGGGGG BBBBBBBB
             [31..24] [23..16] [15...8] [7....0]
```

```java
int alpha = (color >> 24) & 0xFF;
int red   = (color >> 16) & 0xFF;
int green = (color >>  8) & 0xFF;
int blue  =  color        & 0xFF;

int packed = (a << 24) | (r << 16) | (g << 8) | b;
```

### Porter-Duff Source-Over Alpha Blending

When drawing a semi-transparent pixel over an existing background, we compute the composite using the standard Porter-Duff "source over" equation:

$$\text{Out}\alpha = \text{Src}\alpha + \text{Dst}\alpha \times (1 - \text{Src}\alpha)$$

$$\text{Out}_{RGB} = \frac{\text{Src}_{RGB} \times \text{Src}\alpha + \text{Dst}_{RGB} \times \text{Dst}\alpha \times (1 - \text{Src}\alpha)}{\text{Out}_\alpha}$$

**Fast paths:**
- `srcA == 255` → fully opaque, return source directly (no calculation needed)
- `srcA == 0` → fully transparent, return destination directly (no blending needed)

These two early exits are the most common cases in opaque UIs and eliminate millions of unnecessary multiplications per frame.

### Linear Gradients

Linear interpolation between two ARGB colors across an arbitrary axis. The interpolant `t` ranges from 0 to 1 along the gradient axis:

```java
int lerpColor(int c0, int c1, float t)   // per-channel LERP
void fillGradientHorizontal(...)          // gradient along X axis
void fillGradientVertical(...)            // gradient along Y axis
```

All gradient fills are alpha-composited — they blend onto whatever is already in the buffer.

---

## Challenge 2: The Typographic Matrix

**Files:** [`src/sre/Font.java`](src/sre/Font.java), [`src/sre/TextLayout.java`](src/sre/TextLayout.java)

### Bitmap Glyph Storage

Every character (A–Z, 0–9, `.`, `!`, `?`, `-`) is stored as an 8×8 bitmask — one `int` per row, 8 rows per glyph. Each bit in the integer controls one pixel:

```
Glyph 'A' as 8 integers:
{0x18, 0x24, 0x42, 0x42, 0x7E, 0x42, 0x42, 0x00}

In binary:
0x18 = 00011000     ..##....
0x24 = 00100100     .#..#...
0x42 = 01000010     #....#..
0x42 = 01000010     #....#..
0x7E = 01111110     .######.
0x42 = 01000010     #....#..
0x42 = 01000010     #....#..
0x00 = 00000000     ........
```

The bitmask test: `(line >> (7 - col)) & 1` — shifts the row integer right and masks bit 0 to decide whether to place a pixel.

### Proportional Kerning

Not all characters deserve equal horizontal space. The kerning matrix assigns advance widths per glyph:

| Char | Advance | Char | Advance |
|------|---------|------|---------|
| I    | 4 px    | M, W | 7 px    |
| J, 1 | 5 px    | . ! ? | 3 px    |
| A–H, K–L, N–T, Z | 6 px | space | 4 px |

### Word Wrap Engine

`TextLayout.wrap(text, maxWidth, x, y)` splits text into lines at word boundaries:

1. Accumulate words into a candidate line
2. Measure cumulative width using `Font.measureWidth()`
3. If the candidate exceeds `maxWidth`, commit the current line and start a new one
4. If a single word exceeds the container width, hard-break it character by character

The wrapped result is a list of `Line` objects with precomputed `(x, y)` positions, ready for direct rasterization.

---

## Challenge 3: The Spatial Box Engine

**File:** [`src/sre/Node.java`](src/sre/Node.java)

### Layout Tree

Every UI element is a `Node`. Nodes form a tree. The root node's `layout()` call cascades recursively to every child.

```
Node Properties:
  x, y, width, height      ← computed absolute positions (set by layout pass)
  prefWidth, prefHeight    ← fixed size constraints
  flexGrow                 ← proportional allocation weight (0 = fixed)
  marginLeft/Right/Top/Bot ← external spacing
  paddingLeft/Right/Top/Bot← internal spacing
  direction                ← HORIZONTAL or VERTICAL flow
  children                 ← child node list
```

### The Fractional Space Allocator

When laying out children in a direction, the engine:

1. **Sums all fixed sizes** (child nodes with `flexGrow == 0`)
2. **Sums all flex weights** (`flexGrow > 0`)
3. **Computes remaining space:**

```
remaining = parentWidth − Σ(fixed sizes + margins)
```

4. **Allocates proportionally:**

```
spacePerFlex = remaining / Σ(flexGrow)
allocated[i] = flexGrow[i] × spacePerFlex
```

**Example:** A parent 800px wide with three children having flexGrow values `{1, 2, 1}` and no fixed sizes:

```
remaining    = 800 − 0 = 800
spacePerFlex = 800 / 4 = 200
child[0] = 1 × 200 = 200px
child[1] = 2 × 200 = 400px
child[2] = 1 × 200 = 200px
```

This is the same math browsers use for CSS Flexbox `flex-grow`.

### Hit Testing

`Node.hitTest(px, py)` walks the tree from root to deepest leaf, checking bounding boxes. It returns the deepest node that contains the cursor coordinates — children are tested in reverse z-order (last child on top).

---

## Challenge 4: The Reactive UI Engine

**Files:** [`src/sre/Animation.java`](src/sre/Animation.java), [`src/sre/Transform.java`](src/sre/Transform.java)

### LERP Animation Ticker

Values don't snap — they ease. The engine ticks at 60 Hz (16.67ms intervals) and advances all animated properties:

```
current = current + (target − current) × speed
```

When `|target − current| < 0.01`, the property reaches its destination and deactivates (zero CPU cost until the target changes again).

Usage in the dashboard: mouse position sets animation targets for gradient color channels and equalizer bar heights. The values smoothly chase the target.

### Affine Transform Matrices

2×3 matrix for 2D affine transformations:

```
| m00  m01  m02 |   | x |   | x' |
| m10  m11  m12 | × | y | = | y' |
                    | 1 |
```

Operations:
- **Translate:** `m02 += tx`, `m12 += ty`
- **Scale about center:** translate to center → multiply scale matrix → translate back
- **Rotate about center:** translate to center → multiply rotation matrix → translate back
- **Inverse map:** transforms screen coordinates back to local space (for hit-testing under rotation)

```java
Transform t = Transform.identity()
    .translate(100, 50)
    .rotate(0.785f, cx, cy)    // 45° about center
    .scale(1.5f, 1.5f, cx, cy);

float[] screen = t.map(localX, localY);
float[] local  = t.inverseMap(screenX, screenY);
```

---

## Verification: Audio Equalizer Grid Dashboard

**File:** [`src/sre/Dashboard.java`](src/sre/Dashboard.java) — entry point `main()`

The dashboard exercises every challenge simultaneously:

| Feature | Challenge Used |
|---------|---------------|
| Two-column layout (200px sidebar + flexible content) | Challenge 3: HORIZONTAL layout with fixed + flex child |
| Four proportional cards with `flexGrow: 1` | Challenge 3: Vertical fractional allocation |
| Multi-line auto-wrapping text in cards | Challenge 2: `TextLayout.wrap()` + `Font.drawString()` |
| Mouse-reactive semi-transparent gradient overlay | Challenge 1 + 4: Gradient fill + LERP animation on color channels |
| Animated equalizer bar visualization (20 bars, 4 bands) | Challenge 4: LERP-driven bar heights responding to cursor |
| Hit-test highlight on hover | Challenge 3: `Node.hitTest()` → highlight border |

### What you'll see

- **Left:** Dark sidebar with navigation labels ("DASHBOARD", "EQUALIZER", "SPECTRUM", "SETTINGS") and a "60 FPS TICKER" badge
- **Right:** Four cards stacked vertically:
  1. *TYPOGRAPHIC MATRIX* — "The quick brown fox jumps over the lazy dog..." auto-wrapped inside the card
  2. *EQUALIZER SPECTRUM* — 20 animated bars (4 frequency bands) that dance with your cursor
  3. *COLOR SPACE PIPELINE* — Explains ARGB packing and Porter-Duff compositing
  4. *SPATIAL BOX ENGINE* — Documents the flexbox constraint math
- **Overlay:** A vertical gradient tint that shifts hue based on mouse position
- **On hover:** Each card gets a subtle white border highlight

---

## Build & Run

```bash
# Compile
javac src/sre/*.java -d out

# Run
java -cp out sre.Dashboard
```

**Requirements:** JDK 8+. No build tools, no dependencies, no `pom.xml`, no `build.gradle`.

---

## File Map

```
.
├── src/sre/
│   ├── Color.java         ARGB packing, Porter-Duff blend, gradient fills
│   ├── Font.java          8×8 bitmap glyphs, kerning matrix, glyph rasterization
│   ├── TextLayout.java    Word-boundary wrap engine, line measurement
│   ├── Node.java          Layout tree, flexGrow allocator, render, hit-test
│   ├── Animation.java     60Hz LERP ticker, animated property tracking
│   ├── Transform.java     2×3 affine matrix (translate, rotate, scale, inverse)
│   ├── Engine.java        JFrame window, int[] buffer, 60Hz render loop
│   └── Dashboard.java     Verification UI (entry point)
└── .gitignore
```

---

## Why This Exists

Modern UI frameworks abstract away the physics of rendering behind layers of retained-mode trees, GPU shaders, and platform compositors. This project strips all of that away. Every pixel you see was placed by a bitwise operation you can trace back to a single line of Java.

Understanding this engine means understanding:
- How a browser computes `flex-grow` space distribution
- Why alpha blending uses that specific Porter-Duff equation
- How font rasterizers turn glyph outlines into pixel grids
- What an animation easing curve actually does per-frame
- How hit-testing walks a spatial tree under cursor coordinates

No magic. Just math.
