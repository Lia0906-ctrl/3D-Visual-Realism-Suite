package com.grafkom.viewing3d;

import org.joml.Matrix4f;

/**
 * ProjectionTaxonomyManager = pengelola taksonomi proyeksi.
 *
 * Proyeksi Paralel   : Orthographic, Isometric, Oblique Cavalier
 * Proyeksi Perspektif: 1 Titik Hilang, 2 Titik Hilang, 3 Titik Hilang
 */
public class ProjectionTaxonomyManager {

    /** Enam jenis proyeksi. Tiap jenis punya pose kamera standar (azimuth, elevasi). */
    public enum ProjectionType {
        ORTHOGRAPHIC("1. Orthographic", false, 0.0f, 0.0f),
        ISOMETRIC("2. Isometric", false, 45.0f, 35.264f),
        OBLIQUE_CAVALIER("3. Oblique Cavalier", false, 0.0f, 0.0f),
        PERSPECTIVE_1POINT("4. Perspective 1 Point", true, 0.0f, 0.0f),
        PERSPECTIVE_2POINT("5. Perspective 2 Point", true, 45.0f, 0.0f),
        PERSPECTIVE_3POINT("6. Perspective 3 Point", true, 45.0f, 35.264f);

        private final String label;
        private final boolean perspective;
        private final float azimuthDeg;
        private final float elevationDeg;

        ProjectionType(String label, boolean perspective, float azimuthDeg, float elevationDeg) {
            this.label = label;
            this.perspective = perspective;
            this.azimuthDeg = azimuthDeg;
            this.elevationDeg = elevationDeg;
        }

        public String getLabel() { return label; }
        public boolean isPerspective() { return perspective; }
        public float getAzimuthDeg() { return azimuthDeg; }
        public float getElevationDeg() { return elevationDeg; }
    }

    public static final float CAVALIER_ANGLE_DEG = 45.0f;  // sudut garis kedalaman
    public static final float CAVALIER_SCALE = 1.0f;       // 1.0 = Cavalier, 0.5 = Cabinet

    private ProjectionType current = ProjectionType.PERSPECTIVE_3POINT;

    public ProjectionType getCurrent() { return current; }

    public void setCurrent(ProjectionType type) { this.current = type; }

    public boolean isPerspective() { return current.isPerspective(); }

    /** Ciri singkat tiap jenis proyeksi (ditampilkan di panel kontrol). */
    public String getDescription() {
        switch (current) {
            case ORTHOGRAPHIC:
                return "Garis sejajar tetap sejajar, ukuran tidak bergantung jarak";
            case ISOMETRIC:
                return "Tiga sumbu sama pendek, sudut antar sumbu 120 derajat";
            case OBLIQUE_CAVALIER:
                return "Kedalaman miring 45 derajat skala 1:1, sisi sejajar layar tetap asli";
            case PERSPECTIVE_1POINT:
                return "Satu titik hilang: garis kedalaman menyatu di tengah";
            case PERSPECTIVE_2POINT:
                return "Dua titik hilang: garis tegak tetap lurus";
            case PERSPECTIVE_3POINT:
                return "Tiga titik hilang: garis tegak ikut menyatu";
            default:
                return "";
        }
    }

    /**
     * Menghitung matriks proyeksi sesuai jenis yang aktif.
     *
     * @param perspective     parameter perspektif (FOV, aspect, near, far)
     * @param orthoHalfHeight setengah tinggi volume pandang untuk proyeksi paralel
     * @param eyeOffset       geseran mata untuk stereo paralel (0 = mode biasa)
     * @param focalLength     jarak ke objek: bidang zero parallax stereo dan poros shear oblique
     */
    public Matrix4f calculateProjectionMatrix(PerspectiveProjection perspective, float orthoHalfHeight,
                                              float eyeOffset, float focalLength) {
        switch (current) {
            case ORTHOGRAPHIC:
            case ISOMETRIC:
                return buildParallelMatrix(perspective, orthoHalfHeight, 0.0f, 0.0f, 0.0f,
                        eyeOffset, focalLength);

            case OBLIQUE_CAVALIER: {
                float angle = (float) Math.toRadians(CAVALIER_ANGLE_DEG);
                float shearX = -CAVALIER_SCALE * (float) Math.cos(angle);
                float shearY = -CAVALIER_SCALE * (float) Math.sin(angle);
                // Poros shear = bidang tempat objek berada (jarak kamera ke objek)
                return buildParallelMatrix(perspective, orthoHalfHeight, shearX, shearY, focalLength,
                        eyeOffset, focalLength);
            }

            case PERSPECTIVE_1POINT:
            case PERSPECTIVE_2POINT:
            case PERSPECTIVE_3POINT:
            default:
                return perspective.calculateProjectionMatrix();
        }
    }

    /**
     * Matriks proyeksi paralel = Ortho x Shear.
     * Ortho (rumus glOrtho): skala 1/halfWidth, 1/halfHeight, dan pemetaan kedalaman near..far.
     * Shear berporos pada bidang z = -shearPivot (ruang kamera):
     *   x' = x + shearX * (z + shearPivot) ; y' = y + shearY * (z + shearPivot)
     * Dengan poros itu, objek di bidang tersebut tidak bergeser dan hanya kedalamannya yang miring.
     * Untuk stereo, shearX ditambah  -eyeOffset / focalLength  agar bidang zero parallax tetap diam.
     */
    private Matrix4f buildParallelMatrix(PerspectiveProjection perspective, float halfHeight,
                                         float shearX, float shearY, float shearPivot,
                                         float eyeOffset, float focalLength) {
        float halfWidth = halfHeight * perspective.getAspectRatio();
        float near = perspective.getNearPlane();
        float far = perspective.getFarPlane();

        Matrix4f ortho = new Matrix4f();
        ortho.zero();
        ortho.m00(1.0f / halfWidth);
        ortho.m11(1.0f / halfHeight);
        ortho.m22(-2.0f / (far - near));
        ortho.m32(-(far + near) / (far - near));
        ortho.m33(1.0f);

        Matrix4f shear = new Matrix4f();   // matriks identitas
        shear.m20(shearX - eyeOffset / focalLength);
        shear.m21(shearY);
        shear.m30(shearX * shearPivot);    // translasi agar bidang poros tidak bergeser
        shear.m31(shearY * shearPivot);

        return ortho.mul(shear);
    }
}