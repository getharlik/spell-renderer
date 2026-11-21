package dev.harlik.impl.buffer;

import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public record VertexFormat(LinkedHashMap<String, Integer> attributes) {

    private static final Map<String, VertexFormatElement> ELEMENTS = new HashMap<>();

    /** Builds the matching Blaze3D vertex format. Attribute names must match the shader inputs. */
    public com.mojang.blaze3d.vertex.VertexFormat gpu() {
        var builder = com.mojang.blaze3d.vertex.VertexFormat.builder();
        attributes.forEach((name, size) -> builder.add(name, element(name, size)));
        return builder.build();
    }

    private static synchronized VertexFormatElement element(String name, int size) {
        if (size < 1 || size > 4) throw new IllegalArgumentException("Unsupported attribute size " + size + " for " + name);
        return ELEMENTS.computeIfAbsent(name + ":" + size, key -> {
            for (int id = 0; id < VertexFormatElement.MAX_COUNT; id++) {
                if (VertexFormatElement.byId(id) == null) {
                    return VertexFormatElement.register(id, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, size);
                }
            }
            throw new IllegalStateException("No vertex format element IDs available for " + name);
        });
    }

    public int getStride() {
        int total = 0;
        for (Integer i : attributes.values()) {
            total += i * Float.BYTES;
        }
        return total;
    }

    public int getFloats() {
        int total = 0;
        for (Integer i : attributes.values()) {
            total += i;
        }
        return total;
    }

    public static final VertexFormat RECTANGLE = new VertexFormat(
            new LinkedHashMap<String, Integer>() {{
                put("aPos", 2);
                put("aSize", 2);
                put("aColor", 4);
                put("aRadius", 4);
                put("aSmooth", 1);
                put("aUV", 2);
            }}
    );

    public static final VertexFormat GLASS = new VertexFormat(
            new LinkedHashMap<String, Integer>() {{
                put("aPos", 2);
                put("aSize", 2);
                put("aColor", 4);
                put("aRadius", 4);
                put("aSmooth", 1);
                put("aUV", 2);
                put("aGlass", 4);
                put("aHighlight", 1);
            }}
    );

    public static final VertexFormat TEXT =new VertexFormat(
            new LinkedHashMap<String, Integer>() {{
                put("aPos", 2);
                put("aUV", 2);
                put("aColor", 4);
                put("aPxRange", 1);
            }}
    );
}
