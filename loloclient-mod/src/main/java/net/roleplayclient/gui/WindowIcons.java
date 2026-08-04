package net.roleplayclient.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Imposta l'icona della finestra del processo Java Minecraft
 * con il logo RC minimale (stesso stile launcher, meno dettaglio).
 */
public final class WindowIcons {
    private WindowIcons() {
    }

    public static void apply() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        long handle = client.getWindow().getHandle();

        NativeImage i16 = null;
        NativeImage i32 = null;
        NativeImage i48 = null;
        ByteBuffer b16 = null;
        ByteBuffer b32 = null;
        ByteBuffer b48 = null;
        try {
            i16 = Icons.buildMinimalLogo(16);
            i32 = Icons.buildMinimalLogo(32);
            i48 = Icons.buildMinimalLogo(48);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                GLFWImage.Buffer icons = GLFWImage.malloc(3, stack);
                b16 = toRgba(i16);
                b32 = toRgba(i32);
                b48 = toRgba(i48);
                icons.position(0).width(16).height(16).pixels(b16);
                icons.position(1).width(32).height(32).pixels(b32);
                icons.position(2).width(48).height(48).pixels(b48);
                icons.position(0);
                GLFW.glfwSetWindowIcon(handle, icons);
            }
            System.out.println("[Roleplay Client] Icona finestra MC applicata (logo minimale)");
        } catch (Exception e) {
            System.err.println("[Roleplay Client] Impossibile impostare icona finestra: " + e.getMessage());
        } finally {
            if (b16 != null) MemoryUtil.memFree(b16);
            if (b32 != null) MemoryUtil.memFree(b32);
            if (b48 != null) MemoryUtil.memFree(b48);
            if (i16 != null) i16.close();
            if (i32 != null) i32.close();
            if (i48 != null) i48.close();
        }
    }

    private static ByteBuffer toRgba(NativeImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        ByteBuffer buf = MemoryUtil.memAlloc(w * h * 4);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getColorArgb(x, y);
                buf.put((byte) ((argb >> 16) & 0xFF));
                buf.put((byte) ((argb >> 8) & 0xFF));
                buf.put((byte) (argb & 0xFF));
                buf.put((byte) ((argb >> 24) & 0xFF));
            }
        }
        buf.flip();
        return buf;
    }
}
