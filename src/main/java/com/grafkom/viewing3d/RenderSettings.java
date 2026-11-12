package com.grafkom.viewing3d;

/**
 * RenderSettings = "papan status" mode rendering yang sedang aktif.
 * InputHandler mengubahnya (lewat tombol), VisualRealismApp membacanya saat menggambar.
 */
public class RenderSettings {

    private boolean blinnPhong = true;      // B : true = Blinn-Phong, false = Phong
    private boolean flatShading = false;    // F : true = Flat, false = Smooth
    private boolean depthTest = true;       // Z : Z-Buffer on/off
    private boolean backFaceCulling = true; // C : Back-Face Culling on/off
    private boolean shadows = true;         // S : bayangan on/off
    private int shadowBiasSteps = 1;        // [ dan ] : Shadow Bias = langkah x 0.002
    private boolean useTexture = true;      // T : tekstur on/off (off = warna polos)
    private boolean bilinearMipmap = true;  // M : true = Bilinear+Mipmap, false = Nearest
    private boolean autoRotate = true;      // SPASI
    private boolean vsync = true;           // V : nyalakan/matikan batas 60 FPS

    public void toggleBlinnPhong() { blinnPhong = !blinnPhong; }
    public void toggleFlatShading() { flatShading = !flatShading; }
    public void toggleDepthTest() { depthTest = !depthTest; }
    public void toggleBackFaceCulling() { backFaceCulling = !backFaceCulling; }
    public void toggleShadows() { shadows = !shadows; }
    public void increaseShadowBias() { shadowBiasSteps = Math.min(25, shadowBiasSteps + 1); }
    public void decreaseShadowBias() { shadowBiasSteps = Math.max(0, shadowBiasSteps - 1); }
    public void toggleTexture() { useTexture = !useTexture; }
    public void toggleFilter() { bilinearMipmap = !bilinearMipmap; }
    public void toggleAutoRotate() { autoRotate = !autoRotate; }
    public void toggleVsync() { vsync = !vsync; }

    public boolean isBlinnPhong() { return blinnPhong; }
    public boolean isFlatShading() { return flatShading; }
    public boolean isDepthTest() { return depthTest; }
    public boolean isBackFaceCulling() { return backFaceCulling; }
    public boolean isShadows() { return shadows; }
    public float getShadowBias() { return shadowBiasSteps * 0.002f; }
    public boolean isUseTexture() { return useTexture; }
    public boolean isBilinearMipmap() { return bilinearMipmap; }
    public boolean isAutoRotate() { return autoRotate; }
    public boolean isVsync() { return vsync; }

    /** Teks ringkas untuk judul jendela. */
    public String describe() {
        return (blinnPhong ? "Blinn-Phong" : "Phong")
                + " | " + (flatShading ? "Flat" : "Smooth")
                + " | Z " + (depthTest ? "ON" : "OFF")
                + " | Cull " + (backFaceCulling ? "ON" : "OFF")
                + " | Tex " + (!useTexture ? "OFF" : (bilinearMipmap ? "Mipmap" : "Nearest"))
                + " | Shadow " + (shadows ? String.format(java.util.Locale.US, "ON (bias %.3f)", getShadowBias()) : "OFF")
                + " | VSync " + (vsync ? "ON" : "OFF");
    }
}
