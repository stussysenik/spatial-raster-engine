package sre;

public final class Animation {

    public interface FloatConsumer {
        void accept(float value);
    }

    private static final class Prop {
        float current;
        float target;
        float speed;
        FloatConsumer onUpdate;
        boolean active = true;

        Prop(float start, float speed, FloatConsumer onUpdate) {
            this.current = start;
            this.target = start;
            this.speed = speed;
            this.onUpdate = onUpdate;
        }
    }

    private final Prop[] props;
    private int count;

    public Animation(int capacity) {
        props = new Prop[capacity];
    }

    public int animate(float initial, float speed, FloatConsumer onUpdate) {
        Prop p = new Prop(initial, speed, onUpdate);
        int id = count;
        props[count++] = p;
        return id;
    }

    public void setTarget(int propId, float target) {
        if (propId >= 0 && propId < count) {
            props[propId].target = target;
            props[propId].active = true;
        }
    }

    public float current(int propId) {
        if (propId >= 0 && propId < count) {
            return props[propId].current;
        }
        return 0;
    }

    public void kill(int propId) {
        if (propId >= 0 && propId < count) {
            props[propId].active = false;
        }
    }

    // ── Tick: advance all active properties one frame ────────────────

    public void tick() {
        for (int i = 0; i < count; i++) {
            Prop p = props[i];
            if (!p.active) continue;
            float diff = p.target - p.current;
            if (Math.abs(diff) < 0.01f) {
                p.current = p.target;
                p.active = false;
            } else {
                p.current += diff * p.speed;
            }
            p.onUpdate.accept(p.current);
        }
    }

    // ── Static LERP utility ──────────────────────────────────────────

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static int lerpColor(int c0, int c1, float t) {
        return Color.lerpColor(c0, c1, t);
    }
}
