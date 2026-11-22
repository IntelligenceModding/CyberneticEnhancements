package de.artemis.cyberneticenhancements.common.skills;

import org.junit.Test;

import static org.junit.Assert.*;

public class SkillRulesTest {
    @Test
    public void everyLevelGivesOnePointAndLevelsAreClamped() {
        assertEquals(1, Skills.pointsFor(0));
        assertEquals(1, Skills.pointsFor(1));
        assertEquals(50, Skills.pointsFor(50));
        assertEquals(100, Skills.pointsFor(1000));
    }

    @Test
    public void bonusesScaleLinearlyWithRankAndAreZeroAtRankZero() {
        for (SkillEffect effect : SkillEffect.values()) {
            assertEquals(effect.name(), 0.0D, effect.at(0), 1.0E-9D);
            assertEquals(effect.name(), effect.at(10) / 2.0D, effect.at(5), 1.0E-9D);
            assertEquals("held to the scale", effect.at(10), effect.at(99), 1.0E-9D);
            assertEquals("negative ranks give nothing", 0.0D, effect.at(-3), 1.0E-9D);
        }
    }

    @Test
    public void everyEffectBelongsToExactlyOneSkill() {
        for (SkillEffect effect : SkillEffect.values()) {
            int owners = 0;
            for (Skill skill : Skill.values()) if (skill.improves(effect)) owners++;
            assertEquals(effect.name(), 1, owners);
        }
    }

    @Test
    public void everySkillImprovesSomething() {
        for (Skill skill : Skill.values()) {
            boolean any = false;
            for (SkillEffect effect : SkillEffect.values()) any |= skill.improves(effect);
            assertTrue(skill.name(), any);
        }
    }

    @Test
    public void skillKeysAreUnique() {
        var keys = new java.util.HashSet<String>();
        for (Skill skill : Skill.values()) assertTrue(skill.name(), keys.add(skill.key()));
    }
}
