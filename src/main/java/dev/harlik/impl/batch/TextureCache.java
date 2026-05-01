package dev.harlik.impl.batch;

import dev.harlik.SpellRenderer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL33.*;

public class TextureCache {

    public static final TextureCache INSTANCE = new TextureCache();

    private final Map<ResourceLocation, Integer> textures = new HashMap<>();

    public int get(ResourceLocation location) {
        return textures.computeIfAbsent(location, this::load);
    }

    public int load(ResourceLocation location) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
        if (resource.isEmpty()) {
            SpellRenderer.LOGGER.error("Texture not found: {}", location);
            return 0;
        }

        byte[] bytes;
        try (var stream = resource.get().open()) {
            bytes = stream.readAllBytes();
        } catch (IOException e) {
            SpellRenderer.LOGGER.error("Failed to read texture {}: {}", location, e.toString());
            return 0;
        }

        ByteBuffer fileBuffer = BufferUtils.createByteBuffer(bytes.length);
        fileBuffer.put(bytes).flip();

        ByteBuffer pixels;
        int width, height;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var w = stack.mallocInt(1);
            var h = stack.mallocInt(1);
            var channels = stack.mallocInt(1);

            pixels = STBImage.stbi_load_from_memory(fileBuffer, w, h, channels, 4);
            if (pixels == null) {
                SpellRenderer.LOGGER.error("Failed to decode texture {}: {}", location, STBImage.stbi_failure_reason());
                return 0;
            }
            width = w.get(0);
            height = h.get(0);
        }

        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        int texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glBindTexture(GL_TEXTURE_2D, previousTexture);

        STBImage.stbi_image_free(pixels);
        return texId;
    }

    public void destroy() {
        for (int id : textures.values()) {
            glDeleteTextures(id);
        }
        textures.clear();
    }
}
