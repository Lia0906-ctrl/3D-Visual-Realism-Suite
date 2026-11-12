package com.grafkom.viewing3d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ObjModelLoader = membaca file model .obj lalu menyiapkan data siap-gambar:
 * posisi, normal per-face (Flat), normal rata-rata per-vertex (Smooth), dan koordinat UV.
 * Kelas ini murni Java (tidak memakai OpenGL), jadi mudah diuji.
 */
public class ObjModelLoader {

    /** Hasil pembacaan model. Semua array "tidak ber-index": tiap 3 titik = 1 segitiga. */
    public static class MeshData {
        public final float[] positions;      // x, y, z
        public final float[] smoothNormals;  // normal rata-rata vertex (Smooth Shading)
        public final float[] flatNormals;    // normal per-face (Flat Shading)
        public final float[] uvs;            // u, v
        public final int vertexCount;

        MeshData(float[] positions, float[] smoothNormals, float[] flatNormals, float[] uvs) {
            this.positions = positions;
            this.smoothNormals = smoothNormals;
            this.flatNormals = flatNormals;
            this.uvs = uvs;
            this.vertexCount = positions.length / 3;
        }
    }

    // Titik dianggap sama jika posisinya sama sampai 4 angka di belakang koma
    private static final float WELD_PRECISION = 10000.0f;
    // Tekstur diulang beberapa kali mengelilingi teapot agar tidak tampak melar
    private static final float UV_REPEAT_AROUND = 3.0f;

    /**
     * @param resourcePath lokasi file di folder resources, contoh "/models/teapot.obj"
     * @param targetHeight tinggi model setelah diskalakan (alas model berada di y = 0)
     */
    public static MeshData load(String resourcePath, float targetHeight) {
        List<float[]> verts = new ArrayList<>();
        List<int[]> faces = new ArrayList<>();
        readObj(resourcePath, verts, faces);

        // 1) Skala dan letakkan alas model di y = 0 (sumbu x dan z tidak digeser)
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (float[] v : verts) {
            minY = Math.min(minY, v[1]);
            maxY = Math.max(maxY, v[1]);
        }
        float scale = targetHeight / (maxY - minY);
        for (float[] v : verts) {
            v[0] *= scale;
            v[1] = (v[1] - minY) * scale;
            v[2] *= scale;
        }

        // 2) Gabungkan titik kembar (posisi sama) supaya normal rata-rata bisa dihitung
        int[] weldId = new int[verts.size()];
        Map<String, Integer> uniqueIds = new HashMap<>();
        for (int i = 0; i < verts.size(); i++) {
            float[] v = verts.get(i);
            String key = Math.round(v[0] * WELD_PRECISION) + "," + Math.round(v[1] * WELD_PRECISION)
                    + "," + Math.round(v[2] * WELD_PRECISION);
            Integer id = uniqueIds.get(key);
            if (id == null) {
                id = uniqueIds.size();
                uniqueIds.put(key, id);
            }
            weldId[i] = id;
        }

        // 3) Normal tiap face (a x b), lalu jumlahkan ke tiap titik gabungan
        float[][] faceNormals = new float[faces.size()][];
        float[][] sums = new float[uniqueIds.size()][3];
        for (int f = 0; f < faces.size(); f++) {
            int[] face = faces.get(f);
            float[] n = faceNormal(verts.get(face[0]), verts.get(face[1]), verts.get(face[2]));
            faceNormals[f] = n;
            if (n != null) {
                for (int idx : face) {
                    float[] s = sums[weldId[idx]];
                    s[0] += n[0];
                    s[1] += n[1];
                    s[2] += n[2];
                }
            }
        }
        // N_vertex = (jumlah N_face) / panjangnya
        for (float[] s : sums) {
            normalize(s);
        }

        // 4) Susun array siap-gambar
        int vertexCount = faces.size() * 3;
        float[] pos = new float[vertexCount * 3];
        float[] smooth = new float[vertexCount * 3];
        float[] flat = new float[vertexCount * 3];
        float[] uv = new float[vertexCount * 2];

        for (int f = 0; f < faces.size(); f++) {
            int[] face = faces.get(f);
            float[] fn = faceNormals[f] != null ? faceNormals[f] : new float[]{0.0f, 1.0f, 0.0f};

            float[] u = new float[3];
            for (int k = 0; k < 3; k++) {
                float[] v = verts.get(face[k]);
                u[k] = (float) (Math.atan2(v[2], v[0]) / (2.0 * Math.PI)) + 0.5f;
            }
            // Segitiga yang melewati "sambungan" (u = 0 dan u = 1): naikkan u kecil agar tidak melintir
            float uMin = Math.min(u[0], Math.min(u[1], u[2]));
            float uMax = Math.max(u[0], Math.max(u[1], u[2]));
            if (uMax - uMin > 0.5f) {
                for (int k = 0; k < 3; k++) {
                    if (u[k] < 0.5f) {
                        u[k] += 1.0f;
                    }
                }
            }

            for (int k = 0; k < 3; k++) {
                int o = f * 3 + k;
                float[] v = verts.get(face[k]);
                float[] sn = sums[weldId[face[k]]];

                pos[o * 3] = v[0];
                pos[o * 3 + 1] = v[1];
                pos[o * 3 + 2] = v[2];

                smooth[o * 3] = sn[0];
                smooth[o * 3 + 1] = sn[1];
                smooth[o * 3 + 2] = sn[2];

                flat[o * 3] = fn[0];
                flat[o * 3 + 1] = fn[1];
                flat[o * 3 + 2] = fn[2];

                uv[o * 2] = u[k] * UV_REPEAT_AROUND;          // pemetaan silinder
                uv[o * 2 + 1] = v[1] / targetHeight;
            }
        }
        return new MeshData(pos, smooth, flat, uv);
    }

