# 3D Visual Realism Suite

**Project Praktikum Grafika Komputer – Minggu 11**

## 📌 Deskripsi Project

3D Visual Realism Suite merupakan project praktikum mata kuliah Grafika Komputer yang dikembangkan menggunakan bahasa pemrograman Java dengan dukungan library LWJGL (Lightweight Java Game Library) dan OpenGL.

Project ini dibuat untuk mempelajari dan menerapkan konsep grafika komputer, khususnya dalam pengembangan visualisasi grafis 3D serta penerapan teknik untuk menghasilkan tampilan visual yang lebih realistis.

## 🎯 Tujuan

1. Memahami konsep dasar grafika komputer 3D.
2. Mempelajari penggunaan OpenGL untuk proses rendering grafis.
3. Mengimplementasikan LWJGL sebagai penghubung antara Java dan OpenGL.
4. Mengembangkan kemampuan pemrograman grafis menggunakan Java.

## 🛠️ Teknologi yang Digunakan

- **Java** – Bahasa pemrograman utama.
- **LWJGL (Lightweight Java Game Library)** – Library untuk mendukung pengembangan grafis dan interaksi dengan OpenGL.
- **OpenGL** – API grafis untuk proses rendering objek 3D.
- **Apache Maven** – Manajemen dependensi dan konfigurasi project.
- **NetBeans IDE** – Lingkungan pengembangan aplikasi.

## 📂 Struktur Project

```text
VisualRealismSuite/
├── pom.xml
└── src/main/
    ├── java/com/grafkom/viewing3d/
    │   ├── VisualRealismApp.java      (kelas utama)
    │   ├── WindowManager.java
    │   ├── InputHandler.java
    │   ├── RenderSettings.java
    │   ├── Camera3D.java              (dari Minggu 10)
    │   ├── PerspectiveProjection.java (dari Minggu 10)
    │   ├── Lighting.java
    │   ├── ShaderProgram.java
    │   ├── ObjModelLoader.java
    │   ├── Mesh.java
    │   ├── Texture2D.java
    │   ├── HSRManager.java
    │   ├── ShadowMapFBO.java
    │   └── InfoPanel.java
    └── resources/
        ├── models/teapot.obj
        ├── textures/wood.png
        └── shaders/
            ├── phong.vert
            ├── phong.frag
            ├── shadow_depth.vert
            └── shadow_depth.frag
```

## ⚙️ Persyaratan Sistem

- Java Development Kit (JDK)
- NetBeans IDE atau IDE Java lainnya
- Apache Maven
- Library LWJGL dan OpenGL sesuai konfigurasi pada `pom.xml`

## 🚀 Cara Menjalankan Project

1. Clone repository ini ke komputer:

   ```bash
   git clone https://github.com/Lia0906-ctrl/3D-Visual-Realism-Suite.git
   ```

2. Buka project menggunakan NetBeans IDE.
3. Pastikan Maven berhasil memuat seluruh dependensi yang dibutuhkan.
4. Periksa konfigurasi Java dan library LWJGL.
5. Jalankan class utama (*Main Class*) project.

## 👩‍💻 Pengembang

Project ini dikembangkan sebagai bagian dari tugas praktikum mata kuliah Grafika Komputer.

**Nama:** Bunga Aulia Putri Maharani
**Kelas:** 2025D
**NIM:** 25050974122

---

*Project Praktikum Grafika Komputer – Minggu 11*  
*Java | LWJGL | OpenGL*
