package com.grafkom.viewing3d;

import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

/**
 * StereoAnaglyphRenderer = render stereoskopik Anaglyph Red-Cyan.
 * - Mata kiri  : hanya saluran Merah (Red).
 * - Mata kanan : hanya saluran Hijau + Biru (Cyan).
 * - Perspektif : Off-Axis Asymmetric Frustum (frustum ikut digeser sesuai IPD).
 */
public class StereoAnaglyphRenderer {

    /** Pembuat matriks proyeksi untuk satu mata (eyeOffset = -IPD/2 kiri, +IPD/2 kanan). */
    public interface EyeProjectionProvider {
        Matrix4f projectionForEye(float eyeOffset);
    }

    public static final float MIN_IPD = 0.0f;
    public static final float MAX_IPD = 1.0f;
    private static final float MIN_FOCAL = 0.5f;

    private float ipd = 0.2f;           // Interpupillary Distance (satuan dunia)
    private float focalLength = 6.0f;   // jarak ke bidang zero parallax

    private final float[] matrixBuffer = new float[16];

    public void renderAnaglyph(Matrix4f viewMatrix, EyeProjectionProvider provider,
                               Runnable renderSceneCallback) {
        // Bersihkan seluruh layar (semua saluran warna harus aktif agar glClear berlaku)
        glColorMask(true, true, true, true);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // PASS 1: Mata kiri -> hanya saluran Merah
        glColorMask(true, false, false, true);
        setupEye(-ipd / 2.0f, viewMatrix, provider);
        renderSceneCallback.run();

        // PASS 2: Mata kanan -> hanya saluran Hijau + Biru (Cyan)
        glColorMask(false, true, true, true);
        glClear(GL_DEPTH_BUFFER_BIT);   // depth dibersihkan, warna pass 1 tetap tersimpan
        setupEye(ipd / 2.0f, viewMatrix, provider);
        renderSceneCallback.run();

        // Kembalikan color mask ke normal
        glColorMask(true, true, true, true);
    }

    private void setupEye(float eyeOffset, Matrix4f viewMatrix, EyeProjectionProvider provider) {
        glMatrixMode(GL_PROJECTION);
        glLoadMatrixf(provider.projectionForEye(eyeOffset).get(matrixBuffer));

        // Kamera bergeser sejauh eyeOffset ke kanan = dunia bergeser -eyeOffset di ruang kamera
        Matrix4f eyeView = new Matrix4f().translate(-eyeOffset, 0.0f, 0.0f).mul(viewMatrix);
        glMatrixMode(GL_MODELVIEW);
        glLoadMatrixf(eyeView.get(matrixBuffer));
    }

    /**
     * Matriks proyeksi perspektif Off-Axis (asymmetric frustum), dihitung manual seperti glFrustum.
     * Tepi kiri/kanan bidang Near digeser sebesar  -eyeOffset * near / focalLength.
     */
    public Matrix4f calculateOffAxisProjection(float eyeOffset, PerspectiveProjection projection) {
        float near = projection.getNearPlane();
        float far = projection.getFarPlane();
        float top = projection.getFrustumTop();
        float bottom = -top;
        float right = projection.getFrustumRight();

        float shift = -eyeOffset * near / focalLength;
        float left = -right + shift;
        float rightEdge = right + shift;

        Matrix4f m = new Matrix4f();
        m.zero();
        m.m00(2.0f * near / (rightEdge - left));
        m.m11(2.0f * near / (top - bottom));
        m.m20((rightEdge + left) / (rightEdge - left));
        m.m21((top + bottom) / (top - bottom));
        m.m22(-(far + near) / (far - near));
        m.m32(-(2.0f * far * near) / (far - near));
        m.m23(-1.0f);
        return m;
    }

    // ======================= PENGATURAN =======================

    public void setIpd(float ipd) {
        this.ipd = Math.max(MIN_IPD, Math.min(MAX_IPD, ipd));
    }

    public void adjustIpd(float delta) {
        setIpd(ipd + delta);
    }

    public void setFocalLength(float focalLength) {
        this.focalLength = Math.max(MIN_FOCAL, focalLength);
    }

    public float getIpd() { return ipd; }
    public float getFocalLength() { return focalLength; }
}