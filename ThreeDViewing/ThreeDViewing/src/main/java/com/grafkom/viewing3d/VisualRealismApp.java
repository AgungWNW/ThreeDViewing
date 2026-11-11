package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.util.Locale;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 3D Visual Realism Suite: Shading Models, Flat/Smooth, Hidden Surface Removal,
 * Texture Mapping dan Real-Time Shadow Mapping (2-Pass).
 */
public class VisualRealismApp {

    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;
    private static final float Z_FAR = 100f;
    private static final float FLOOR_HALF_SIZE = 6f;
    private static final float DECAL_OFFSET_Y = 0.0002f; // jarak decal dari lantai untuk demo Z-Fighting

    private long window;
    private int fbWidth = WINDOW_WIDTH;
    private int fbHeight = WINDOW_HEIGHT;

    // ---- State & komponen ----
    private final RenderSettings settings = new RenderSettings();
    private RenderSettings settingsBackup;
    private final OrbitCamera camera = new OrbitCamera();
    private final Light light = new Light();
    private final Benchmark benchmark = new Benchmark();

    private ShaderProgram phongShader;
    private ShaderProgram depthShader;
    private ShaderProgram debugShader;
    private Texture2D objectTexture;
    private Texture2D floorTexture;
    private ShadowMapFBO shadowFbo;

    private Mesh[] meshes;
    private String[] meshNames;
    private float[] meshHeights;
    private float[] meshTilts;
    private int meshIndex = 0;
    private Mesh floorMesh;
    private Mesh decalMesh;
    private int quadVao;
    private int quadVbo;

    // ---- Variabel runtime ----
    private float objectAngle = 0f;
    private boolean appliedVsync = true;
    private boolean screenshotRequested = false;
    private boolean dragging = false;
    private double lastCursorX, lastCursorY;
    private String systemInfo = "";

    // FPS untuk judul jendela
    private double fpsTimer = 0;
    private int fpsFrames = 0;
    private double currentFps = 0;

    // Matriks yang dipakai ulang tiap frame
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f lightSpaceMatrix = new Matrix4f();
    private final Matrix4f modelMatrix = new Matrix4f();
    private final Vector3f tmpVec = new Vector3f();

    public void run() {
        try {
            init();
            loop();
        } finally {
            cleanup();
        }
    }

