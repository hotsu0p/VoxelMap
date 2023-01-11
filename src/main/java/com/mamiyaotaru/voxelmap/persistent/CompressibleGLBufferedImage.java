package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.interfaces.IGLBufferedImage;
import com.mamiyaotaru.voxelmap.util.CompressionUtils;
import com.mamiyaotaru.voxelmap.util.GLShim;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.zip.DataFormatException;

public class CompressibleGLBufferedImage implements IGLBufferedImage {
    private byte[] bytes;
    private int index = 0;
    private final int width;
    private final int height;
    private final Object bufferLock = new Object();
    private boolean isCompressed = false;
    private static final HashMap<Integer, ByteBuffer> byteBuffers = new HashMap<>(4);
    private static final ByteBuffer defaultSizeBuffer = ByteBuffer.allocateDirect(262144).order(ByteOrder.nativeOrder());
    private final boolean compressNotDelete;

    public CompressibleGLBufferedImage(int width, int height, int imageType) {
        this.width = width;
        this.height = height;
        this.bytes = new byte[width * height * 4];
        this.compressNotDelete = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions().outputImages;
    }

    public byte[] getData() {
        if (this.isCompressed) {
            this.decompress();
        }

        return this.bytes;
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void baleet() {
        int currentIndex = this.index;
        this.index = 0;
        if (currentIndex != 0 && RenderSystem.isOnRenderThreadOrInit()) {
            GLShim.glDeleteTextures(currentIndex);
        }

    }

    @Override
    public void write() {
        if (this.isCompressed) {
            this.decompress();
        }

        if (this.index == 0) {
            this.index = GLShim.glGenTextures();
        }

        ByteBuffer buffer = byteBuffers.get(this.width * this.height);
        if (buffer == null) {
            buffer = ByteBuffer.allocateDirect(this.width * this.height * 4).order(ByteOrder.nativeOrder());
            byteBuffers.put(this.width * this.height, buffer);
        }

        buffer.clear();
        synchronized (this.bufferLock) {
            buffer.put(this.bytes);
        }

        buffer.position(0).limit(this.bytes.length);
        GLShim.glBindTexture(GL11.GL_TEXTURE_2D, this.index);
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GLShim.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GLShim.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GLShim.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GLShim.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, this.getWidth(), this.getHeight(), 0, GL11.GL_RGBA, GL12.GL_UNSIGNED_INT_8_8_8_8, buffer);
        GLShim.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        this.compress();
    }

    @Override
    public void blank() {
        if (this.isCompressed) {
            this.decompress();
        }

        Arrays.fill(this.bytes, (byte) 0);
        this.write();
    }

    @Override
    public void setRGB(int x, int y, int color) {
        if (this.isCompressed) {
            this.decompress();
        }

        int index = (x + y * this.getWidth()) * 4;
        synchronized (this.bufferLock) {
            int alpha = color >> 24 & 0xFF;
            this.bytes[index] = -1;
            this.bytes[index + 1] = (byte) ((color & 0xFF) * alpha / 255);
            this.bytes[index + 2] = (byte) ((color >> 8 & 0xFF) * alpha / 255);
            this.bytes[index + 3] = (byte) ((color >> 16 & 0xFF) * alpha / 255);
        }
    }

    @Override
    public void moveX(int x) {
        synchronized (this.bufferLock) {
            if (x > 0) {
                System.arraycopy(this.bytes, x * 4, this.bytes, 0, this.bytes.length - x * 4);
            } else if (x < 0) {
                System.arraycopy(this.bytes, 0, this.bytes, -x * 4, this.bytes.length + x * 4);
            }

        }
    }

    @Override
    public void moveY(int y) {
        synchronized (this.bufferLock) {
            if (y > 0) {
                System.arraycopy(this.bytes, y * this.getWidth() * 4, this.bytes, 0, this.bytes.length - y * this.getWidth() * 4);
            } else if (y < 0) {
                System.arraycopy(this.bytes, 0, this.bytes, -y * this.getWidth() * 4, this.bytes.length + y * this.getWidth() * 4);
            }

        }
    }

    private synchronized void compress() {
        if (!this.isCompressed) {
            if (this.compressNotDelete) {
                try {
                    this.bytes = CompressionUtils.compress(this.bytes);
                } catch (IOException ignored) {}
            } else {
                this.bytes = null;
            }

            this.isCompressed = true;
        }
    }

    private synchronized void decompress() {
        if (this.isCompressed) {
            if (this.compressNotDelete) {
                try {
                    this.bytes = CompressionUtils.decompress(this.bytes);
                } catch (IOException | DataFormatException ignored) {}
            } else {
                this.bytes = new byte[this.width * this.height * 4];
                this.isCompressed = false;
            }

        }
    }

    static {
        byteBuffers.put(65536, defaultSizeBuffer);
    }
}
