package dev.harlik.api.scale;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

public class Scale {

    public static final Scale INSTANCE = new Scale();

    @Getter @Setter
    private float scale = 1.0f;
    @Getter
    private final float[] matrix = new float[16];
    @Getter
    private float viewportWidth, viewportHeight;
    private float lastW, lastH, lastScale;

    public void updateMatrix() {
        Minecraft mc = Minecraft.getInstance();
        int fbWidth = mc.getWindow().getWidth();
        int fbHeight = mc.getWindow().getHeight();

        if (fbWidth == lastW && fbHeight == lastH && scale == lastScale) return;

        lastW = fbWidth;
        lastH = fbHeight;
        lastScale = scale;

        viewportWidth = fbWidth / scale;
        viewportHeight = fbHeight / scale;

        float left = 0, right = viewportWidth;
        float top = 0, bottom = viewportHeight;
        float near = -1, far = 1;

        matrix[0] = 2 / (right - left);
        matrix[1] = 0;
        matrix[2] = 0;
        matrix[3] = 0;
        matrix[4] = 0;
        matrix[5] = 2 / (top - bottom);
        matrix[6] = 0;
        matrix[7] = 0;
        matrix[8] = 0;
        matrix[9] = 0;
        matrix[10] = -2 / (far - near);
        matrix[11] = 0;
        matrix[12] = -(right + left) / (right - left);
        matrix[13] = -(top + bottom) / (top - bottom);
        matrix[14] = -(far + near) / (far - near);
        matrix[15] = 1;
    }
}
