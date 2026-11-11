# ThreeDViewing — 3D Visual Realism Suite
Grafika Komputer · Minggu ke-11 · Rendering Faces pada Visual Realism
Java 17+ · LWJGL 3.3.3 · JOML · Modern OpenGL 3.3 (GLSL 330 core) · Maven (NetBeans)

## Cara membuka di NetBeans
1. Pastikan JDK 17 atau lebih baru terpasang (Tools > Java Platforms).
2. `File > Open Project...` lalu pilih folder `ThreeDViewing` (ikon cangkir kopi/Maven).
3. Klik kanan project > `Build with Dependencies` (butuh internet **sekali** untuk mengunduh LWJGL & JOML).
4. Klik kanan project > `Run` (F6). Main class: `com.grafkom.viewing3d.Main`.

Catatan: komputer harus punya GPU/driver yang mendukung OpenGL 3.3+.
Pengguna macOS: tambahkan VM option `-XstartOnFirstThread` pada Project Properties > Run.

## Kontrol
| Tombol | Fungsi | Indikator |
|---|---|---|
| **B** | Phong ↔ Blinn-Phong | 1 |
| **F** | Flat ↔ Smooth Shading | 2 |
| **Z** | Z-Buffer Depth Test ON/OFF | 3 |
| **C** | Back-Face Culling ON/OFF | 3 |
| V | Wireframe (memudahkan mengamati culling) | 3 |
| X | Decal koplanar untuk demo Z-Fighting | 3 |
| N | Ganti zNear: 0.1 / 0.01 / 0.001 / 0.0001 | 3 |
| T | Tekstur ON/OFF | 4 |
| M | Filter Nearest ↔ Bilinear + Mipmap | 4 |
| H | Shadow Mapping ON/OFF | 5 |
| K / L | Kurangi / tambah Shadow Bias (0.00–1.00; 1.00 = rumus modul) | 5 |
| U | Pratinjau Shadow Map (Depth FBO) di pojok kiri-bawah | 5 |
| 1 / 2 / 3 | Sphere / Torus / Cube | — |
| Panah | Gerakkan sumber cahaya | — |
| Mouse drag / scroll | Putar kamera / zoom | — |
| P | Jeda rotasi objek | — |
| Y | VSync ON/OFF | — |
| **F2** | Simpan screenshot PNG ke folder `screenshots/` | — |
| **F9** | Benchmark FPS otomatis → `benchmark_result.txt` & `.csv` | — |
| R / F1 / ESC | Reset / bantuan / keluar | — |

Status fitur selalu tampil di **judul jendela** (beserta FPS) dan di konsol NetBeans.

## Struktur kode (package `com.grafkom.viewing3d`)
| File | Peran |
|---|---|
| `Main`, `VisualRealismApp` | Window GLFW, input, loop, Render Pass 1 (depth) & Pass 2 (lighting) |
| `RenderSettings` | Seluruh toggle fitur (OOP, bisa disalin/di-reset) |
| `ShaderProgram`, `ResourceUtil` | Kompilasi GLSL & pembacaan resource dari classpath |
| `MeshData`, `MeshFactory`, `Mesh` | Geometri (Sphere/Torus/Cube/Plane) + normal vertex + UV; VAO smooth & flat |
| `HSRManager` | **Praktikum 2** — Z-Buffer & Back-Face Culling |
| `Texture2D` | **Praktikum 3** — STBImage, UV, filtering & mipmap |
| `ShadowMapFBO` | **Praktikum 4** — Depth FBO 2048×2048 |
| `Light`, `OrbitCamera` | Light Space Matrix (ortografik) & kamera orbit |
| `ScreenshotSaver`, `Benchmark` | Alat bantu pengujian untuk laporan |

Shader (`src/main/resources/shaders/`): `phong.vert/.frag` (**Praktikum 1**), `depth.vert/.frag` (Pass 1), `debug_depth.vert/.frag` (pratinjau shadow map).
Tekstur (`src/main/resources/textures/`): `object.png`, `floor.png`.

## Pemetaan indikator → implementasi
1. **Shading Models** — `phong.frag`: Ambient + Diffuse + Specular; `useBlinnPhong` memilih `reflect()` (Phong, n=16) atau Halfway Vector (Blinn-Phong, n=32).
2. **Flat vs Smooth** — `MeshData.buildFlatVertexBuffer()` (satu normal per face) vs `computeSmoothNormals()` (vertex normal averaging).
3. **HSR** — `HSRManager` (GL_DEPTH_TEST + GL_LESS, GL_CULL_FACE + GL_BACK + GL_CCW); depth buffer 24-bit & zNear=0.1 untuk menekan Z-Fighting.
4. **Texture Mapping** — UV pada setiap mesh, `Texture2D` + sampler GLSL, `GL_NEAREST` vs `GL_LINEAR_MIPMAP_LINEAR`.
5. **Shadow Mapping** — Pass 1 ke `ShadowMapFBO`, Pass 2 membandingkan `d_current` dengan `d_shadow + bias`, PCF 3×3, Shadow Factor 0.2 / 1.0.
