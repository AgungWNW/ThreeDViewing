package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Sumber cahaya (diposisikan secara sferis di sekitar origin).
 * Menyediakan Light Space Matrix = P_light * V_light untuk Shadow Mapping 2-Pass
 * (proyeksi ortografik, sesuai Directional Light pada modul).
 */
public class Light {

    private static final float DEFAULT_AZIMUTH = 0.7f;
    private static final float DEFAULT_ELEVATION = 0.9f;
    private static final float DISTANCE = 11f;

    // Frustum ortografik cahaya: harus menutupi seluruh lantai
    private static final float ORTHO_SIZE = 10f;
    private static final float NEAR_PLANE = 1f;
    private static final float FAR_PLANE = 30f;

    private float azimuth = DEFAULT_AZIMUTH;
    private float elevation = DEFAULT_ELEVATION;

    public void reset() {
        azimuth = DEFAULT_AZIMUTH;
        elevation = DEFAULT_ELEVATION;
    }

    public void rotateAzimuth(float delta) {
        azimuth += delta;
    }

    public void rotateElevation(float delta) {
        elevation = Math.max(0.25f, Math.min(1.40f, elevation + delta)); // ~14 s.d. 80 derajat
    }

    public Vector3f getPosition(Vector3f dest) {
        float ce = (float) Math.cos(elevation);
        return dest.set(
                DISTANCE * ce * (float) Math.cos(azimuth),
                DISTANCE * (float) Math.sin(elevation),
                DISTANCE * ce * (float) Math.sin(azimuth));
    }

    public Matrix4f getLightSpaceMatrix(Matrix4f dest) {
        Matrix4f proj = new Matrix4f().ortho(-ORTHO_SIZE, ORTHO_SIZE, -ORTHO_SIZE, ORTHO_SIZE, NEAR_PLANE, FAR_PLANE);
        Matrix4f view = new Matrix4f().lookAt(getPosition(new Vector3f()),
                new Vector3f(0f, 0f, 0f), new Vector3f(0f, 1f, 0f));
        return dest.set(proj).mul(view);
    }
}