    // =====================================================================
    // INISIALISASI
    // =====================================================================
    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Gagal menginisialisasi GLFW");
        }
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);   // dibutuhkan di macOS
        glfwWindowHint(GLFW_DEPTH_BITS, 24);                     // presisi Z-Buffer 24-bit
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) {
            throw new IllegalStateException("Gagal membuat jendela GLFW (butuh GPU/driver OpenGL 3.3+)");
        }

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> onKey(key, action));
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = (action == GLFW_PRESS);
                if (dragging) {
                    double[] x = new double[1], y = new double[1];
                    glfwGetCursorPos(window, x, y);
                    lastCursorX = x[0];
                    lastCursorY = y[0];
                }
            }
        });
        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (dragging) {
                camera.orbit((float) (x - lastCursorX), (float) (y - lastCursorY));
            }
            lastCursorX = x;
            lastCursorY = y;
        });
        glfwSetScrollCallback(window, (win, xOffset, yOffset) -> camera.zoom((float) yOffset));
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            fbWidth = w;
            fbHeight = h;
        });

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        appliedVsync = true;

        int[] w = new int[1], h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        fbWidth = w[0];
        fbHeight = h[0];

        GL.createCapabilities();
        glfwShowWindow(window);

        systemInfo = String.format("GPU      : %s%nVendor   : %s%nOpenGL   : %s%nGLSL     : %s%nJava     : %s (%s)%nOS       : %s %s",
                glGetString(GL_RENDERER), glGetString(GL_VENDOR), glGetString(GL_VERSION),
                glGetString(0x8B8C /* GL_SHADING_LANGUAGE_VERSION */),
                System.getProperty("java.version"), System.getProperty("java.vendor"),
                System.getProperty("os.name"), System.getProperty("os.arch"));
        System.out.println(systemInfo);

        initScene();
        printHelp();
        logState();
    }

    private void initScene() {
        phongShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
        debugShader = new ShaderProgram("/shaders/debug_depth.vert", "/shaders/debug_depth.frag");

        objectTexture = new Texture2D("/textures/object.png");
        floorTexture = new Texture2D("/textures/floor.png");
        shadowFbo = new ShadowMapFBO();

        // Mesh 3D bermateri: normal vertex + UV (Sphere, Torus, Cube)
        meshes = new Mesh[]{
                new Mesh(MeshFactory.createSphere(1.0f, 14, 28)),
                new Mesh(MeshFactory.createTorus(1.2f, 0.45f, 32, 16)),
                new Mesh(MeshFactory.createCube(1.0f))
        };
        meshNames = new String[]{"Sphere", "Torus", "Cube"};
        meshHeights = new float[]{1.0f, 1.6f, 1.02f};   // tinggi pusat objek di atas lantai
        meshTilts = new float[]{0f, 0.9f, 0f};           // kemiringan awal (radian)

        floorMesh = new Mesh(MeshFactory.createPlane(FLOOR_HALF_SIZE, 8f));
        decalMesh = new Mesh(MeshFactory.createPlane(2.2f, 1f));

        // Quad layar penuh untuk pratinjau Shadow Map: (x, y, u, v)
        float[] quad = {
                -1f, -1f, 0f, 0f,
                 1f, -1f, 1f, 0f,
                -1f,  1f, 0f, 1f,
                 1f,  1f, 1f, 1f
        };
        quadVao = glGenVertexArrays();
        glBindVertexArray(quadVao);
        quadVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
        glBufferData(GL_ARRAY_BUFFER, quad, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        glBindVertexArray(0);
    }

    // =====================================================================
    // INPUT
    // =====================================================================
    private void onKey(int key, int action) {
        if (action == GLFW_RELEASE) {
            return;
        }
        if (key == GLFW_KEY_ESCAPE) {
            glfwSetWindowShouldClose(window, true);
            return;
        }
        if (benchmark.isRunning()) {
            return; // saat benchmark, input fitur dikunci
        }
        boolean press = (action == GLFW_PRESS);

        // K dan L boleh ditahan (repeat)
        if (key == GLFW_KEY_K) {
            settings.shadowBias = Math.max(0f, Math.round((settings.shadowBias - 0.05f) * 100f) / 100f);
            logState();
            return;
        }
        if (key == GLFW_KEY_L) {
            settings.shadowBias = Math.min(1f, Math.round((settings.shadowBias + 0.05f) * 100f) / 100f);
            logState();
            return;
        }
        if (!press) {
            return;
        }

        switch (key) {
            case GLFW_KEY_B -> settings.blinnPhong = !settings.blinnPhong;
            case GLFW_KEY_F -> settings.flatShading = !settings.flatShading;
            case GLFW_KEY_Z -> settings.depthTest = !settings.depthTest;
            case GLFW_KEY_C -> settings.backFaceCulling = !settings.backFaceCulling;
            case GLFW_KEY_V -> settings.wireframe = !settings.wireframe;
            case GLFW_KEY_T -> settings.useTexture = !settings.useTexture;
            case GLFW_KEY_M -> settings.mipmapFilter = !settings.mipmapFilter;
            case GLFW_KEY_H -> settings.shadows = !settings.shadows;
            case GLFW_KEY_X -> settings.zFightDemo = !settings.zFightDemo;
            case GLFW_KEY_N -> settings.zNearIndex = (settings.zNearIndex + 1) % RenderSettings.Z_NEAR_VALUES.length;
            case GLFW_KEY_U -> settings.showShadowMap = !settings.showShadowMap;
            case GLFW_KEY_P -> settings.autoRotate = !settings.autoRotate;
            case GLFW_KEY_Y -> settings.vsync = !settings.vsync;
            case GLFW_KEY_1 -> meshIndex = 0;
            case GLFW_KEY_2 -> meshIndex = 1;
            case GLFW_KEY_3 -> meshIndex = 2;
            case GLFW_KEY_R -> resetAll();
            case GLFW_KEY_F1 -> printHelp();
            case GLFW_KEY_F2 -> screenshotRequested = true;
            case GLFW_KEY_F9 -> startBenchmark();
            default -> { return; }
        }
        logState();
    }

    private void resetAll() {
        settings.copyFrom(new RenderSettings());
        camera.reset();
        light.reset();
        meshIndex = 0;
        objectAngle = 0f;
    }

    private void startBenchmark() {
        settingsBackup = settings.copy();
        camera.reset();
        light.reset();
        benchmark.start(settings, glfwGetTime(), systemInfo);
    }

    private void printHelp() {
        System.out.println("""

                ==================== KONTROL ====================
                 B  : Phong <-> Blinn-Phong           (Indikator 1)
                 F  : Flat <-> Smooth Shading         (Indikator 2)
                 Z  : Z-Buffer Depth Test ON/OFF      (Indikator 3)
                 C  : Back-Face Culling ON/OFF        (Indikator 3)
                 V  : Wireframe ON/OFF (amati culling)
                 X  : Decal koplanar (demo Z-Fighting)
                 N  : Ganti nilai zNear (0.1/0.01/0.001/0.0001)
                 T  : Tekstur ON/OFF                  (Indikator 4)
                 M  : Filter Nearest <-> Bilinear+Mipmap
                 H  : Shadow Mapping ON/OFF           (Indikator 5)
                 K/L: Kurangi / Tambah Shadow Bias (0.00 - 1.00)
                 U  : Tampilkan Shadow Map (Depth FBO)
                 1/2/3 : Sphere / Torus / Cube
                 Panah : Gerakkan cahaya   | Mouse drag: putar kamera | Scroll: zoom
                 P  : Jeda rotasi objek    | Y: VSync ON/OFF
                 F2 : Simpan screenshot ke folder 'screenshots'
                 F9 : Benchmark FPS otomatis (hasil ke benchmark_result.txt/csv)
                 R  : Reset semua           | F1: Bantuan | ESC: Keluar
                =================================================
                """);
    }

    private void logState() {
        System.out.println("[" + meshNames[meshIndex] + "] " + settings.summary());
    }

    // =====================================================================
    // LOOP UTAMA
    // =====================================================================
    private void loop() {
        double last = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            last = now;

            glfwPollEvents();
            update(dt);

            if (fbWidth > 0 && fbHeight > 0) {
                render();
                if (screenshotRequested) {
                    screenshotRequested = false;
                    String path = ScreenshotSaver.save(fbWidth, fbHeight, settings.fileTag());
                    System.out.println(path != null ? "[Screenshot] Tersimpan: " + path : "[Screenshot] GAGAL menyimpan");
                }
            }
            glfwSwapBuffers(window);

            if (settings.vsync != appliedVsync) {
                glfwSwapInterval(settings.vsync ? 1 : 0);
                appliedVsync = settings.vsync;
            }

            if (benchmark.onFrame(settings, glfwGetTime())) {
                settings.copyFrom(settingsBackup);
                logState();
            }
            updateTitle(dt);
        }
    }

    private void update(float dt) {
        if (settings.autoRotate) {
            objectAngle += 0.6f * dt;
        }
        if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS)  light.rotateAzimuth(-1.5f * dt);
        if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) light.rotateAzimuth(1.5f * dt);
        if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS)    light.rotateElevation(1.0f * dt);
        if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS)  light.rotateElevation(-1.0f * dt);
    }

    private void updateTitle(float dt) {
        fpsTimer += dt;
        fpsFrames++;
        if (fpsTimer >= 0.5) {
            currentFps = fpsFrames / fpsTimer;
            fpsTimer = 0;
            fpsFrames = 0;
            String prefix = benchmark.isRunning() ? "[" + benchmark.status() + "] " : "";
            glfwSetWindowTitle(window, String.format(Locale.US, "%s3D Visual Realism Suite | %s | FPS: %.1f | %s",
                    prefix, meshNames[meshIndex], currentFps, settings.summary()));
        }
    }

    // =====================================================================
    // RENDERING
    // =====================================================================
    private void render() {
        light.getLightSpaceMatrix(lightSpaceMatrix);

        // ---------- Render Pass 1: Depth Pass (sudut pandang cahaya) ----------
        if (settings.shadows || settings.showShadowMap) {
            renderDepthPass();
        }

        // ---------- Render Pass 2: Lighting Pass (sudut pandang kamera) ----------
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, fbWidth, fbHeight);
        glClearColor(0.10f, 0.12f, 0.17f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        HSRManager.setupDepthTest(settings.depthTest);
        HSRManager.setupBackFaceCulling(settings.backFaceCulling);
        HSRManager.setupWireframe(settings.wireframe);

        objectTexture.setMipmapped(settings.mipmapFilter);
        floorTexture.setMipmapped(settings.mipmapFilter);

        projectionMatrix.identity().perspective((float) Math.toRadians(45.0),
                (float) fbWidth / (float) fbHeight, settings.zNear(), Z_FAR);
        camera.getViewMatrix(viewMatrix);
        Vector3f camPos = camera.getPosition(new Vector3f());
        Vector3f lightPos = light.getPosition(new Vector3f());

        phongShader.use();
        phongShader.setMat4("view", viewMatrix);
        phongShader.setMat4("projection", projectionMatrix);
        phongShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        phongShader.setVec3("lightPos", lightPos);
        phongShader.setVec3("viewPos", camPos);
        phongShader.setVec3("lightColor", 1.0f, 1.0f, 1.0f);
        phongShader.setBool("useBlinnPhong", settings.blinnPhong);
        phongShader.setBool("useShadow", settings.shadows);
        phongShader.setFloat("shadowBias", settings.shadowBias);
        phongShader.setInt("textureSampler", 0);
        phongShader.setInt("shadowMap", 1);

        // Shadow Map pada texture unit 1
        glActiveTexture(GL_TEXTURE0 + 1);
        glBindTexture(GL_TEXTURE_2D, shadowFbo.getDepthTextureID());
        glActiveTexture(GL_TEXTURE0);

        // Lantai
        phongShader.setMat4("model", modelMatrix.identity());
        phongShader.setBool("useTexture", settings.useTexture);
        phongShader.setVec3("objectColor", 0.75f, 0.78f, 0.85f);
        floorTexture.bind(0);
        floorMesh.draw(false);

        // Objek 3D
        buildObjectModelMatrix();
        phongShader.setMat4("model", modelMatrix);
        phongShader.setBool("useTexture", settings.useTexture);
        phongShader.setVec3("objectColor", 0.95f, 0.55f, 0.20f);
        objectTexture.bind(0);
        meshes[meshIndex].draw(settings.flatShading);

        // Decal koplanar dengan lantai (hanya untuk mengamati Z-Fighting)
        if (settings.zFightDemo) {
            phongShader.setMat4("model", modelMatrix.identity().translate(3.5f, DECAL_OFFSET_Y, 2.5f));
            phongShader.setBool("useTexture", false);
            phongShader.setVec3("objectColor", 0.90f, 0.10f, 0.10f);
            decalMesh.draw(false);
        }

        HSRManager.setupWireframe(false);

        if (settings.showShadowMap) {
            drawShadowMapPreview();
        }
    }

    private void buildObjectModelMatrix() {
        modelMatrix.identity()
                .translate(0f, meshHeights[meshIndex], 0f)
                .rotateY(objectAngle)
                .rotateX(meshTilts[meshIndex]);
    }

    /** Render Pass 1: hanya menyimpan depth dari sudut pandang cahaya ke Shadow Map FBO. */
    private void renderDepthPass() {
        HSRManager.setupWireframe(false);
        HSRManager.setupDepthTest(true);          // Depth Pass selalu butuh depth test
        HSRManager.setupBackFaceCulling(false);

        shadowFbo.bindForWriting();
        depthShader.use();
        depthShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);

        depthShader.setMat4("model", modelMatrix.identity());
        floorMesh.draw(false);

        buildObjectModelMatrix();
        depthShader.setMat4("model", modelMatrix);
        meshes[meshIndex].draw(settings.flatShading);

        shadowFbo.unbind(fbWidth, fbHeight);
    }

    /** Menampilkan isi Shadow Map (grayscale) di pojok kiri-bawah layar. */
    private void drawShadowMapPreview() {
        int size = Math.max(128, Math.min(fbWidth, fbHeight) / 3);
        glViewport(12, 12, size, size);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        debugShader.use();
        debugShader.setInt("depthMap", 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, shadowFbo.getDepthTextureID());
        glBindVertexArray(quadVao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);
        glViewport(0, 0, fbWidth, fbHeight);
    }

    // =====================================================================
    // CLEANUP
    // =====================================================================
    private void cleanup() {
        if (window != NULL) {
            Callbacks.glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
        }
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) {
            cb.free();
        }
    }
}
