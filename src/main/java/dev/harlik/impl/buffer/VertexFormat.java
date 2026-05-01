package dev.harlik.impl.buffer;

import java.util.LinkedHashMap;

public record VertexFormat(LinkedHashMap<String, Integer> attributes) {

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

    public static final VertexFormat TEXT = new VertexFormat(
            new LinkedHashMap<String, Integer>() {{
                put("aPos", 2);
                put("aUV", 2);
                put("aColor", 4);
                put("aPxRange", 1);
            }}
    );
}
