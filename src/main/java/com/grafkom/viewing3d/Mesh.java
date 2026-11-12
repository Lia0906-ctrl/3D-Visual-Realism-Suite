package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL30.*;

/**
 * Mesh = model 3D yang sudah ada di memori GPU (VBO) beserta "cara membacanya" (VAO).
 * Ada dua VAO yang memakai posisi dan UV yang sama, tetapi normalnya berbeda:
 *  - VAO Smooth : normal rata-rata vertex
 *  - VAO Flat   : normal per-face
 * Jadi pergantian Flat/Smooth cukup memilih VAO, tanpa mengirim ulang data.
 */
public class Mesh {

    private final int vboPositions;
    private final int vboSmooth;
    private final int vboFlat;
    private final int vboUV;
    private final int vaoSmooth;
    private final int vaoFlat;
    private final int vertexCount;

    public Mesh(ObjModelLoader.MeshData data) {
        vertexCount = data.vertexCount;
        vboPositions = upload(data.positions);
        vboSmooth = upload(data.smoothNormals);
        vboFlat = upload(data.flatNormals);
        vboUV = upload(data.uvs);
        vaoSmooth = createVao(vboSmooth);
        vaoFlat = createVao(vboFlat);
    }

    private static int upload(float[] data) {
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    /** location 0 = posisi, 1 = normal, 2 = UV (sesuai "layout (location = ...)" di vertex shader). */
    private int createVao(int normalVbo) {
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vboPositions);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, normalVbo);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, vboUV);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return vao;
    }

    public void draw(boolean flatShading) {
        glBindVertexArray(flatShading ? vaoFlat : vaoSmooth);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public int getTriangleCount() {
        return vertexCount / 3;
    }

    public void cleanup() {
        glDeleteVertexArrays(vaoSmooth);
        glDeleteVertexArrays(vaoFlat);
        glDeleteBuffers(vboPositions);
        glDeleteBuffers(vboSmooth);
        glDeleteBuffers(vboFlat);
        glDeleteBuffers(vboUV);
    }
}
