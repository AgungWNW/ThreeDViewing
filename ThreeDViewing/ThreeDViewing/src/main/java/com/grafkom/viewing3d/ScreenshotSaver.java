package com.grafkom.viewing3d;

import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImageWrite.stbi_write_png;

/** Menyimpan isi framebuffer menjadi PNG ke folder ./screenshots (Key F2). */
public final class ScreenshotSaver {

    private ScreenshotSaver() { }

    /** Harus dipanggil setelah render selesai dan sebelum glfwSwapBuffers. @return path file atau null bila gagal */
    public static String save(int width, int height, String tag) {
        try {
            Files.createDirectories(Path.of("screenshots"));
        } catch (IOException e) {
            System.err.println("Gagal membuat folder screenshots: " + e.getMessage());
            return null;
        }
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 3);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glReadBuffer(GL_BACK);
        glReadPixels(0, 0, width, height, GL_RGB, GL_UNSIGNED_BYTE, pixels);

        // OpenGL membaca dari baris bawah; PNG dari baris atas -> balik vertikal
        int rowBytes = width * 3;
        ByteBuffer flipped = BufferUtils.createByteBuffer(width * height * 3);
        byte[] row = new byte[rowBytes];
        for (int y = height - 1; y >= 0; y--) {
            pixels.position(y * rowBytes);
            pixels.get(row);
            flipped.put(row);
        }
        flipped.flip();

        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path file = Path.of("screenshots", "shot_" + stamp + "_" + tag + ".png");
        boolean ok = stbi_write_png(file.toString(), width, height, 3, flipped, rowBytes);
        return ok ? file.toAbsolutePath().toString() : null;
    }
}
