package de.battlerap.tvapp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CursorMathTest {

    @Test
    public void stepGrowsSmoothlyWithRepeat() {
        assertEquals(24, MainActivity.stepForRepeat(0));
        assertEquals(30, MainActivity.stepForRepeat(1));
        assertEquals(48, MainActivity.stepForRepeat(4));
        assertEquals(72, MainActivity.stepForRepeat(8));
        assertEquals(144, MainActivity.stepForRepeat(20));
    }

    @Test
    public void stepIsCappedAndSafe() {
        assertEquals(144, MainActivity.stepForRepeat(999));
        assertEquals(24, MainActivity.stepForRepeat(-5));
    }

    @Test
    public void clampKeepsCursorOnScreen() {
        assertEquals(0f, MainActivity.clamp(-10f, 0f, 320f), 0f);
        assertEquals(320f, MainActivity.clamp(400f, 0f, 320f), 0f);
        assertEquals(150f, MainActivity.clamp(150f, 0f, 320f), 0f);
        assertEquals(0f, MainActivity.clamp(0f, 0f, 0f), 0f);
    }
}
