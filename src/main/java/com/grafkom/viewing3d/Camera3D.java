package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Camera3D = kamera sintetis 3D.
 * Parameter utama: Eye (posisi kamera), Target (titik yang dilihat), Up (arah atas).
 * Mendukung dua mode: ORBITAL (mengelilingi objek) dan FPS (bergerak bebas).
 */
public class Camera3D {

    public enum Mode { ORBITAL, FPS }

    private static final float MIN_RADIUS = 2.0f;
    private static final float MAX_RADIUS = 30.0f;
    private static final float MAX_ANGLE = 89.0f;  // batas elevasi/pitch agar kamera tidak terbalik
    private static final float FPS_STEP = 0.5f;    // jarak maju per 1 langkah zoom di mode FPS

    private final Vector3f eye;
    private final Vector3f target;
    private final Vector3f up;

    private final Vector3f initialEye;
    private final Vector3f initialTarget;

    private Mode mode = Mode.ORBITAL;

    // Parameter mode orbital (koordinat bola)
    private float radius;
    private float azimuthDeg;
    private float elevationDeg;

    // Parameter mode FPS (sudut Euler)
    private float yawDeg;
    private float pitchDeg;

    public Camera3D(Vector3f eye, Vector3f target, Vector3f up) {
        this.eye = new Vector3f(eye);
        this.target = new Vector3f(target);
        this.up = new Vector3f(up);
        this.initialEye = new Vector3f(eye);
        this.initialTarget = new Vector3f(target);
        syncOrbitalFromEye();
    }

    // ======================= LOOKAT MATRIX (manual) =======================

    /** Menghitung View Matrix dengan rumus modul: n, u, v lalu isi matriks 4x4. */
    public Matrix4f calculateLookAtMatrix() {
        Vector3f n = new Vector3f();
        eye.sub(target, n).normalize();      // View Plane Normal: n = (E - At) / |E - At|

        Vector3f u = new Vector3f();
        up.cross(n, u).normalize();          // Right Vector: u = (Up x n) / |Up x n|

        Vector3f v = new Vector3f();
        n.cross(u, v);                       // True Up Vector: v = n x u

        Matrix4f viewMatrix = new Matrix4f();
        viewMatrix.m00(u.x).m10(u.y).m20(u.z).m30(-u.dot(eye));
        viewMatrix.m01(v.x).m11(v.y).m21(v.z).m31(-v.dot(eye));
        viewMatrix.m02(n.x).m12(n.y).m22(n.z).m32(-n.dot(eye));
        viewMatrix.m03(0.0f).m13(0.0f).m23(0.0f).m33(1.0f);
        return viewMatrix;
    }

    // ======================= MODE ORBITAL =======================

    /** Posisi eye dihitung dari koordinat bola yang berpusat di target. */
    public void updateOrbitalPosition(float radius, float azimuthDeg, float elevationDeg) {
        this.radius = clamp(radius, MIN_RADIUS, MAX_RADIUS);
        this.azimuthDeg = azimuthDeg % 360.0f;
        this.elevationDeg = clamp(elevationDeg, -MAX_ANGLE, MAX_ANGLE);

        float azRad = (float) Math.toRadians(this.azimuthDeg);
        float elRad = (float) Math.toRadians(this.elevationDeg);

        eye.x = target.x + this.radius * (float) (Math.cos(elRad) * Math.sin(azRad));
        eye.y = target.y + this.radius * (float) Math.sin(elRad);
        eye.z = target.z + this.radius * (float) (Math.cos(elRad) * Math.cos(azRad));
    }

