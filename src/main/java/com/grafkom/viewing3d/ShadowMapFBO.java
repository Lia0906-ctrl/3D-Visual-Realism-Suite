package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL30.*;

/**
 * ShadowMapFBO = Framebuffer Object yang hanya menyimpan DEPTH (kedalaman) dari sudut pandang lampu.
 * Hasilnya disebut Shadow Map (Depth Map). Dipakai pada Render Pass 1 (Depth Pass),
 * lalu dibaca oleh fragment shader pada Render Pass 2 untuk menentukan area bayangan.
 */
public class ShadowMapFBO {

    public static final int SHADOW_WIDTH = 2048;
    public static final int SHADOW_HEIGHT = 2048;

    private final int fboID;
    private final int depthTextureID;

    public ShadowMapFBO() {
        fboID = glGenFramebuffers();

        // Tekstur depth 2048 x 2048 (24-bit agar presisinya terjamin)
        depthTextureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTextureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, SHADOW_WIDTH, SHADOW_HEIGHT, 0,
                GL_DEPTH_COMPONENT, GL_FLOAT, (java.nio.ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        // Di luar shadow map dianggap "jauh" (depth 1.0) sehingga tidak ada bayangan palsu
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        float[] borderColor = {1.0f, 1.0f, 1.0f, 1.0f};
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);

        // Pasang tekstur depth ke FBO; tidak ada buffer warna
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextureID, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Depth FBO tidak lengkap (framebuffer incomplete)");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /** Mulai menggambar ke shadow map (Pass 1). */
    public void bindForWriting() {
        glViewport(0, 0, SHADOW_WIDTH, SHADOW_HEIGHT);
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    /** Kembali menggambar ke layar. */
    public void unbind(int screenWidth, int screenHeight) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, screenWidth, screenHeight);
    }

    /** Pasang shadow map ke texture unit tertentu agar bisa dibaca shader (Pass 2). */
    public void bindDepthTexture(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, depthTextureID);
    }

    public int getDepthTextureID() { return depthTextureID; }

    public void cleanup() {
        glDeleteFramebuffers(fboID);
        glDeleteTextures(depthTextureID);
    }
}
