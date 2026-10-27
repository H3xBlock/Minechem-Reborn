package minechem.radiation;

import net.minecraft.ChatFormatting;

/**
 * How radioactive a chemical is. Each level has a half-life in ticks and the damage one item does per tick, multiplied by the
 * stack size.
 *
 * <p>The half-life multiplier is not configurable yet: it is fixed at 100 (no change).
 */
public enum RadiationEnum {
    stable(0, 0, ChatFormatting.GRAY),
    hardlyRadioactive(Ticks.DAY, 1, ChatFormatting.GREEN),
    slightlyRadioactive(Ticks.HOUR * 12, 2, ChatFormatting.DARK_GREEN),
    radioactive(Ticks.HOUR * 6, 6, ChatFormatting.YELLOW),
    highlyRadioactive(Ticks.HOUR * 3, 8, ChatFormatting.GOLD),
    extremelyRadioactive(Ticks.HOUR, 16, ChatFormatting.DARK_RED);

    /** Half-life multiplier in percent; 100 keeps the half-lives. */
    static final int HALF_LIFE_MULTIPLIER = 100;

    private final long life;
    private final int damage;
    private final ChatFormatting colour;

    RadiationEnum(long life, int damage, ChatFormatting colour) {
        this.life = life;
        this.damage = damage;
        this.colour = colour;
    }

    /** The colour of the level in the item tooltip. */
    public ChatFormatting getColour() {
        return colour;
    }

    /** The life in ticks: the level's half-life scaled by the multiplier. */
    public long getLife() {
        return (long) (life * (HALF_LIFE_MULTIPLIER / 100F));
    }

    /** The damage per unit of the stack. */
    public int getDamage() {
        return damage;
    }

    /** Minecraft ticks (20 per second). */
    static final class Ticks {
        static final int MINUTE = 20 * 60;
        static final int HOUR = MINUTE * 60;
        static final int DAY = HOUR * 24;

        private Ticks() {
        }
    }
}
