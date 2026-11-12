package com.grafkom.viewing3d;

import com.grafkom.viewing3d.ProjectionTaxonomyManager.ProjectionType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 3D Viewing Interactive Visualizer (TAHAP 7 - integrasi).
 * Color Cube + Camera3D (orbital/FPS) + PerspectiveProjection + Stereo Anaglyph
 * + ProjectionTaxonomyManager (6 proyeksi) + ControlPanel (menu dan petunjuk).
 */
public class ThreeDViewing {

    private static final float MOUSE_SENSITIVITY = 0.3f;   // derajat per piksel
    private static final float KEY_ROTATE_SPEED = 90.0f;   // derajat per detik
    private static final float KEY_ZOOM_SPEED = 6.0f;      // langkah zoom per detik
    private static final float KEY_MOVE_SPEED = 3.0f;      // satuan jarak per detik (FPS)
    private static final float FOV_SPEED = 40.0f;          // derajat per detik
    private static final float NEAR_SPEED = 2.0f;          // satuan per detik
    private static final float FAR_SPEED = 20.0f;          // satuan per detik
    private static final float IPD_SPEED = 0.3f;           // satuan per detik

    private static final float DEFAULT_VIEW_DISTANCE = 6.0f;
    // Skala proyeksi paralel agar ukuran kubus sama dengan perspektif FOV 45 di titik fokus
    private static final float ORTHO_SCALE = (float) Math.tan(Math.toRadians(22.5));

    private static final int WINDOW_WIDTH = 880;
    private static final int WINDOW_HEIGHT = 600;

    private long window;
    private Mesh3D cube;
    private Camera3D camera;
    private PerspectiveProjection projection;
    private StereoAnaglyphRenderer stereo;
    private ProjectionTaxonomyManager taxonomy;

    private boolean stereoEnabled = false;
    private boolean autoRotate = false;
    private boolean titleDirty = true;
    private float angle = 0.0f;
    private float orthoHalfHeight = 3.0f;
    private double lastTime;

    private boolean dragging = false;
    private double lastMouseX;
    private double lastMouseY;

    private final int[] fbWidth = new int[1];
    private final int[] fbHeight = new int[1];
    private final float[] matrixBuffer = new float[16];

    // Komunikasi dengan ControlPanel (thread Swing) --------------------------
    private final Queue<Runnable> pendingActions = new ConcurrentLinkedQueue<>();
    private volatile String statusText = "";
    private volatile ProjectionType shownType = ProjectionType.PERSPECTIVE_3POINT;
    private volatile boolean shownStereo = false;
    private volatile boolean shownAutoRotate = false;
    private volatile float shownIpd = 0.2f;

    public static void main(String[] args) {
        new ThreeDViewing().run();
        System.exit(0);   // pastikan panel kontrol ikut tertutup
    }

    private void run() {
        init();
        loop();
        cleanup();
    }

