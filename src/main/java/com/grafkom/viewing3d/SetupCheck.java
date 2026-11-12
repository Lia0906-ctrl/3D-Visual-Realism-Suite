package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * SetupCheck = program tes untuk memastikan LWJGL, JOML, STB, dan OpenGL 3.3 Core siap dipakai.
 * Jendela akan berwarna biru tua, dan informasi GPU dicetak di jendela Output NetBeans.
 */
public class SetupCheck {

    public static void main(String[] args) {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("GLFW gagal diinisialisasi");
        }

        // Minta OpenGL 3.3 Core Profile (wajib untuk GLSL #version 330 core)
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        long window = glfwCreateWindow(800, 600, "SetupCheck - OpenGL 3.3 Core", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Jendela OpenGL 3.3 Core gagal dibuat. Update driver grafis laptop.");
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();

        System.out.println("=== HASIL PENGECEKAN ===");
        System.out.println("OpenGL Version : " + glGetString(GL_VERSION));
        System.out.println("GLSL Version   : " + glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
        System.out.println("GPU / Renderer : " + glGetString(GL_RENDERER));
        System.out.println("Vendor         : " + glGetString(GL_VENDOR));

        // Cek JOML
        Matrix4f m = new Matrix4f().perspective((float) Math.toRadians(45.0), 4.0f / 3.0f, 0.1f, 100.0f);
        System.out.println("JOML           : OK (m00 = " + m.m00() + ")");

        // Cek STB (dipakai untuk memuat PNG/JPG)
        try {
            Class.forName("org.lwjgl.stb.STBImage");
            System.out.println("STBImage       : OK");
        } catch (ClassNotFoundException e) {
            System.out.println("STBImage       : GAGAL (cek pom.xml)");
        }
        System.out.println("Tutup jendela atau tekan ESC untuk keluar.");

        while (!glfwWindowShouldClose(window)) {
            if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, true);
            }
            glClearColor(0.10f, 0.20f, 0.45f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);
            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        glfwDestroyWindow(window);
        glfwTerminate();
    }
}