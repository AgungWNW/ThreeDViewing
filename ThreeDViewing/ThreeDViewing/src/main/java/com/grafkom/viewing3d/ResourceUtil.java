package com.grafkom.viewing3d;

import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** Utilitas membaca file dari classpath (src/main/resources), aman dijalankan dari NetBeans maupun JAR. */
public final class ResourceUtil {

    private ResourceUtil() { }

    public static byte[] readBytes(String path) {
        try (InputStream in = ResourceUtil.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Resource tidak ditemukan di classpath: " + path);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca resource: " + path, e);
        }
    }

    public static String readText(String path) {
        return new String(readBytes(path), StandardCharsets.UTF_8);
    }

    /** Membaca resource ke direct ByteBuffer (dibutuhkan STBImage). */
    public static ByteBuffer readToDirectBuffer(String path) {
        byte[] bytes = readBytes(path);
        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }
}
