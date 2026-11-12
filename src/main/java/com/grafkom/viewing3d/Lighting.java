package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Lighting = satu cahaya directional (seperti matahari): posisi, titik yang disorot, dan warna.
 * Juga menyediakan "Light Space Matrix" = matriks kamera dari sudut pandang lampu
 * (proyeksi ortografik x view lampu) yang dipakai untuk Shadow Mapping.
 */
public class Lighting {

    private static final float SHADOW_AREA = 8.0f;   // setengah lebar area yang tercakup shadow map
    private static final float LIGHT_NEAR = 2.0f;
    private static final float LIGHT_FAR = 18.0f;

    private final Vector3f position = new Vector3f(5.0f, 7.0f, 4.0f);
    private final Vector3f target = new Vector3f(0.0f, 1.0f, 0.0f);
    private final Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);

    /** Kirim arah dan warna cahaya ke shader utama. */
    public void apply(ShaderProgram shader) {
        shader.setVec3("lightDirection", getDirection());
        shader.setVec3("lightColor", color);
    }

    /** Arah dari scene MENUJU lampu (dipakai untuk rumus N dot L). */
    public Vector3f getDirection() {
        return new Vector3f(position).sub(target).normalize();
    }

    /** Light Space Matrix = P_light * V_light (Orthographic Projection untuk Directional Light). */
    public Matrix4f getLightSpaceMatrix() {
        Matrix4f lightProjection = new Matrix4f().ortho(-SHADOW_AREA, SHADOW_AREA,
                -SHADOW_AREA, SHADOW_AREA, LIGHT_NEAR, LIGHT_FAR);
        Matrix4f lightView = new Matrix4f().lookAt(position, target, new Vector3f(0.0f, 1.0f, 0.0f));
        return lightProjection.mul(lightView);
    }

    public Vector3f getPosition() { return new Vector3f(position); }
    public Vector3f getColor()    { return new Vector3f(color); }
}
