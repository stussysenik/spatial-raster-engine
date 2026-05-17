package sre;

public final class Transform {

    public float
        m00 = 1, m01 = 0, m02 = 0,
        m10 = 0, m11 = 1, m12 = 0;

    private Transform() {}

    public static Transform identity() {
        return new Transform();
    }

    public Transform translate(float tx, float ty) {
        m02 += tx;
        m12 += ty;
        return this;
    }

    public Transform scale(float sx, float sy, float cx, float cy) {
        translate(cx, cy);
        multiply(buildScale(sx, sy));
        translate(-cx, -cy);
        return this;
    }

    public Transform rotate(float radians, float cx, float cy) {
        translate(cx, cy);
        multiply(buildRotate(radians));
        translate(-cx, -cy);
        return this;
    }

    // ── Internal matrix ops ──────────────────────────────────────────

    private void multiply(Transform t) {
        float nm00 = m00 * t.m00 + m01 * t.m10;
        float nm01 = m00 * t.m01 + m01 * t.m11;
        float nm02 = m00 * t.m02 + m01 * t.m12 + m02;
        float nm10 = m10 * t.m00 + m11 * t.m10;
        float nm11 = m10 * t.m01 + m11 * t.m11;
        float nm12 = m10 * t.m02 + m11 * t.m12 + m12;
        m00 = nm00; m01 = nm01; m02 = nm02;
        m10 = nm10; m11 = nm11; m12 = nm12;
    }

    private static Transform buildScale(float sx, float sy) {
        Transform t = new Transform();
        t.m00 = sx;
        t.m11 = sy;
        return t;
    }

    private static Transform buildRotate(float rad) {
        Transform t = new Transform();
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        t.m00 = cos; t.m01 = -sin;
        t.m10 = sin; t.m11 =  cos;
        return t;
    }

    // ── Apply to point ───────────────────────────────────────────────

    public float[] map(float px, float py) {
        return new float[] {
            m00 * px + m01 * py + m02,
            m10 * px + m11 * py + m12
        };
    }

    public float[] inverseMap(float px, float py) {
        float det = m00 * m11 - m01 * m10;
        if (det == 0) return new float[] { px, py };
        float dx = px - m02;
        float dy = py - m12;
        return new float[] {
            ( m11 * dx - m01 * dy) / det,
            (-m10 * dx + m00 * dy) / det
        };
    }
}