    /** Menghitung radius, azimuth, elevasi dari posisi eye dan target saat ini. */
    private void syncOrbitalFromEye() {
        Vector3f offset = new Vector3f(eye).sub(target);
        float length = offset.length();
        if (length < 1.0e-4f) {
            offset.set(0.0f, 0.0f, MIN_RADIUS);
            length = MIN_RADIUS;
        }
        float az = (float) Math.toDegrees(Math.atan2(offset.x, offset.z));
        float el = (float) Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, offset.y / length))));
        updateOrbitalPosition(length, az, el);
    }

    // ======================= MODE FPS =======================

    private Vector3f fpsDirection() {
        float yaw = (float) Math.toRadians(yawDeg);
        float pitch = (float) Math.toRadians(pitchDeg);
        return new Vector3f(
                (float) (Math.cos(pitch) * Math.sin(yaw)),
                (float) Math.sin(pitch),
                (float) (-Math.cos(pitch) * Math.cos(yaw)));
    }

    private void updateFpsTarget() {
        target.set(eye).add(fpsDirection());
    }

    private void moveFps(float forward, float right, float upAmount) {
        Vector3f dir = fpsDirection();
        Vector3f rightVec = new Vector3f(dir).cross(up).normalize();
        Vector3f upVec = new Vector3f(up).normalize();
        eye.add(new Vector3f(dir).mul(forward));
        eye.add(new Vector3f(rightVec).mul(right));
        eye.add(new Vector3f(upVec).mul(upAmount));
        updateFpsTarget();
    }

    // ======================= KONTROL (dipakai mouse dan keyboard) =======================

    /**
     * Memutar kamera. dxDeg positif = geser ke kanan, dyDeg positif = geser ke bawah
     * (sama seperti gerakan mouse di layar).
     */
    public void rotate(float dxDeg, float dyDeg) {
        if (mode == Mode.ORBITAL) {
            updateOrbitalPosition(radius, azimuthDeg - dxDeg, elevationDeg + dyDeg);
        } else {
            yawDeg += dxDeg;
            pitchDeg = clamp(pitchDeg - dyDeg, -MAX_ANGLE, MAX_ANGLE);
            updateFpsTarget();
        }
    }

    /** Zoom. steps positif = mendekat. Orbital: radius mengecil. FPS: maju. */
    public void zoom(float steps) {
        if (mode == Mode.ORBITAL) {
            updateOrbitalPosition((float) (radius * Math.pow(0.9, steps)), azimuthDeg, elevationDeg);
        } else {
            moveFps(steps * FPS_STEP, 0.0f, 0.0f);
        }
    }

    /** Geser kanan/atas (hanya berlaku di mode FPS). */
    public void strafe(float right, float upAmount) {
        if (mode == Mode.FPS) {
            moveFps(0.0f, right, upAmount);
        }
    }

    /** Ganti mode ORBITAL <-> FPS tanpa lompatan posisi yang mengagetkan. */
    public void toggleMode() {
        if (mode == Mode.ORBITAL) {
            Vector3f dir = new Vector3f(target).sub(eye).normalize();
            yawDeg = (float) Math.toDegrees(Math.atan2(dir.x, -dir.z));
            pitchDeg = clamp((float) Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, dir.y)))),
                    -MAX_ANGLE, MAX_ANGLE);
            mode = Mode.FPS;
            updateFpsTarget();
        } else {
            target.set(initialTarget);
            mode = Mode.ORBITAL;
            syncOrbitalFromEye();
        }
    }
    
        /** Memasang pose orbital (azimuth, elevasi). Jika sedang mode FPS, kembali ke mode orbital. */
    public void setOrbitalPose(float azimuthDeg, float elevationDeg) {
        if (mode == Mode.FPS) {
            target.set(initialTarget);
            mode = Mode.ORBITAL;
            syncOrbitalFromEye();
        }
        updateOrbitalPosition(radius, azimuthDeg, elevationDeg);
    }

    /** Kembali ke posisi awal (mode orbital). */
    public void reset() {
        eye.set(initialEye);
        target.set(initialTarget);
        mode = Mode.ORBITAL;
        syncOrbitalFromEye();
    }

    // ======================= GETTER =======================

    public Mode getMode() { return mode; }
    public Vector3f getEye() { return new Vector3f(eye); }
    public Vector3f getTarget() { return new Vector3f(target); }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}