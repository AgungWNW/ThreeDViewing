package com.grafkom.viewing3d;

import java.util.HashMap;
import java.util.Map;

/**
 * Data geometri mesh di sisi CPU (tanpa ketergantungan OpenGL).
 *
 * Menyimpan posisi, koordinat UV dan indeks segitiga, lalu mampu membangun dua
 * versi vertex buffer interleaved [pos(3) | normal(3) | uv(2)]:
 *   - Smooth : normal vertex = rata-rata normal semua face yang berbagi posisi vertex
 *              N_vertex = (SUM N_face_i) / ||SUM N_face_i||        (Indikator 2)
 *   - Flat   : tiap segitiga punya 3 vertex sendiri dengan satu normal face
 */
public class MeshData {

    public static final int FLOATS_PER_VERTEX = 8; // 3 pos + 3 normal + 2 uv

    private final float[] positions; // xyz per vertex
    private final float[] uvs;       // uv per vertex
    private final int[] indices;     // 3 indeks per segitiga (CCW = tampak depan)

    private record PosKey(int x, int y, int z) { }

    public MeshData(float[] positions, float[] uvs, int[] indices) {
        this.positions = positions;
        this.uvs = uvs;
        this.indices = indices;
    }

    public int getVertexCount()   { return positions.length / 3; }
    public int getTriangleCount() { return indices.length / 3; }
    public int[] getIndices()     { return indices; }
    public float[] getPositions() { return positions; }

    /** Normal face (unit) dari segitiga a-b-c; null bila segitiga degenerate (luas nol). */
    public float[] faceNormal(int a, int b, int c) {
        float ax = positions[a * 3],     ay = positions[a * 3 + 1], az = positions[a * 3 + 2];
        float bx = positions[b * 3],     by = positions[b * 3 + 1], bz = positions[b * 3 + 2];
        float cx = positions[c * 3],     cy = positions[c * 3 + 1], cz = positions[c * 3 + 2];
        float e1x = bx - ax, e1y = by - ay, e1z = bz - az;
        float e2x = cx - ax, e2y = cy - ay, e2z = cz - az;
        float nx = e1y * e2z - e1z * e2y;
        float ny = e1z * e2x - e1x * e2z;
        float nz = e1x * e2y - e1y * e2x;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-8f) {
            return null;
        }
        return new float[]{nx / len, ny / len, nz / len};
    }

    private PosKey keyOf(int v) {
        // Kuantisasi agar vertex duplikat (jahitan UV / sisi kubus) dianggap satu posisi
        return new PosKey(Math.round(positions[v * 3] * 10000f),
                          Math.round(positions[v * 3 + 1] * 10000f),
                          Math.round(positions[v * 3 + 2] * 10000f));
    }

    /** Vertex Normal Averaging berdasarkan posisi (welded). */
    public float[] computeSmoothNormals() {
        Map<PosKey, float[]> acc = new HashMap<>();
        for (int t = 0; t < indices.length; t += 3) {
            float[] n = faceNormal(indices[t], indices[t + 1], indices[t + 2]);
            if (n == null) {
                continue;
            }
            for (int k = 0; k < 3; k++) {
                float[] s = acc.computeIfAbsent(keyOf(indices[t + k]), key -> new float[3]);
                s[0] += n[0];
                s[1] += n[1];
                s[2] += n[2];
            }
        }
        float[] normals = new float[getVertexCount() * 3];
        for (int v = 0; v < getVertexCount(); v++) {
            float[] s = acc.get(keyOf(v));
            float len = (s == null) ? 0f : (float) Math.sqrt(s[0] * s[0] + s[1] * s[1] + s[2] * s[2]);
            if (len < 1e-8f) {
                normals[v * 3 + 1] = 1f; // fallback
            } else {
                normals[v * 3]     = s[0] / len;
                normals[v * 3 + 1] = s[1] / len;
                normals[v * 3 + 2] = s[2] / len;
            }
        }
        return normals;
    }

    /** Buffer untuk Smooth Shading (indexed, normal vertex rata-rata). */
    public float[] buildSmoothVertexBuffer() {
        float[] normals = computeSmoothNormals();
        float[] out = new float[getVertexCount() * FLOATS_PER_VERTEX];
        for (int v = 0; v < getVertexCount(); v++) {
            int o = v * FLOATS_PER_VERTEX;
            out[o]     = positions[v * 3];
            out[o + 1] = positions[v * 3 + 1];
            out[o + 2] = positions[v * 3 + 2];
            out[o + 3] = normals[v * 3];
            out[o + 4] = normals[v * 3 + 1];
            out[o + 5] = normals[v * 3 + 2];
            out[o + 6] = uvs[v * 2];
            out[o + 7] = uvs[v * 2 + 1];
        }
        return out;
    }

    /** Buffer untuk Flat Shading (non-indexed, satu normal per face). */
    public float[] buildFlatVertexBuffer() {
        float[] out = new float[indices.length * FLOATS_PER_VERTEX];
        int o = 0;
        for (int t = 0; t < indices.length; t += 3) {
            float[] n = faceNormal(indices[t], indices[t + 1], indices[t + 2]);
            if (n == null) {
                n = new float[]{0f, 1f, 0f};
            }
            for (int k = 0; k < 3; k++) {
                int v = indices[t + k];
                out[o++] = positions[v * 3];
                out[o++] = positions[v * 3 + 1];
                out[o++] = positions[v * 3 + 2];
                out[o++] = n[0];
                out[o++] = n[1];
                out[o++] = n[2];
                out[o++] = uvs[v * 2];
                out[o++] = uvs[v * 2 + 1];
            }
        }
        return out;
    }

    public int getFlatVertexCount() {
        return indices.length;
    }
}
