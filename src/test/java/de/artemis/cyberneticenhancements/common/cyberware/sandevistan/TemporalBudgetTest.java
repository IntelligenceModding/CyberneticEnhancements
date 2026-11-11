package de.artemis.cyberneticenhancements.common.cyberware.sandevistan;

import org.junit.Test;

import static org.junit.Assert.*;

public class TemporalBudgetTest {
    @Test
    public void everyModelAdvancesAtItsIntendedRateWithoutDrift() {
        for (SandevistanProfile profile : SandevistanProfile.DEFAULTS) {
            TemporalBudget budget = new TemporalBudget();
            int ticks = 0;
            for (int i = 0; i < 2000; i++) if (budget.advance(profile.timeScale())) ticks++;
            assertEquals(profile.id(), (int) (2000 * profile.timeScale()), ticks);
        }
    }

    @Test
    public void switchingOverlappingFieldsPreservesFractionalProgress() {
        TemporalBudget budget = new TemporalBudget();
        for (int i = 0; i < 5; i++) assertFalse(budget.advance(0.08));
        assertFalse(budget.advance(0.55));
        assertTrue(budget.advance(0.55));
        assertEquals(0.5F, budget.renderPartial(0, 0.55), 0.00001F);
        assertEquals(0.775F, budget.renderPartial(0.5F, 0.55), 0.00001F);
    }

    @Test
    public void InvalidConfigurationCannotFreezeEntities() {
        TemporalBudget budget = new TemporalBudget();
        assertTrue(budget.advance(Double.NaN));
        assertTrue(budget.advance(Double.POSITIVE_INFINITY));
        int ticks = 0;
        for (int i = 0; i < 100; i++) if (budget.advance(-100)) ticks++;
        assertEquals(5, ticks);
        assertTrue(budget.advance(1));
        assertEquals(0, budget.renderPartial(0, 1), 0);
    }
}
