package dev.harlik.impl;

import com.mojang.blaze3d.opengl.GlStateManager;
import static org.lwjgl.opengl.GL33.*;

public final class GlState implements AutoCloseable {

    private final boolean blend;
    private final boolean depth;
    private final boolean scissor;
    private final boolean cull;
    private final int blendSrcRgb;
    private final int blendDstRgb;
    private final int blendSrcAlpha;
    private final int blendDstAlpha;
    private final int activeTexture;
    private final int texture2d;
    private final int drawFbo;
    private final int[] viewport = new int[4];
    private final int[] scissorBox = new int[4];

    private GlState() {
        blend = glIsEnabled(GL_BLEND);
        depth = glIsEnabled(GL_DEPTH_TEST);
        scissor = glIsEnabled(GL_SCISSOR_TEST);
        cull = glIsEnabled(GL_CULL_FACE);
        blendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB);
        blendDstRgb = glGetInteger(GL_BLEND_DST_RGB);
        blendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA);
        blendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);
        activeTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        texture2d = glGetInteger(GL_TEXTURE_BINDING_2D);
        drawFbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        glGetIntegerv(GL_VIEWPORT, viewport);
        glGetIntegerv(GL_SCISSOR_TEST, scissorBox);
    }

    public static GlState capture() {
        return new GlState();
    }

    public void restore() {
        if (blend) glEnable(GL_BLEND); else glDisable(GL_BLEND);
        if (depth) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
        if (scissor) glEnable(GL_SCISSOR_TEST); else glDisable(GL_SCISSOR_TEST);
        if (cull) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);
        glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        glActiveTexture(activeTexture);
        glBindTexture(GL_TEXTURE_2D, texture2d);
        GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, drawFbo);
        GlStateManager._viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        glScissor(scissorBox[0], scissorBox[1], scissorBox[2], viewport[3]);
    }

    @Override
    public void close() {
        restore();
    }
}
