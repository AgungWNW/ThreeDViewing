package com.grafkom.viewing3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Pabrik mesh prosedural. Semua mesh memiliki koordinat UV dan winding CCW
 * (tampak depan menghadap keluar) agar kompatibel dengan Back-Face Culling.
 */
public final class MeshFactory {

    private MeshFactory() { }

    /** Bola UV (stacks x slices). Resolusi rendah sengaja dipakai agar efek Flat vs Smooth terlihat jelas. */
    public static MeshData createSphere(float radius, int stacks, int slices) {
        int vCount = (stacks + 1) * (slices + 1);
        float[] pos = new float[vCount * 3];
        float[] uv = new float[vCount * 2];
        int p = 0, t = 0;
        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI * i / stacks;
            for (int j = 0; j <= slices; j++) {
                double theta = 2.0 * Math.PI * j / slices;
                pos[p++] = (float) (radius * Math.sin(phi) * Math.cos(theta));
                pos[p++] = (float) (radius * Math.cos(phi));
                pos[p++] = (float) (radius * Math.sin(phi) * Math.sin(theta));
                uv[t++] = (float) j / slices;
                uv[t++] = 1f - (float) i / stacks;
            }
        }
        int[] idx = new int[stacks * slices * 6];
        int k = 0;
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int a = i * (slices + 1) + j;
                int a1 = a + 1;
                int b = a + slices + 1;
                int b1 = b + 1;
                idx[k++] = a;  idx[k++] = a1; idx[k++] = b;
                idx[k++] = a1; idx[k++] = b1; idx[k++] = b;
            }
        }
        return new MeshData(pos, uv, idx);
    }

    /** Torus (donat) dengan jari-jari utama R dan jari-jari tabung r. */
    public static MeshData createTorus(float R, float r, int rings, int sides) {
        int vCount = (rings + 1) * (sides + 1);
        float[] pos = new float[vCount * 3];
        float[] uv = new float[vCount * 2];
        int p = 0, t = 0;
        for (int i = 0; i <= rings; i++) {
            double theta = 2.0 * Math.PI * i / rings;
            for (int k = 0; k <= sides; k++) {
                double phi = 2.0 * Math.PI * k / sides;
                double ring = R + r * Math.cos(phi);
                pos[p++] = (float) (ring * Math.cos(theta));
                pos[p++] = (float) (r * Math.sin(phi));
                pos[p++] = (float) (ring * Math.sin(theta));
                uv[t++] = 3f * i / rings;   // diulang 3x agar kotak tekstur tidak terlalu melar
                uv[t++] = (float) k / sides;
            }
        }
        int[] idx = new int[rings * sides * 6];
        int n = 0;
        for (int i = 0; i < rings; i++) {
            for (int k = 0; k < sides; k++) {
                int a = i * (sides + 1) + k;
                int a1 = a + 1;
                int b = a + sides + 1;
                int b1 = b + 1;
                idx[n++] = a;  idx[n++] = a1; idx[n++] = b;
                idx[n++] = a1; idx[n++] = b1; idx[n++] = b;
            }
        }
        return new MeshData(pos, uv, idx);
    }

    /** Kubus dengan 24 vertex (4 per sisi) dan UV 0..1 per sisi. */
    public static MeshData createCube(float half) {
        QuadBuilder qb = new QuadBuilder();
        //            normal          sumbu-u          sumbu-v   (u x v = normal)
        qb.addQuad(1, 0, 0,   0, 0, -1,   0, 1, 0,   half, half, half, 1f);  // +X
        qb.addQuad(-1, 0, 0,  0, 0, 1,    0, 1, 0,   half, half, half, 1f);  // -X
        qb.addQuad(0, 1, 0,   1, 0, 0,    0, 0, -1,  half, half, half, 1f);  // +Y
        qb.addQuad(0, -1, 0,  1, 0, 0,    0, 0, 1,   half, half, half, 1f);  // -Y
        qb.addQuad(0, 0, 1,   1, 0, 0,    0, 1, 0,   half, half, half, 1f);  // +Z
        qb.addQuad(0, 0, -1,  -1, 0, 0,   0, 1, 0,   half, half, half, 1f);  // -Z
        return qb.build();
    }

    /** Bidang datar horizontal (normal +Y) berpusat di origin, UV diulang uvRepeat kali (GL_REPEAT). */
    public static MeshData createPlane(float halfSize, float uvRepeat) {
        QuadBuilder qb = new QuadBuilder();
        qb.addQuad(0, 1, 0,   1, 0, 0,   0, 0, -1,   halfSize, halfSize, 0f, uvRepeat);
        return qb.build();
    }

    /** Pembangun quad: titik = n*hn + u*(+-hu) + v*(+-hv), urutan CCW bila dilihat dari arah normal. */
    private static final class QuadBuilder {
        private final List<Float> pos = new ArrayList<>();
        private final List<Float> uv = new ArrayList<>();
        private final List<Integer> idx = new ArrayList<>();

        void addQuad(float nx, float ny, float nz,
                     float ux, float uy, float uz,
                     float vx, float vy, float vz,
                     float hu, float hv, float hn, float uvRepeat) {
            int base = pos.size() / 3;
            float[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
            float[][] coords  = {{0, 0}, {uvRepeat, 0}, {uvRepeat, uvRepeat}, {0, uvRepeat}};
            for (int c = 0; c < 4; c++) {
                float su = corners[c][0] * hu;
                float sv = corners[c][1] * hv;
                pos.add(nx * hn + ux * su + vx * sv);
                pos.add(ny * hn + uy * su + vy * sv);
                pos.add(nz * hn + uz * su + vz * sv);
                uv.add(coords[c][0]);
                uv.add(coords[c][1]);
            }
            idx.add(base);     idx.add(base + 1); idx.add(base + 2);
            idx.add(base);     idx.add(base + 2); idx.add(base + 3);
        }

        MeshData build() {
            float[] p = new float[pos.size()];
            for (int i = 0; i < p.length; i++) p[i] = pos.get(i);
            float[] t = new float[uv.size()];
            for (int i = 0; i < t.length; i++) t[i] = uv.get(i);
            int[] ix = new int[idx.size()];
            for (int i = 0; i < ix.length; i++) ix[i] = idx.get(i);
            return new MeshData(p, t, ix);
        }
    }
}
