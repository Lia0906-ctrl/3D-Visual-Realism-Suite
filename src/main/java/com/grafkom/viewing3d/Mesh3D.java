package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

/**
 * Mesh3D = model 3D yang terdiri dari titik sudut (vertex), sisi (face), dan warna.
 * Semua data bersifat private (enkapsulasi), hanya bisa dibaca lewat method.
 */
public class Mesh3D {

    private final float[][] vertices;   // [jumlah titik][x,y,z]
    private final int[][] faces;        // [jumlah sisi][4 indeks titik]
    private final float[][] faceColors; // [jumlah sisi][r,g,b]

    public Mesh3D(float[][] vertices, int[][] faces, float[][] faceColors) {
        if (faces.length != faceColors.length) {
            throw new IllegalArgumentException("Jumlah sisi dan jumlah warna harus sama");
        }
        this.vertices = vertices;
        this.faces = faces;
        this.faceColors = faceColors;
    }

    /** Membuat kubus berwarna dengan pusat di (0,0,0). size = panjang sisi kubus. */
    public static Mesh3D createColorCube(float size) {
        float h = size / 2.0f;

        float[][] v = {
            {-h, -h,  h},  // 0 depan-kiri-bawah
            { h, -h,  h},  // 1 depan-kanan-bawah
            { h,  h,  h},  // 2 depan-kanan-atas
            {-h,  h,  h},  // 3 depan-kiri-atas
            {-h, -h, -h},  // 4 belakang-kiri-bawah
            { h, -h, -h},  // 5 belakang-kanan-bawah
            { h,  h, -h},  // 6 belakang-kanan-atas
            {-h,  h, -h}   // 7 belakang-kiri-atas
        };

        // Urutan titik tiap sisi berlawanan arah jarum jam jika dilihat dari luar
        int[][] f = {
            {0, 1, 2, 3},  // depan   (+Z)
            {5, 4, 7, 6},  // belakang(-Z)
            {1, 5, 6, 2},  // kanan   (+X)
            {4, 0, 3, 7},  // kiri    (-X)
            {3, 2, 6, 7},  // atas    (+Y)
            {4, 5, 1, 0}   // bawah   (-Y)
        };

        float[][] c = {
            {1.0f, 0.0f, 0.0f},  // depan   : merah
            {0.0f, 0.8f, 0.0f},  // belakang: hijau
            {0.0f, 0.2f, 1.0f},  // kanan   : biru
            {1.0f, 0.9f, 0.0f},  // kiri    : kuning
            {1.0f, 0.0f, 1.0f},  // atas    : magenta
            {0.0f, 0.9f, 0.9f}   // bawah   : cyan
        };

        return new Mesh3D(v, f, c);
    }

    public int getVertexCount() { return vertices.length; }
    public int getFaceCount()   { return faces.length; }

    /** Menggambar mesh ke layar (OpenGL mode langsung). */
    public void draw() {
        // 1) Isi sisi dengan warna. Polygon offset mendorong sisi sedikit ke belakang
        //    supaya garis tepi tidak "berkedip" (z-fighting).
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.0f, 1.0f);
        glBegin(GL_QUADS);
        for (int i = 0; i < faces.length; i++) {
            glColor3f(faceColors[i][0], faceColors[i][1], faceColors[i][2]);
            for (int index : faces[i]) {
                glVertex3f(vertices[index][0], vertices[index][1], vertices[index][2]);
            }
        }
        glEnd();
        glDisable(GL_POLYGON_OFFSET_FILL);

        // 2) Garis tepi hitam supaya bentuk kubus jelas
        glColor3f(0.0f, 0.0f, 0.0f);
        glLineWidth(2.0f);
        for (int[] face : faces) {
            glBegin(GL_LINE_LOOP);
            for (int index : face) {
                glVertex3f(vertices[index][0], vertices[index][1], vertices[index][2]);
            }
            glEnd();
        }
    }
}