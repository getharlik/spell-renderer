package dev.harlik.api.builder;

import dev.harlik.api.builder.shapes.*;
import dev.harlik.impl.batch.BatchManager;
import net.minecraft.resources.ResourceLocation;

public class Builder {

    public static ShapeRect rect() {
        return new ShapeRect(BatchManager.INSTANCE.getRectangle());
    }

    public static ShapeCircle circle() {
        return new ShapeCircle(BatchManager.INSTANCE.getRectangle());
    }

    public static ShapeImage image(ResourceLocation texture) {
        return new ShapeImage(BatchManager.INSTANCE.getImage(), texture);
    }

    public static ShapeGlass glass() {
        return new ShapeGlass(BatchManager.INSTANCE.getGlass());
    }

    public static ShapeText text(String text) {
        return new ShapeText(BatchManager.INSTANCE.getText(),  text);
    }
}
