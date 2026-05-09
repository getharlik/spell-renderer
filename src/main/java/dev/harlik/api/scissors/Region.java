package dev.harlik.api.scissors;

public record Region(float x, float y, float w, float h) {

    public boolean inBounds(float mouseX, float mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.w && mouseY >= this.y && mouseY <= this.y + this.h;
    }

    public static boolean inBounds(float mouseX, float mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    public static Region intersect(Region a, Region b) {
        float left   = Math.max(a.x, b.x);
        float top    = Math.max(a.y, b.y);
        float right  = Math.min(a.x + a.w, b.x + b.w);
        float bottom = Math.min(a.y + a.h, b.y + b.h);
        if (right <= left || bottom <= top) return new Region(0, 0, 0, 0);
        return new Region(left, top, right - left, bottom - top);
    }

}
