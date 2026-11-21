package dev.harlik.impl.batch;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import dev.harlik.SpellRenderer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

/** Loads image files into GPU textures. Textures are referred to by an id, 0 means the texture failed to load. */
public class TextureCache {

    public static final TextureCache INSTANCE = new TextureCache();

    private final Map<Identifier, Integer> ids = new HashMap<>();
    private final List<GpuTexture> textures = new ArrayList<>();
    private final List<GpuTextureView> views = new ArrayList<>();

    public int get(Identifier location) {
        return ids.computeIfAbsent(location, this::load);
    }

    /** Returns the view of a texture id, or null when it failed to load. */
    public GpuTextureView view(int id) {
        return id > 0 && id <= views.size() ? views.get(id - 1) : null;
    }

    /** Loads the image. Must be called while no render pass is open. */
    public int load(Identifier location) {
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

        GpuDevice device = RenderSystem.getDevice();
        GpuTexture texture = device.createTexture(() -> "spell-renderer " + location,
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8, width, height, 1, 1);
        device.createCommandEncoder().writeToTexture(texture, pixels, NativeImage.Format.RGBA, 0, 0, 0, 0, width, height);
        STBImage.stbi_image_free(pixels);

        textures.add(texture);
        views.add(device.createTextureView(texture));
        return views.size();
    }

    public void destroy() {
        views.forEach(GpuTextureView::close);
        textures.forEach(GpuTexture::close);
        views.clear();
        textures.clear();
        ids.clear();
    }
}
