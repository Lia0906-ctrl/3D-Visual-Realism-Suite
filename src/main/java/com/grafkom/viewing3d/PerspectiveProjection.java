package com.grafkom.viewing3d;

import org.joml.Matrix4f;

/**
 * PerspectiveProjection = proyeksi perspektif (viewing frustum).
 * Parameter: FOV vertikal, Aspect Ratio, Near Plane, Far Plane.
 * Matriks dihitung manual dengan rumus modul (tanpa gluPerspective / perspective() JOML).
 */
public class PerspectiveProjection {

    public static final float MIN_FOV = 10.0f;
    public static final float MAX_FOV = 120.0f;
    public static final float MIN_NEAR = 0.1f;
    public static final float MAX_FAR = 100.0f;
    private static final float MIN_GAP = 0.5f;   // jarak minimal antara near dan far

    private final float defaultFov;
    private final float defaultNear;
    private final float defaultFar;

    private float fovDegree;
    private float aspectRatio;
    private float zNear;
    private float zFar;

    public PerspectiveProjection(float fovDegree, float aspectRatio, float zNear, float zFar) {
        if (aspectRatio <= 0.0f) {
            throw new IllegalArgumentException("Aspect ratio harus > 0");
        }
        if (zNear <= 0.0f || zFar <= zNear) {
            throw new IllegalArgumentException("Syarat: 0 < zNear < zFar");
        }
        this.fovDegree = clamp(fovDegree, MIN_FOV, MAX_FOV);
        this.aspectRatio = aspectRatio;
        this.zNear = zNear;
        this.zFar = zFar;
        this.defaultFov = this.fovDegree;
        this.defaultNear = zNear;
        this.defaultFar = zFar;
    }

    /** Matriks proyeksi perspektif: f = 1 / tan(FOV / 2). */
    public Matrix4f calculateProjectionMatrix() {
        float fovRad = (float) Math.toRadians(fovDegree);
        float f = 1.0f / (float) Math.tan(fovRad / 2.0f);

        Matrix4f proj = new Matrix4f();
        proj.zero();
        proj.m00(f / aspectRatio);
        proj.m11(f);
        proj.m22(-(zFar + zNear) / (zFar - zNear));
        proj.m32(-(2.0f * zFar * zNear) / (zFar - zNear));
        proj.m23(-1.0f);
        proj.m33(0.0f);
        return proj;
    }

    // ======================= PENGATURAN INTERAKTIF =======================

    public void setFovDegree(float fovDegree) {
        this.fovDegree = clamp(fovDegree, MIN_FOV, MAX_FOV);
    }

    public void adjustFov(float deltaDegree) {
        setFovDegree(fovDegree + deltaDegree);
    }

    /** Dipanggil tiap frame dengan lebar/tinggi jendela agar objek tidak terdistorsi. */
    public void setAspectRatio(float aspectRatio) {
        if (aspectRatio > 0.0f) {
            this.aspectRatio = aspectRatio;
        }
    }

    public void setNearPlane(float zNear) {
        this.zNear = clamp(zNear, MIN_NEAR, zFar - MIN_GAP);
    }

    public void setFarPlane(float zFar) {
        this.zFar = clamp(zFar, zNear + MIN_GAP, MAX_FAR);
    }

    public void adjustNear(float delta) {
        setNearPlane(zNear + delta);
    }

    public void adjustFar(float delta) {
        setFarPlane(zFar + delta);
    }

    public void resetToDefault() {
        fovDegree = defaultFov;
        zNear = defaultNear;
        zFar = defaultFar;
    }

    // ======================= INFO FRUSTUM (dipakai juga di Tahap 5) =======================

    /** Setengah tinggi bidang Near: top = near * tan(FOV / 2). */
    public float getFrustumTop() {
        return zNear * (float) Math.tan(Math.toRadians(fovDegree) / 2.0);
    }

    /** Setengah lebar bidang Near: right = top * aspect. */
    public float getFrustumRight() {
        return getFrustumTop() * aspectRatio;
    }

    public float getFovDegree()   { return fovDegree; }
    public float getAspectRatio() { return aspectRatio; }
    public float getNearPlane()   { return zNear; }
    public float getFarPlane()    { return zFar; }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}