package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

/** Pembungkus program GLSL (vertex + fragment) beserta helper pengiriman uniform. */
public class ShaderProgram {

    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    /** @param vertexPath   path classpath, contoh "/shaders/phong.vert"
     *  @param fragmentPath path classpath, contoh "/shaders/phong.frag" */
    public ShaderProgram(String vertexPath, String fragmentPath) {
        int vs = compile(GL_VERTEX_SHADER, ResourceUtil.readText(vertexPath), vertexPath);
        int fs = compile(GL_FRAGMENT_SHADER, ResourceUtil.readText(fragmentPath), fragmentPath);
        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new IllegalStateException("Link shader gagal (" + vertexPath + " + " + fragmentPath + "):\n"
                    + glGetProgramInfoLog(programId));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static int compile(int type, String source, String name) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            throw new IllegalStateException("Kompilasi shader gagal (" + name + "):\n" + glGetShaderInfoLog(shader));
        }
        return shader;
    }

    public void use() {
        glUseProgram(programId);
    }

    private int loc(String name) {
        return uniformCache.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    public void setInt(String name, int v)        { glUniform1i(loc(name), v); }
    public void setBool(String name, boolean v)   { glUniform1i(loc(name), v ? 1 : 0); }
    public void setFloat(String name, float v)    { glUniform1f(loc(name), v); }
    public void setVec3(String name, Vector3f v)  { glUniform3f(loc(name), v.x, v.y, v.z); }
    public void setVec3(String name, float x, float y, float z) { glUniform3f(loc(name), x, y, z); }

    public void setMat4(String name, Matrix4f m) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            m.get(fb);
            glUniformMatrix4fv(loc(name), false, fb);
        }
    }

    public void dispose() {
        glDeleteProgram(programId);
    }
}
