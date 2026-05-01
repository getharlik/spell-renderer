package dev.harlik.api.font;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.harlik.SpellRenderer;
import dev.harlik.impl.batch.TextureCache;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class FontManager {

    public static final FontManager INSTANCE = new FontManager();
    private final Map<String, Font> fonts = new HashMap<>();
    private Font fallback;

    public void init() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            load("spell_fallback", ResourceLocation.fromNamespaceAndPath(SpellRenderer.MOD_ID, "fonts/inter"));
            fallback = fonts.get("spell_fallback");
        });
    }

    public void load(String key, ResourceLocation baseName) {
        ResourceLocation jsonLoc = ResourceLocation.fromNamespaceAndPath(
                baseName.getNamespace(), baseName.getPath() + ".json");
        ResourceLocation pngLoc = ResourceLocation.fromNamespaceAndPath(
                baseName.getNamespace(), baseName.getPath() + ".png");

        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        Optional<Resource> res = rm.getResource(jsonLoc);
        if (res.isEmpty()) {
            SpellRenderer.LOGGER.error("Font JSON missing: {}", jsonLoc);
            return;
        }

        JsonObject root;
        try (var stream = res.get().open()) {
            root = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
        } catch (IOException e) {
            SpellRenderer.LOGGER.error("Failed to read font JSON {}: {}", jsonLoc, e.toString());
            return;
        }

        JsonObject atlas = root.getAsJsonObject("atlas");
        JsonObject metrics = root.getAsJsonObject("metrics");

        Map<Integer, Glyph> glyphs = new HashMap<>();
        for (var el : root.getAsJsonArray("glyphs")) {
            JsonObject g = el.getAsJsonObject();
            int cp = g.get("unicode").getAsInt();
            float advance = g.get("advance").getAsFloat();

            boolean visible = g.has("planeBounds") && g.has("atlasBounds");
            float pl = 0, pb = 0, pr = 0, pt = 0, al = 0, ab = 0, ar = 0, at = 0;
            if (visible) {
                JsonObject p = g.getAsJsonObject("planeBounds");
                pl = p.get("left").getAsFloat();
                pb = p.get("bottom").getAsFloat();
                pr = p.get("right").getAsFloat();
                pt = p.get("top").getAsFloat();
                JsonObject a = g.getAsJsonObject("atlasBounds");
                al = a.get("left").getAsFloat();
                ab = a.get("bottom").getAsFloat();
                ar = a.get("right").getAsFloat();
                at = a.get("top").getAsFloat();
            }
            glyphs.put(cp, new Glyph(cp, advance, pl, pb, pr, pt, al, ab, ar, at, visible));
        }

        Font font = new Font(
                glyphs,
                metrics.get("lineHeight").getAsFloat(),
                metrics.get("ascender").getAsFloat(),
                metrics.get("descender").getAsFloat(),
                atlas.get("distanceRange").getAsFloat(),
                atlas.get("size").getAsFloat(),
                atlas.get("width").getAsInt(),
                atlas.get("height").getAsInt(),
                TextureCache.INSTANCE.get(pngLoc)
        );

        fonts.put(key, font);
        SpellRenderer.LOGGER.info("Loaded font '{}' ({}) with {} glyphs", key, baseName, glyphs.size());
    }

    public Font get(String key) {
        Font font = fonts.get(key);
        if (font == null) {
            SpellRenderer.LOGGER.error("Font '{}' not found", key);
            return fallback;
        }
        return font;
    }

}
