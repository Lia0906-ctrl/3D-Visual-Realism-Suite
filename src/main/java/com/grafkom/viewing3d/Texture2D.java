package com.grafkom.viewing3d;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;

/**
 * Texture2D = memuat gambar PNG/JPG (lewat STBImage) ke memori GPU sebagai tekstur.
 * Gambar dibaca dari folder resources (contoh "/textures/wood.png"), jadi program
 * bisa dijalankan dari folder mana pun.
 * Dua mode filter:
 *  - Nearest             : texel terdekat (kotak-kotak, tanpa mipmap)
 *  - Bilinear + Mipmap   : rata-rata 4 texel + pilihan ukuran gambar sesuai jarak (halus, tanpa aliasing)
 */
public class Texture2D {

    private final int textureID;
    private int width;
    private int height;
    private Boolean bilinearMipmap = null;   // mode filter yang sedang dipasang

    public Texture2D(String resourcePath) {
        ByteBuffer fileData = readResource(resourcePath);

        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Wrap: tekstur diulang jika koordinat UV melebihi 1.0
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // Gambar disusun dari atas, OpenGL dari bawah: balik agar tidak terbalik arah V
        stbi_set_flip_vertically_on_load(true);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            ByteBuffer data = stbi_load_from_memory(fileData, w, h, channels, 4);   // paksa RGBA
            if (data == null) {
                throw new RuntimeException("Gagal memuat tekstur " + resourcePath
                        + " : " + stbi_failure_reason());
            }
            width = w.get(0);
            height = h.get(0);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
            glGenerateMipmap(GL_TEXTURE_2D);   // buat gambar 1/2, 1/4, 1/8, ... ukuran asli
            stbi_image_free(data);
        }

        setFilter(true);
    }

    private static ByteBuffer readResource(String path) {
        try (InputStream in = Texture2D.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("File tekstur tidak ditemukan: " + path
                        + " (cek folder src/main/resources/textures lalu Clean and Build)");
            }
            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca tekstur: " + path, e);
        }
    }

    /** true = Bilinear + Mipmap, false = Nearest. */
    public void setFilter(boolean useBilinearMipmap) {
        if (bilinearMipmap != null && bilinearMipmap == useBilinearMipmap) {
            return;   // sudah dalam mode ini
        }
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (useBilinearMipmap) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
        bilinearMipmap = useBilinearMipmap;
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public int getWidth()  { return width; }
    public int getHeight() { return height; }

    public void cleanup() {
        glDeleteTextures(textureID);
    }
}
