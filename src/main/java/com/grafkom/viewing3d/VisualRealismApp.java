package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.swing.SwingUtilities;
import java.util.Locale;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL30.*;

/**
 * VisualRealismApp = kelas utama "3D Visual Realism Suite".
 * Menyatukan semua bagian: jendela, kamera, shader, model, cahaya, tekstur, bayangan, input,
 * dan panel informasi (InfoPanel).
 *
 * Tiap frame digambar dengan 2 pass:
 *   Pass 1 (Depth Pass)    : teapot digambar dari sudut pandang LAMPU ke Depth FBO (shadow map).
 *   Pass 2 (Lighting Pass) : lantai dan teapot digambar dari sudut pandang KAMERA, shader
 *                            membandingkan kedalaman dengan shadow map untuk menentukan bayangan.
 */
public class VisualRealismApp {

    private static final int WINDOW_WIDTH = 1100;
    private static final int WINDOW_HEIGHT = 700;
    private static final float MODEL_HEIGHT = 2.0f;
    private static final float FLOOR_HALF_SIZE = 6.0f;
    private static final float ROTATE_SPEED = 30.0f;   // derajat per detik

    private WindowManager window;
    private Camera3D camera;
    private PerspectiveProjection projection;
    private ShaderProgram shader;
    private ShaderProgram shadowShader;
    private ShadowMapFBO shadowMap;
    private Mesh teapot;
    private Mesh floor;
    private Texture2D woodTexture;
    private Lighting lighting;
    private RenderSettings settings;
    private InputHandler input;
    private volatile InfoPanel infoPanel;   // dibuat di thread Swing

    private float angle = 0.0f;

    public static void main(String[] args) {
        new VisualRealismApp().run();
        System.exit(0);   // pastikan panel informasi ikut tertutup
    }

    private void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        window = new WindowManager(WINDOW_WIDTH, WINDOW_HEIGHT, "3D Visual Realism Suite");

        // Kamera dan proyeksi dipakai ulang dari Minggu 10
        camera = new Camera3D(new Vector3f(0.0f, 3.0f, 8.0f),
                              new Vector3f(0.0f, 1.0f, 0.0f),
                              new Vector3f(0.0f, 1.0f, 0.0f));
        projection = new PerspectiveProjection(45.0f, (float) WINDOW_WIDTH / WINDOW_HEIGHT, 0.1f, 100.0f);

        settings = new RenderSettings();
        lighting = new Lighting();
        input = new InputHandler(window, camera, settings);

        shader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        shadowShader = new ShaderProgram("/shaders/shadow_depth.vert", "/shaders/shadow_depth.frag");
        shadowMap = new ShadowMapFBO();

        teapot = new Mesh(ObjModelLoader.load("/models/teapot.obj", MODEL_HEIGHT));
        floor = new Mesh(ObjModelLoader.createFloor(FLOOR_HALF_SIZE));
        woodTexture = new Texture2D("/textures/wood.png");

        glClearColor(0.10f, 0.10f, 0.14f, 1.0f);

        openInfoPanel();

