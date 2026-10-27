package minechem.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DeliriumOverlayTest {
    /** Level 0 gives markiplier 5: (int) (220 * 5 - 150) = 950, capped to 0xB6, the level I alpha. */
    @Test
    void levelZeroKeepsTheAlpha() {
        assertEquals((0xB6 << 24) | 0x101020, DeliriumOverlay.colourFor(0));
    }

    /** Level 1 gives markiplier 10: (int) (220 * 10 - 150) = 2050, capped to 0xB6 (it was 0x02 after the shift). */
    @Test
    void levelOneIsAsStrongAsLevelZero() {
        assertEquals((0xB6 << 24) | 0x101020, DeliriumOverlay.colourFor(1));
    }
}
