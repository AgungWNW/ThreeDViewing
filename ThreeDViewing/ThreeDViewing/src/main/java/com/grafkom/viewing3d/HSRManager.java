package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

/**
 * Praktikum 2 - Hidden Surface Removal (Indikator 3).
 * Mengonfigurasi Z-Buffer Depth Test dan Back-Face Culling pada state OpenGL.
 */
public class HSRManager {

    /** Z-Buffer: fragment lolos jika z_new < Z_Buffer[x,y]. */
    public static void setupDepthTest(boolean enable) {
        if (enable) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS); // Pixel lebih dekat lolos uji depth
        } else {
            glDisable(GL_DEPTH_TEST);
        }
    }

    /** Back-Face Culling: polygon CW (membelakangi kamera) dibuang sebelum rasterisasi. */
    public static void setupBackFaceCulling(boolean enable) {
        if (enable) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);   // Buang polygon belakang
            glFrontFace(GL_CCW);   // Counter-Clockwise adalah tampak depan
        } else {
            glDisable(GL_CULL_FACE);
        }
    }

    /** Mode wireframe untuk mengamati efek culling pada garis-garis polygon. */
    public static void setupWireframe(boolean enable) {
        glPolygonMode(GL_FRONT_AND_BACK, enable ? GL_LINE : GL_FILL);
    }
}
