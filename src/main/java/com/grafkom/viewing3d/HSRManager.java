package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

/**
 * HSRManager = mengatur Hidden Surface Removal (membuang bagian yang tidak terlihat).
 *  - Z-Buffer (Depth Test)  : benda yang lebih dekat ke kamera menutupi yang lebih jauh.
 *  - Back-Face Culling      : sisi belakang segitiga (membelakangi kamera) tidak digambar.
 */
public class HSRManager {

    public static void setupDepthTest(boolean enable) {
        if (enable) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);   // pixel yang lebih dekat lolos uji depth
        } else {
            glDisable(GL_DEPTH_TEST);
        }
    }

    public static void setupBackFaceCulling(boolean enable) {
        if (enable) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);    // buang sisi belakang
            glFrontFace(GL_CCW);    // urutan titik berlawanan jarum jam = sisi depan
        } else {
            glDisable(GL_CULL_FACE);
        }
    }
}
