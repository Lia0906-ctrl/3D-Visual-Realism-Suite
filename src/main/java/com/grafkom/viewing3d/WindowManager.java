package com.grafkom.viewing3d;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * WindowManager = membuat jendela GLFW dengan konteks OpenGL 3.3 Core,
 * mengatur VSync, judul, ukuran, dan menukar buffer tiap frame.
 */
public class WindowManager {

    private final long window;
    private final int[] fbWidth = new int[1];
    private final int[] fbHeight = new int[1];

    public WindowManager(int width, int height, String title) {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW gagal diinisialisasi");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(width, height, title, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Jendela OpenGL 3.3 Core gagal dibuat. Update driver grafis.");
        }
        glfwSetWindowPos(window, 100, 60);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();
    }

    public long getHandle() { return window; }

    public boolean shouldClose() { return glfwWindowShouldClose(window); }

    public void requestClose() { glfwSetWindowShouldClose(window, true); }

    /** Tukar buffer (tampilkan gambar) lalu proses event keyboard/mouse. */
    public void swapAndPoll() {
        glfwSwapBuffers(window);
        glfwPollEvents();
    }

    public void setTitle(String title) { glfwSetWindowTitle(window, title); }

    public void setVSync(boolean on) { glfwSwapInterval(on ? 1 : 0); }

    /** Membaca ukuran area gambar saat ini dan menyesuaikan viewport. */
    public void updateViewport() {
        glfwGetFramebufferSize(window, fbWidth, fbHeight);
        glViewport(0, 0, fbWidth[0], fbHeight[0]);
    }

    public int getWidth()  { return fbWidth[0]; }
    public int getHeight() { return fbHeight[0]; }

    public void destroy() {
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }
}
