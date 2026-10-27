package minechem.radiation;

/**
 * The decay rule of one radioactive stack. It is a pure function of the stack's state and the world time, so it is tested
 * without a world.
 */
public final class RadiationDecay {

    /** What one update did: the new state, the damage the stack does now, and whether it decayed into the next element. */
    public record Result(RadiationState state, int damage, boolean decayed) {
    }

    private RadiationDecay() {
    }

    /**
     * One update of a stack with {@code count} items at world time {@code now}, in dimension {@code dimension}. A stable element
     * is not radioactive and is left alone. A stack with no state starts its decay now. A stack that moved dimension gets the
     * new dimension and no damage. An empty stack only moves its clock on. Otherwise, when the level's life has passed, the stack
     * does its damage and decays (the caller changes the element); if not, nothing happens but the clock moves on.
     */
    /**
     * The damage left after a shield: the damage times (1 - reduction), never below zero, rounded to the nearest whole point.
     */
    public static int reduce(int damage, float reduction) {
        float left = Math.max(0F, 1F - reduction);
        return Math.round(damage * left);
    }

    public static Result step(RadiationState state, RadiationEnum level, int count, long now, String dimension) {
        if (level == RadiationEnum.stable) {
            return new Result(state, 0, false);
        }
        if (state == null) {
            return new Result(new RadiationState(now, now, dimension), 0, false);
        }
        if (!dimension.equals(state.dimension())) {
            return new Result(new RadiationState(state.decayStarted(), state.lastDecayUpdate(), dimension), 0, false);
        }
        if (count == 0) {
            long started = state.decayStarted() + (now - state.lastDecayUpdate());
            return new Result(new RadiationState(started, now, state.dimension()), 0, false);
        }
        long lifeTime = now - state.decayStarted() - level.getLife();
        if (lifeTime > 0) {
            return new Result(new RadiationState(now, now, dimension), level.getDamage() * count, true);
        }
        return new Result(new RadiationState(state.decayStarted(), now, state.dimension()), 0, false);
    }
}
