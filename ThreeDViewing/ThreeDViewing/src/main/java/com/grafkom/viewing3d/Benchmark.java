package com.grafkom.viewing3d;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Benchmark FPS otomatis (Key F9) untuk bahan analisis "perbandingan kinerja rendering (FPS)".
 * Tiap skenario dijalankan dengan VSync OFF: pemanasan 0,7 detik lalu pengukuran 3 detik.
 * Hasil dicetak ke konsol dan disimpan ke benchmark_result.txt + benchmark_result.csv.
 */
public class Benchmark {

    private static final double WARMUP_SECONDS = 0.7;
    private static final double MEASURE_SECONDS = 3.0;

    private record Step(String name, Consumer<RenderSettings> config) { }

    private record Result(String name, double fps) { }

    private final List<Step> steps = new ArrayList<>();
    private final List<Result> results = new ArrayList<>();
    private boolean running;
    private int index;
    private double stepStart;
    private double measureStart;
    private int frames;
    private boolean measuring;
    private String systemInfo = "";

    public Benchmark() {
        steps.add(new Step("Baseline (Blinn-Phong, Smooth, Z-Buffer ON, Culling ON, Tekstur Mipmap, Shadow ON)", s -> { }));
        steps.add(new Step("Phong (bukan Blinn-Phong)", s -> s.blinnPhong = false));
        steps.add(new Step("Flat Shading", s -> s.flatShading = true));
        steps.add(new Step("Z-Buffer Depth Test OFF", s -> s.depthTest = false));
        steps.add(new Step("Back-Face Culling OFF", s -> s.backFaceCulling = false));
        steps.add(new Step("Tekstur OFF", s -> s.useTexture = false));
        steps.add(new Step("Tekstur Nearest (tanpa mipmap)", s -> s.mipmapFilter = false));
        steps.add(new Step("Shadow Mapping OFF", s -> s.shadows = false));
        steps.add(new Step("Minimal (Tekstur OFF + Shadow OFF)", s -> { s.useTexture = false; s.shadows = false; }));
    }

    public boolean isRunning() {
        return running;
    }

    public String status() {
        return running ? "BENCHMARK " + (index + 1) + "/" + steps.size() + ": " + steps.get(index).name() : "";
    }

    public void start(RenderSettings settings, double now, String systemInfo) {
        results.clear();
        index = 0;
        running = true;
        this.systemInfo = systemInfo;
        beginStep(settings, now);
        System.out.println("[Benchmark] Dimulai: " + steps.size() + " skenario x ~"
                + (WARMUP_SECONDS + MEASURE_SECONDS) + " detik. Jangan menggeser jendela.");
    }

    private void beginStep(RenderSettings s, double now) {
        s.copyFrom(new RenderSettings());   // mulai dari pengaturan default
        s.vsync = false;                    // ukur FPS tanpa batas refresh rate
        s.autoRotate = true;
        steps.get(index).config().accept(s);
        stepStart = now;
        frames = 0;
        measuring = false;
        System.out.println("[Benchmark] (" + (index + 1) + "/" + steps.size() + ") " + steps.get(index).name());
    }

    /** Dipanggil sekali per frame setelah swap buffer. @return true saat seluruh skenario selesai. */
    public boolean onFrame(RenderSettings settings, double now) {
        if (!running) {
            return false;
        }
        if (!measuring) {
            if (now - stepStart >= WARMUP_SECONDS) {
                measuring = true;
                measureStart = now;
                frames = 0;
            }
            return false;
        }
        frames++;
        double elapsed = now - measureStart;
        if (elapsed >= MEASURE_SECONDS) {
            results.add(new Result(steps.get(index).name(), frames / elapsed));
            index++;
            if (index >= steps.size()) {
                running = false;
                report();
                return true;
            }
            beginStep(settings, now);
        }
        return false;
    }

    private void report() {
        double base = results.get(0).fps();
        StringBuilder sb = new StringBuilder();
        sb.append(systemInfo).append(System.lineSeparator()).append(System.lineSeparator());
        sb.append(String.format(Locale.US, "%-4s %-92s %10s %12s %10s%n", "No", "Skenario", "FPS", "ms/frame", "vs Base"));
        StringBuilder csv = new StringBuilder("No,Skenario,FPS,ms_per_frame,persen_vs_baseline\n");
        int no = 1;
        for (Result r : results) {
            double ms = 1000.0 / r.fps();
            double pct = (r.fps() / base - 1.0) * 100.0;
            if (Math.abs(pct) < 0.05) {
                pct = 0.0;
            }
            sb.append(String.format(Locale.US, "%-4d %-92s %10.1f %12.4f %9.1f%%%n", no, r.name(), r.fps(), ms, pct));
            csv.append(String.format(Locale.US, "%d,\"%s\",%.2f,%.4f,%.2f%n", no, r.name(), r.fps(), ms, pct));
            no++;
        }
        System.out.println();
        System.out.println(sb);
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Path.of("benchmark_result.txt"), StandardCharsets.UTF_8))) {
            w.print(sb);
        } catch (IOException e) {
            System.err.println("Gagal menulis benchmark_result.txt: " + e.getMessage());
        }
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Path.of("benchmark_result.csv"), StandardCharsets.UTF_8))) {
            w.print(csv);
        } catch (IOException e) {
            System.err.println("Gagal menulis benchmark_result.csv: " + e.getMessage());
        }
        System.out.println("[Benchmark] Selesai. Hasil disimpan: benchmark_result.txt dan benchmark_result.csv");
    }
}
