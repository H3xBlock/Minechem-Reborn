package minechem.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RadiationRulesTest {
    @Test
    void elementLevelsFollowTheTable() {
        assertEquals(RadiationEnum.stable, RadiationRules.ofElement("H"));
        assertEquals(RadiationEnum.hardlyRadioactive, RadiationRules.ofElement("Tc"));
        assertEquals(RadiationEnum.radioactive, RadiationRules.ofElement("Po"));
        assertEquals(RadiationEnum.highlyRadioactive, RadiationRules.ofElement("At"));
    }

    @Test
    void levelsHaveLifeAndDamage() {
        assertEquals(1728000L, RadiationEnum.hardlyRadioactive.getLife());
        assertEquals(432000L, RadiationEnum.radioactive.getLife());
        assertEquals(16, RadiationEnum.extremelyRadioactive.getDamage());
    }

    /** The shortest life wins: radioactive (6 hours) over hardly radioactive (a day). */
    @Test
    void moleculeTakesTheShortestLivedComponent() {
        assertEquals(RadiationEnum.radioactive, RadiationRules.ofFormula("TcPo"));
        assertEquals(RadiationEnum.stable, RadiationRules.ofFormula("H2O"));
    }
}
