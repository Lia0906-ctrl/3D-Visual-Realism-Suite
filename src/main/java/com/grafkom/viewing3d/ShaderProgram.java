package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL30.*;

/**
 * ShaderProgram = membaca file GLSL (.vert/.frag) dari folder resources, mengompilasi,
 * menautkannya jadi satu program GPU, lalu menyediakan method untuk mengirim data (uniform).
 */
public class ShaderProgram {

    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();
    private final float[] matrixBuffer = new float[16];

    public ShaderProgram(String vertexPath, String fragmentPath) {
        int vs = compile(GL_VERTEX_SHADER, readResource(vertexPath), vertexPath);
        int fs = compile(GL_FRAGMENT_SHADER, readResource(fragmentPath), fragmentPath);

        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Link shader gagal:\n" + glGetProgramInfoLog(programId));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private int compile(int type, String source, String name) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Compile shader gagal (" + name + "):\n" + glGetShaderInfoLog(id));
        }
        return id;
    }

    private static String readResource(String path) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("File shader tidak ditemukan: " + path
                        + " (cek folder src/main/resources lalu Clean and Build)");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca shader: " + path, e);
        }
    }

    public void use() {
        glUseProgram(programId);
    }

    private int location(String name) {
        return uniformCache.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    public void setInt(String name, int value)        { glUniform1i(location(name), value); }
    public void setBool(String name, boolean value)   { glUniform1i(location(name), value ? 1 : 0); }
    public void setFloat(String name, float value)    { glUniform1f(location(name), value); }

    public void setVec3(String name, Vector3f v) {
        glUniform3f(location(name), v.x, v.y, v.z);
    }

    public void setMat4(String name, Matrix4f m) {
        glUniformMatrix4fv(location(name), false, m.get(matrixBuffer));
    }

    public void cleanup() {
        glDeleteProgram(programId);
    }
}