    // ======================= INISIALISASI =======================

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("GLFW gagal diinisialisasi");
        }

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "ThreeDViewing", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Jendela gagal dibuat (cek driver grafis)");
        }
        glfwSetWindowPos(window, 10, 40);

        cube = Mesh3D.createColorCube(2.0f);
        camera = new Camera3D(new Vector3f(0.0f, 2.0f, 6.0f),
                              new Vector3f(0.0f, 0.0f, 0.0f),
                              new Vector3f(0.0f, 1.0f, 0.0f));
        projection = new PerspectiveProjection(45.0f, (float) WINDOW_WIDTH / WINDOW_HEIGHT, 0.1f, 100.0f);
        stereo = new StereoAnaglyphRenderer();
        taxonomy = new ProjectionTaxonomyManager();
        selectProjection(taxonomy.getCurrent());

        setupInput();

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();
        glfwShowWindow(window);

        glEnable(GL_DEPTH_TEST);
        glClearColor(0.10f, 0.10f, 0.14f, 1.0f);

        openControlPanel();
        lastTime = glfwGetTime();
    }

    private void openControlPanel() {
        SwingUtilities.invokeLater(() -> {
            try {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // tampilan bawaan Java dipakai jika gaya Windows gagal dimuat
                }
                new ControlPanel(this);
            } catch (Throwable t) {
                System.err.println("Panel kontrol tidak bisa dibuka: " + t);
            }
        });
    }

    private void setupInput() {
        // Keyboard (tombol sekali tekan)
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (action != GLFW_RELEASE) {
                return;
            }
            if (key >= GLFW_KEY_1 && key <= GLFW_KEY_6) {
                selectProjection(ProjectionType.values()[key - GLFW_KEY_1]);
                return;
            }
            switch (key) {
                case GLFW_KEY_ESCAPE:
                    glfwSetWindowShouldClose(w, true);
                    break;
                case GLFW_KEY_SPACE:
                    autoRotate = !autoRotate;
                    titleDirty = true;
                    break;
                case GLFW_KEY_TAB:
                    camera.toggleMode();
                    titleDirty = true;
                    break;
                case GLFW_KEY_R:
                    resetCamera();
                    break;
                case GLFW_KEY_P:
                    projection.resetToDefault();
                    titleDirty = true;
                    break;
                case GLFW_KEY_G:
                    stereoEnabled = !stereoEnabled;
                    titleDirty = true;
                    break;
                default:
                    break;
            }
        });

        // Mouse: tahan tombol kiri lalu geser
        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = (action == GLFW_PRESS);
            }
        });

        glfwSetCursorPosCallback(window, (w, x, y) -> {
            if (dragging) {
                float dx = (float) (x - lastMouseX);
                float dy = (float) (y - lastMouseY);
                camera.rotate(dx * MOUSE_SENSITIVITY, dy * MOUSE_SENSITIVITY);
            }
            lastMouseX = x;
            lastMouseY = y;
        });

        // Scroll mouse = zoom
        glfwSetScrollCallback(window, (w, xOffset, yOffset) -> camera.zoom((float) yOffset));
    }

    // ======================= AKSI (hanya dijalankan di thread OpenGL) =======================

    /** Memilih jenis proyeksi dan memindahkan kamera ke pose standarnya. */
    private void selectProjection(ProjectionType type) {
        taxonomy.setCurrent(type);
        camera.setOrbitalPose(type.getAzimuthDeg(), type.getElevationDeg());
        titleDirty = true;
    }

    private void resetCamera() {
        camera.reset();
        ProjectionType type = taxonomy.getCurrent();
        camera.setOrbitalPose(type.getAzimuthDeg(), type.getElevationDeg());
        titleDirty = true;
    }

    // ======================= API UNTUK ControlPanel (thread Swing) =======================

    private void post(Runnable action) {
        pendingActions.add(action);
    }

    public void requestProjection(ProjectionType type) {
        post(() -> selectProjection(type));
    }

    public void requestStereo(boolean on) {
        post(() -> {
            stereoEnabled = on;
            titleDirty = true;
        });
    }

    public void requestAutoRotate(boolean on) {
        post(() -> {
            autoRotate = on;
            titleDirty = true;
        });
    }

    public void requestIpd(float ipd) {
        post(() -> {
            stereo.setIpd(ipd);
            titleDirty = true;
        });
    }

    public void requestToggleCameraMode() {
        post(() -> {
            camera.toggleMode();
            titleDirty = true;
        });
    }

    public void requestResetCamera() {
        post(this::resetCamera);
    }

    public void requestResetProjection() {
        post(() -> {
            projection.resetToDefault();
            titleDirty = true;
        });
    }

    public String getStatusText() { return statusText; }
    public ProjectionType getShownType() { return shownType; }
    public boolean isShownStereo() { return shownStereo; }
    public boolean isShownAutoRotate() { return shownAutoRotate; }
    public float getShownIpd() { return shownIpd; }

    // ======================= STATUS DAN JUDUL JENDELA =======================

    /** Memperbarui judul jendela dan data status yang dibaca ControlPanel. */
    private void publishState() {
        ProjectionType type = taxonomy.getCurrent();
        String stereoText = stereoEnabled
                ? String.format(Locale.US, "ON (IPD %.2f)", stereo.getIpd())
                : "OFF";

        shownType = type;
        shownStereo = stereoEnabled;
        shownAutoRotate = autoRotate;
        shownIpd = stereo.getIpd();

        statusText = String.format(Locale.US,
                "Proyeksi : %s\n"
              + "Jenis    : %s\n"
              + "Ciri     : %s\n"
              + "Kamera   : %s\n"
              + "FOV      : %.1f deg (perspektif)\n"
              + "Near/Far : %.2f / %.1f\n"
              + "Aspect   : %.2f\n"
              + "Stereo   : %s",
                type.getLabel(),
                type.isPerspective() ? "Perspektif" : "Paralel",
                taxonomy.getDescription(),
                camera.getMode(),
                projection.getFovDegree(),
                projection.getNearPlane(), projection.getFarPlane(),
                projection.getAspectRatio(),
                stereoText);

        glfwSetWindowTitle(window, String.format(Locale.US,
                "ThreeDViewing | %s | Kamera: %s | Stereo: %s",
                type.getLabel(), camera.getMode(), stereoText));
    }

    // ======================= LOOP UTAMA =======================

    private boolean isDown(int key) {
        return glfwGetKey(window, key) == GLFW_PRESS;
    }

    /** Input keyboard yang ditahan, dikali dt supaya halus dan sama di semua laptop. */
    private void processKeyboard(float dt) {
        // Kamera
        if (isDown(GLFW_KEY_LEFT))  camera.rotate(-KEY_ROTATE_SPEED * dt, 0.0f);
        if (isDown(GLFW_KEY_RIGHT)) camera.rotate(KEY_ROTATE_SPEED * dt, 0.0f);
        if (isDown(GLFW_KEY_UP))    camera.rotate(0.0f, -KEY_ROTATE_SPEED * dt);
        if (isDown(GLFW_KEY_DOWN))  camera.rotate(0.0f, KEY_ROTATE_SPEED * dt);

        if (isDown(GLFW_KEY_W)) camera.zoom(KEY_ZOOM_SPEED * dt);
        if (isDown(GLFW_KEY_S)) camera.zoom(-KEY_ZOOM_SPEED * dt);

        float right = 0.0f;
        float upAmount = 0.0f;
        if (isDown(GLFW_KEY_D)) right += KEY_MOVE_SPEED * dt;
        if (isDown(GLFW_KEY_A)) right -= KEY_MOVE_SPEED * dt;
        if (isDown(GLFW_KEY_E)) upAmount += KEY_MOVE_SPEED * dt;
        if (isDown(GLFW_KEY_Q)) upAmount -= KEY_MOVE_SPEED * dt;
        if (right != 0.0f || upAmount != 0.0f) {
            camera.strafe(right, upAmount);
        }

        // Proyeksi: FOV, Near, Far
        if (isDown(GLFW_KEY_Z)) { projection.adjustFov(-FOV_SPEED * dt);   titleDirty = true; }
        if (isDown(GLFW_KEY_X)) { projection.adjustFov(FOV_SPEED * dt);    titleDirty = true; }
        if (isDown(GLFW_KEY_C)) { projection.adjustNear(-NEAR_SPEED * dt); titleDirty = true; }
        if (isDown(GLFW_KEY_V)) { projection.adjustNear(NEAR_SPEED * dt);  titleDirty = true; }
        if (isDown(GLFW_KEY_B)) { projection.adjustFar(-FAR_SPEED * dt);   titleDirty = true; }
        if (isDown(GLFW_KEY_N)) { projection.adjustFar(FAR_SPEED * dt);    titleDirty = true; }

        // Stereo: IPD
        if (isDown(GLFW_KEY_I)) { stereo.adjustIpd(-IPD_SPEED * dt); titleDirty = true; }
        if (isDown(GLFW_KEY_O)) { stereo.adjustIpd(IPD_SPEED * dt);  titleDirty = true; }
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - lastTime);
            lastTime = now;

            // Jalankan perintah dari ControlPanel
            Runnable action;
            while ((action = pendingActions.poll()) != null) {
                action.run();
            }

            processKeyboard(dt);
            if (autoRotate) {
                angle += 30.0f * dt;
            }

            render();

            if (titleDirty) {
                publishState();
                titleDirty = false;
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    // ======================= RENDER =======================

    private void render() {
        glfwGetFramebufferSize(window, fbWidth, fbHeight);
        if (fbWidth[0] == 0 || fbHeight[0] == 0) {
            return; // jendela di-minimize
        }
        glViewport(0, 0, fbWidth[0], fbHeight[0]);

        // Aspect ratio mengikuti ukuran jendela agar objek tidak terdistorsi
        float aspect = (float) fbWidth[0] / (float) fbHeight[0];
        if (Math.abs(aspect - projection.getAspectRatio()) > 1.0e-4f) {
            projection.setAspectRatio(aspect);
            titleDirty = true;
        }

        // Jarak ke objek: dipakai untuk ukuran proyeksi paralel dan bidang zero parallax stereo
        float distance = DEFAULT_VIEW_DISTANCE;
        if (camera.getMode() == Camera3D.Mode.ORBITAL) {
            distance = camera.getEye().distance(camera.getTarget());
        }
        orthoHalfHeight = distance * ORTHO_SCALE;
        stereo.setFocalLength(distance);

        // View dari Camera3D buatan sendiri (LookAt manual)
        Matrix4f view = camera.calculateLookAtMatrix();

        if (stereoEnabled) {
            stereo.renderAnaglyph(view, this::projectionForEye, this::drawScene);
        } else {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glMatrixMode(GL_PROJECTION);
            glLoadMatrixf(projectionForEye(0.0f).get(matrixBuffer));

            glMatrixMode(GL_MODELVIEW);
            glLoadMatrixf(view.get(matrixBuffer));

            drawScene();
        }
    }

    /** Matriks proyeksi untuk satu mata (eyeOffset 0 = mode biasa tanpa stereo). */
    private Matrix4f projectionForEye(float eyeOffset) {
        if (eyeOffset != 0.0f && taxonomy.isPerspective()) {
            // Perspektif + stereo: Off-Axis Asymmetric Frustum
            return stereo.calculateOffAxisProjection(eyeOffset, projection);
        }
        return taxonomy.calculateProjectionMatrix(projection, orthoHalfHeight, eyeOffset,
                stereo.getFocalLength());
    }

    /** Menggambar objek. Dipanggil 1x (mode biasa) atau 2x (mode stereo). */
    private void drawScene() {
        glRotatef(angle, 0.0f, 1.0f, 0.0f);
        glRotatef(angle * 0.5f, 1.0f, 0.0f, 0.0f);
        cube.draw();
    }

    private void cleanup() {
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }
}