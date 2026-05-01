package dev.harlik.impl.shader;

import com.mojang.blaze3d.opengl.GlStateManager;
import dev.harlik.SpellRenderer;
import dev.harlik.impl.batch.BatchManager;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL33;
import static org.lwjgl.opengl.GL33.*;

public class ShaderManager {

    public static final ShaderManager INSTANCE = new ShaderManager();
    private final Map<String, Shader> shaders = new HashMap<>();

    public void init() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> load());
    }

    public void load() {
        register("rectangle", "rectangle");
        register("rectangle", "image");
        register("rectangle", "glass");
        register("text", "text");

        BatchManager.INSTANCE.init();
    }

    private void register(String vertex, String fragment) {
        Shader shader = new Shader(vertex, fragment);
        shaders.put(fragment, shader);
    }

    public Shader getShader(String name) {
        return shaders.get(name);
    }

    public int readResources(String name, ShaderType type) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        Optional<Resource> resource = resourceManager.getResource(ResourceLocation.fromNamespaceAndPath(SpellRenderer.MOD_ID, "shader/core/" + name + type.fileExtension));
        if (resource.isEmpty()) {
            SpellRenderer.LOGGER.error("Failed to load shader {}, {}", name, type.fileExtension);
            return 0;
        }

        String source;
        try (var stream = resource.get().open()) {
            source = new String(stream.readAllBytes());
        } catch (IOException e) {
            SpellRenderer.LOGGER.error("Failed to read shader. {}, {}, {}", name, type.fileExtension, e.toString());
            return 0;
        }

        int i = glCreateShader(type.glType);
        GlStateManager.glShaderSource(i, source);
        glCompileShader(i);
        if (glGetShaderi(i, GL33.GL_COMPILE_STATUS) == GL_FALSE) {
            SpellRenderer.LOGGER.error("Failed to compile shader. {}, {}, {}", name, type.fileExtension, glGetShaderInfoLog(i));
            glDeleteShader(i);
            return 0;
        }
        return i;
    }

    public void destroy() {
        for (Shader shader : shaders.values()) {
            shader.destroy();
        }
        shaders.clear();
    }

}
