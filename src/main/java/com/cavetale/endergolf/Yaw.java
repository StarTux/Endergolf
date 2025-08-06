package com.cavetale.endergolf;

import com.cavetale.core.font.Unicode;

public final class Yaw {
    public static Unicode yawToArrow(float yaw) {
        if (yaw < -157.5f) {
            return Unicode.ARROW_DOWN;
        } else if (yaw < -112.5f) {
            return Unicode.ARROW_DOWN_LEFT;
        } else if (yaw < -67.5f) {
            return Unicode.ARROW_LEFT;
        } else if (yaw < -22.5f) {
            return Unicode.ARROW_UP_LEFT;
        } else if (yaw < 22.5f) {
            return Unicode.ARROW_UP;
        } else if (yaw < 67.5f) {
            return Unicode.ARROW_UP_RIGHT;
        } else if (yaw < 112.5f) {
            return Unicode.ARROW_RIGHT;
        } else if (yaw < 157.5f) {
            return Unicode.ARROW_DOWN_RIGHT;
        } else {
            return Unicode.ARROW_DOWN;
        }
    }

    private Yaw() { }
}
