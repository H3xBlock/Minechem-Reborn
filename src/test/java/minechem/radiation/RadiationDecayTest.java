package minechem.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RadiationDecayTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final long LIFE = RadiationEnum.hardlyRadioactive.getLife();

    /** The shield removes its share of the damage, rounded; a full shield removes all of it. */
    @Test
    void shieldsReduceTheDamage() {
        assertEquals(18, RadiationDecay.reduce(18, 0F));
        assertEquals(11, RadiationDecay.reduce(18, 0.4F));
        assertEquals(0, RadiationDecay.reduce(18, 1F));
    }

    @Test
    void aNewStackStartsItsDecayNow() {
        RadiationDecay.Result result = RadiationDecay.step(null, RadiationEnum.hardlyRadioactive, 1, 500L, OVERWORLD);
        assertEquals(new RadiationState(500L, 500L, OVERWORLD), result.state());
        assertEquals(0, result.damage());
        assertFalse(result.decayed());
    }

    @Test
    void nothingHappensBeforeTheLifeHasPassed() {
        RadiationState start = new RadiationState(0L, 0L, OVERWORLD);
        RadiationDecay.Result result = RadiationDecay.step(start, RadiationEnum.hardlyRadioactive, 4, LIFE - 1, OVERWORLD);
        assertEquals(0, result.damage());
        assertFalse(result.decayed());
        assertEquals(LIFE - 1, result.state().lastDecayUpdate());
    }

    /** The damage: the level's damage per item, times the stack size, on the tick the life has passed. */
    @Test
    void afterTheLifeTheStackDamagesAndDecays() {
        RadiationState start = new RadiationState(0L, 0L, OVERWORLD);
        long now = RadiationEnum.radioactive.getLife() + 1;
        RadiationDecay.Result result = RadiationDecay.step(start, RadiationEnum.radioactive, 3, now, OVERWORLD);
        assertEquals(6 * 3, result.damage());
        assertTrue(result.decayed());
        assertEquals(now, result.state().decayStarted(), "the new element starts its decay now");
    }

    @Test
    void movingDimensionDoesNoDamageAndKeepsTheClock() {
        RadiationState start = new RadiationState(0L, 0L, OVERWORLD);
        RadiationDecay.Result result = RadiationDecay.step(start, RadiationEnum.extremelyRadioactive, 1, LIFE * 10, "minecraft:the_nether");
        assertEquals(0, result.damage());
        assertFalse(result.decayed());
        assertEquals("minecraft:the_nether", result.state().dimension());
        assertEquals(0L, result.state().decayStarted());
    }

    @Test
    void anEmptyStackOnlyMovesItsClock() {
        RadiationState start = new RadiationState(0L, 100L, OVERWORLD);
        RadiationDecay.Result result = RadiationDecay.step(start, RadiationEnum.hardlyRadioactive, 0, 400L, OVERWORLD);
        assertEquals(300L, result.state().decayStarted());
        assertEquals(400L, result.state().lastDecayUpdate());
        assertEquals(0, result.damage());
    }

    @Test
    void stableStacksAreLeftAlone() {
        RadiationState start = new RadiationState(0L, 0L, OVERWORLD);
        RadiationDecay.Result result = RadiationDecay.step(start, RadiationEnum.stable, 1, 10L, "minecraft:the_end");
        assertEquals(start, result.state());
        assertEquals(0, result.damage());
        assertFalse(result.decayed());
    }
}
