package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Kamera orbit (drag mouse kiri = putar, scroll = zoom) yang mengelilingi titik target. */
public class OrbitCamera {

    private static final float DEFAULT_YAW = 0.6f;
    private static final float DEFAULT_PITCH = 0.45f;
    private static final float DEFAULT_DISTANCE = 11f;

    private float yaw = DEFAULT_YAW;
    private float pitch = DEFAULT_PITCH;
    private float distance = DEFAULT_DISTANCE;
    private final Vector3f target = new Vector3f(0f, 0.8f, 0f);

    public void reset() {
        yaw = DEFAULT_YAW;
        pitch = DEFAULT_PITCH;
        distance = DEFAULT_DISTANCE;
    }

    public void orbit(float dx, float dy) {
        yaw -= dx * 0.006f;
        pitch += dy * 0.006f;
        pitch = Math.max(0.05f, Math.min(1.5f, pitch)); // tidak boleh di bawah lantai
    }

    public void zoom(float scroll) {
        distance -= scroll * 0.8f;
        distance = Math.max(3f, Math.min(30f, distance));
    }

    public Vector3f getPosition(Vector3f dest) {
        float cp = (float) Math.cos(pitch);
        return dest.set(
                target.x + distance * cp * (float) Math.sin(yaw),
                target.y + distance * (float) Math.sin(pitch),
                target.z + distance * cp * (float) Math.cos(yaw));
    }

    public Matrix4f getViewMatrix(Matrix4f dest) {
        Vector3f eye = getPosition(new Vector3f());
        return dest.identity().lookAt(eye, target, new Vector3f(0f, 1f, 0f));
    }
}