    /** Lantai datar (2 segitiga, menghadap ke atas) berpusat di (0, 0, 0). */
    public static MeshData createFloor(float halfSize) {
        float h = halfSize;
        float[] pos = {
            -h, 0, -h,   -h, 0,  h,    h, 0,  h,    // segitiga 1 (berlawanan jarum jam dari atas)
            -h, 0, -h,    h, 0,  h,    h, 0, -h     // segitiga 2
        };
        float[] normals = new float[18];
        for (int i = 0; i < 6; i++) {
            normals[i * 3 + 1] = 1.0f;                // normal lantai = ke atas (0, 1, 0)
        }
        float[] uv = {0, 0,  0, 1,  1, 1,   0, 0,  1, 1,  1, 0};
        return new MeshData(pos, normals, normals.clone(), uv);
    }

    // ---------------------------------------------------------------------

    private static void readObj(String path, List<float[]> verts, List<int[]> faces) {
        try (InputStream in = ObjModelLoader.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("File model tidak ditemukan: " + path
                        + " (cek folder src/main/resources lalu Clean and Build)");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                String[] p = line.split("\\s+");
                if (line.startsWith("v ")) {
                    verts.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
                } else if (line.startsWith("f ")) {
                    int[] idx = new int[p.length - 1];
                    for (int i = 1; i < p.length; i++) {
                        idx[i - 1] = Integer.parseInt(p[i].split("/")[0]) - 1;   // OBJ mulai dari 1
                    }
                    for (int i = 1; i < idx.length - 1; i++) {                   // poligon -> segitiga
                        faces.add(new int[]{idx[0], idx[i], idx[i + 1]});
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca model: " + path, e);
        }
    }

    /** Normal satu segitiga (arah luar jika urutan titik berlawanan jarum jam). null jika luasnya nol. */
    private static float[] faceNormal(float[] a, float[] b, float[] c) {
        float e1x = b[0] - a[0], e1y = b[1] - a[1], e1z = b[2] - a[2];
        float e2x = c[0] - a[0], e2y = c[1] - a[1], e2z = c[2] - a[2];
        float[] n = {
            e1y * e2z - e1z * e2y,
            e1z * e2x - e1x * e2z,
            e1x * e2y - e1y * e2x
        };
        float len = (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        if (len < 1.0e-9f) {
            return null;
        }
        n[0] /= len;
        n[1] /= len;
        n[2] /= len;
        return n;
    }

    private static void normalize(float[] v) {
        float len = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (len < 1.0e-9f) {
            v[0] = 0.0f;
            v[1] = 1.0f;
            v[2] = 0.0f;
        } else {
            v[0] /= len;
            v[1] /= len;
            v[2] /= len;
        }
    }
}
