package com.grafkom.viewing3d;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

/**
 * Mesh di sisi GPU. Menyimpan DUA VAO:
 *  - smooth : indexed, normal = rata-rata normal face di tiap vertex (Smooth Shading)
 *  - flat   : non-indexed, normal = normal face (Flat Shading)
 * Layout atribut: location 0 = posisi, 1 = normal, 2 = UV (stride 32 byte).
 */
public class Mesh {

    private final int smoothVao, smoothVbo, smoothEbo, smoothIndexCount;
    private final int flatVao, flatVbo, flatVertexCount;

    public Mesh(MeshData data) {
        // ---- VAO Smooth (indexed) ----
        float[] smooth = data.buildSmoothVertexBuffer();
        int[] indices = data.getIndices();
        smoothIndexCount = indices.length;

        smoothVao = glGenVertexArrays();
        glBindVertexArray(smoothVao);
        smoothVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, smoothVbo);
        glBufferData(GL_ARRAY_BUFFER, toFloatBuffer(smooth), GL_STATIC_DRAW);
        smoothEbo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, smoothEbo);
        IntBuffer ib = BufferUtils.createIntBuffer(indices.length);
        ib.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
        setupAttributes();
        glBindVertexArray(0);

        // ---- VAO Flat (non-indexed) ----
        float[] flat = data.buildFlatVertexBuffer();
        flatVertexCount = data.getFlatVertexCount();

        flatVao = glGenVertexArrays();
        glBindVertexArray(flatVao);
        flatVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, flatVbo);
        glBufferData(GL_ARRAY_BUFFER, toFloatBuffer(flat), GL_STATIC_DRAW);
        setupAttributes();
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private static FloatBuffer toFloatBuffer(float[] data) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(data.length);
        fb.put(data).flip();
        return fb;
    }

    private static void setupAttributes() {
        int stride = MeshData.FLOATS_PER_VERTEX * Float.BYTES;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
    }

    /** @param flatShading true = per-face normal, false = per-vertex averaged normal */
    public void draw(boolean flatShading) {
        if (flatShading) {
            glBindVertexArray(flatVao);
            glDrawArrays(GL_TRIANGLES, 0, flatVertexCount);
        } else {
            glBindVertexArray(smoothVao);
            glDrawElements(GL_TRIANGLES, smoothIndexCount, GL_UNSIGNED_INT, 0L);
        }
        glBindVertexArray(0);
    }

    public void dispose() {
        glDeleteVertexArrays(smoothVao);
        glDeleteVertexArrays(flatVao);
        glDeleteBuffers(smoothVbo);
        glDeleteBuffers(smoothEbo);
        glDeleteBuffers(flatVbo);
    }
}