        System.out.println("OpenGL " + glGetString(GL_VERSION) + " | " + glGetString(GL_RENDERER));
        System.out.println("Tekstur: " + woodTexture.getWidth() + " x " + woodTexture.getHeight());
        System.out.println("Teapot: " + teapot.getTriangleCount() + " segitiga");
        System.out.println("Shadow map: " + ShadowMapFBO.SHADOW_WIDTH + " x " + ShadowMapFBO.SHADOW_HEIGHT);
    }

    private void openInfoPanel() {
        SwingUtilities.invokeLater(() -> {
            try {
                infoPanel = new InfoPanel();
            } catch (Throwable t) {
                System.err.println("Panel informasi tidak bisa dibuka: " + t);
            }
        });
    }

    private void loop() {
        double lastTime = glfwGetTime();
        double fpsTimer = lastTime;
        int frames = 0;

        while (!window.shouldClose()) {
            double now = glfwGetTime();
            float dt = (float) (now - lastTime);
            lastTime = now;

            if (settings.isAutoRotate()) {
                angle += ROTATE_SPEED * dt;
            }

            render();
            window.swapAndPoll();

            // Hitung FPS, perbarui judul jendela dua kali per detik
            frames++;
            if (now - fpsTimer >= 0.5) {
                double fps = frames / (now - fpsTimer);
                window.setTitle(String.format(Locale.US, "3D Visual Realism Suite | %s | FPS: %.0f",
                        settings.describe(), fps));
                InfoPanel panel = infoPanel;
                if (panel != null) {
                    panel.update(settings, fps, teapot.getTriangleCount());
                }
                frames = 0;
                fpsTimer = now;
            }
        }
    }

    private void render() {
        window.updateViewport();
        if (window.getWidth() == 0 || window.getHeight() == 0) {
            return;   // jendela di-minimize
        }
        projection.setAspectRatio((float) window.getWidth() / window.getHeight());

        Matrix4f teapotModel = new Matrix4f().rotateY((float) Math.toRadians(angle));
        Matrix4f lightSpaceMatrix = lighting.getLightSpaceMatrix();

        // ---------- PASS 1: Depth Pass (dari sudut pandang lampu) ----------
        if (settings.isShadows()) {
            renderDepthPass(lightSpaceMatrix, teapotModel);
        }

        // ---------- PASS 2: Lighting Pass (dari sudut pandang kamera) ----------
        // Hidden Surface Removal mengikuti tombol Z dan C
        HSRManager.setupDepthTest(settings.isDepthTest());
        HSRManager.setupBackFaceCulling(settings.isBackFaceCulling());

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        shader.use();
        shader.setMat4("view", camera.calculateLookAtMatrix());
        shader.setMat4("projection", projection.calculateProjectionMatrix());
        shader.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        shader.setVec3("viewPos", camera.getEye());
        shader.setBool("useBlinnPhong", settings.isBlinnPhong());
        shader.setBool("useShadow", settings.isShadows());
        shader.setFloat("shadowBias", settings.getShadowBias());
        lighting.apply(shader);

        // Texture unit 0 = tekstur kayu, texture unit 1 = shadow map
        woodTexture.setFilter(settings.isBilinearMipmap());
        woodTexture.bind(0);
        shadowMap.bindDepthTexture(1);
        shader.setInt("textureSampler", 0);
        shader.setInt("shadowMap", 1);

        // Lantai: warna polos abu-abu terang, kilau lemah
        shader.setMat4("model", new Matrix4f());
        shader.setBool("useTexture", false);
        shader.setVec3("objectColor", new Vector3f(0.72f, 0.74f, 0.78f));
        shader.setFloat("specularStrength", 0.15f);
        floor.draw(false);

        // Teapot
        shader.setMat4("model", teapotModel);
        shader.setBool("useTexture", settings.isUseTexture());
        shader.setVec3("objectColor", new Vector3f(0.80f, 0.35f, 0.20f));
        shader.setFloat("specularStrength", 0.8f);
        teapot.draw(settings.isFlatShading());
    }

    /** Pass 1: gambar teapot dari sudut pandang lampu, hanya depth yang disimpan ke Depth FBO. */
    private void renderDepthPass(Matrix4f lightSpaceMatrix, Matrix4f teapotModel) {
        shadowMap.bindForWriting();
        glEnable(GL_DEPTH_TEST);   // pass ini selalu butuh depth test, apa pun pengaturan tombol Z
        glDepthFunc(GL_LESS);
        glDisable(GL_CULL_FACE);   // teapot berongga: semua sisi ikut menghasilkan bayangan

        shadowShader.use();
        shadowShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        shadowShader.setMat4("model", teapotModel);
        teapot.draw(false);

        shadowMap.unbind(window.getWidth(), window.getHeight());
    }

    private void cleanup() {
        teapot.cleanup();
        floor.cleanup();
        woodTexture.cleanup();
        shadowMap.cleanup();
        shader.cleanup();
        shadowShader.cleanup();
        window.destroy();
        SwingUtilities.invokeLater(() -> {
            if (infoPanel != null) {
                infoPanel.dispose();
            }
        });
    }
}
