package com.grafkom.viewing3d;

import java.util.Locale;

/** Seluruh toggle fitur Visual Realism dalam satu objek agar mudah disalin / di-reset (OOP). */
public class RenderSettings {

    /** Variasi zNear untuk mengamati Z-Fighting (Key 'N'). */
    public static final float[] Z_NEAR_VALUES = {0.1f, 0.01f, 0.001f, 0.0001f};

    public boolean blinnPhong = true;        // Key B  : Blinn-Phong <-> Phong
    public boolean flatShading = false;      // Key F  : Flat <-> Smooth
    public boolean depthTest = true;         // Key Z  : Z-Buffer ON/OFF
    public boolean backFaceCulling = true;   // Key C  : Back-Face Culling ON/OFF
    public boolean wireframe = false;        // Key V  : Wireframe
    public boolean useTexture = true;        // Key T  : Texture ON/OFF
    public boolean mipmapFilter = true;      // Key M  : Nearest <-> Bilinear+Mipmap
    public boolean shadows = true;           // Key H  : Shadow Mapping ON/OFF
    public float shadowBias = 0.10f;         // Key K/L: pengali bias (1.0 = rumus modul)
    public boolean zFightDemo = false;       // Key X  : decal koplanar untuk demo Z-Fighting
    public int zNearIndex = 0;               // Key N  : siklus nilai zNear
    public boolean showShadowMap = false;    // Key U  : pratinjau Depth Map
    public boolean autoRotate = true;        // Key P  : rotasi objek
    public boolean vsync = true;             // Key Y  : VSync

    public float zNear() {
        return Z_NEAR_VALUES[zNearIndex];
    }

    public RenderSettings copy() {
        RenderSettings s = new RenderSettings();
        s.copyFrom(this);
        return s;
    }

    public void copyFrom(RenderSettings o) {
        blinnPhong = o.blinnPhong;
        flatShading = o.flatShading;
        depthTest = o.depthTest;
        backFaceCulling = o.backFaceCulling;
        wireframe = o.wireframe;
        useTexture = o.useTexture;
        mipmapFilter = o.mipmapFilter;
        shadows = o.shadows;
        shadowBias = o.shadowBias;
        zFightDemo = o.zFightDemo;
        zNearIndex = o.zNearIndex;
        showShadowMap = o.showShadowMap;
        autoRotate = o.autoRotate;
        vsync = o.vsync;
    }

    private static String onOff(boolean b) {
        return b ? "ON" : "OFF";
    }

    /** Ringkasan status untuk judul jendela dan konsol. */
    public String summary() {
        return String.format(Locale.US,
                "Shading: %s | Normal: %s | Z-Buffer: %s | Culling: %s | Wireframe: %s | Tekstur: %s | Shadow: %s (bias x%.2f) | zNear: %s | VSync: %s",
                blinnPhong ? "Blinn-Phong" : "Phong",
                flatShading ? "Flat" : "Smooth",
                onOff(depthTest),
                onOff(backFaceCulling),
                onOff(wireframe),
                useTexture ? (mipmapFilter ? "ON (Mipmap)" : "ON (Nearest)") : "OFF",
                onOff(shadows),
                shadowBias,
                zNear(),
                onOff(vsync));
    }

    /** Tag singkat untuk nama file screenshot. */
    public String fileTag() {
        return String.format(Locale.US, "%s_%s_Z%d_C%d_W%d_%s_Sh%d_b%.2f",
                blinnPhong ? "Blinn" : "Phong",
                flatShading ? "Flat" : "Smooth",
                depthTest ? 1 : 0,
                backFaceCulling ? 1 : 0,
                wireframe ? 1 : 0,
                useTexture ? (mipmapFilter ? "TexMip" : "TexNearest") : "NoTex",
                shadows ? 1 : 0,
                shadowBias);
    }
}
