package com.grafkom.viewing3d;

import static org.lwjgl.glfw.GLFW.*;

/**
 * InputHandler = menerjemahkan keyboard dan mouse menjadi perubahan pada RenderSettings dan Camera3D.
 *
 * Tombol : B = Phong/Blinn-Phong, F = Flat/Smooth, Z = Z-Buffer, C = Back-Face Culling,
 *          T = tekstur on/off, M = filter Nearest/Bilinear-Mipmap,
 *          S = bayangan on/off, [ dan ] = kurangi/tambah Shadow Bias,
 *          SPASI = putar otomatis, V = VSync, R = reset kamera, ESC = keluar.
 * Mouse  : tahan klik kiri lalu geser = putar kamera, scroll = zoom.
 */
public class InputHandler {

    private static final float MOUSE_SENSITIVITY = 0.3f;   // derajat per piksel

    private final WindowManager windowManager;
    private final Camera3D camera;
    private final RenderSettings settings;

    private boolean dragging = false;
    private double lastMouseX;
    private double lastMouseY;

    public InputHandler(WindowManager windowManager, Camera3D camera, RenderSettings settings) {
        this.windowManager = windowManager;
        this.camera = camera;
        this.settings = settings;
        register();
    }

    private void register() {
        long window = windowManager.getHandle();

        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (action != GLFW_PRESS) {
                return;
            }
            switch (key) {
                case GLFW_KEY_B:
                    settings.toggleBlinnPhong();
                    break;
                case GLFW_KEY_F:
                    settings.toggleFlatShading();
                    break;
                case GLFW_KEY_Z:
                    settings.toggleDepthTest();
                    break;
                case GLFW_KEY_C:
                    settings.toggleBackFaceCulling();
                    break;
                case GLFW_KEY_T:
                    settings.toggleTexture();
                    break;
                case GLFW_KEY_M:
                    settings.toggleFilter();
                    break;
                case GLFW_KEY_S:
                    settings.toggleShadows();
                    break;
                case GLFW_KEY_LEFT_BRACKET:
                    settings.decreaseShadowBias();
                    break;
                case GLFW_KEY_RIGHT_BRACKET:
                    settings.increaseShadowBias();
                    break;
                case GLFW_KEY_SPACE:
                    settings.toggleAutoRotate();
                    break;
                case GLFW_KEY_V:
                    settings.toggleVsync();
                    windowManager.setVSync(settings.isVsync());
                    break;
                case GLFW_KEY_R:
                    camera.reset();
                    break;
                case GLFW_KEY_ESCAPE:
                    windowManager.requestClose();
                    break;
                default:
                    break;
            }
        });

        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = (action == GLFW_PRESS);
            }
        });

        glfwSetCursorPosCallback(window, (w, x, y) -> {
            if (dragging) {
                camera.rotate((float) (x - lastMouseX) * MOUSE_SENSITIVITY,
                              (float) (y - lastMouseY) * MOUSE_SENSITIVITY);
            }
            lastMouseX = x;
            lastMouseY = y;
        });

        glfwSetScrollCallback(window, (w, xOffset, yOffset) -> camera.zoom((float) yOffset));
    }
}
