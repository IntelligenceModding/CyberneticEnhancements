package de.artemis.cyberneticenhancements.common.quest;

import net.minecraft.core.BlockPos;
import org.junit.Test;

import static org.junit.Assert.*;

public class InvestigationSurveyTest {
    @Test
    public void separatedBearingsIntersectAtTheSameSourceAfterReload() {
        var source = new BlockPos(40, 73, 40);
        var survey = new InvestigationSurvey(BlockPos.ZERO, source);
        assertTrue(survey.record(new BlockPos(-50, 60, 0), 1));
        survey = InvestigationSurvey.fromTag(survey.toTag());
        assertEquals(1, survey.count());
        assertTrue(survey.record(new BlockPos(50, 100, -30), 1));
        assertFalse(survey.located());
        assertTrue(survey.record(new BlockPos(0, -20, 70), 1));
        assertTrue(survey.located());
        assertEquals(source, survey.triangulatedPosition());
        assertEquals(source, InvestigationSurvey.fromTag(survey.toTag()).triangulatedPosition());
        assertFalse(survey.record(new BlockPos(-100, 80, 30), 1));
    }

    @Test
    public void nearbyParallelAndOutOfRangeReadingsCannotAdvanceTheSurvey() {
        var survey = new InvestigationSurvey(BlockPos.ZERO, new BlockPos(100, 70, 0));
        assertTrue(survey.record(BlockPos.ZERO, 1));
        assertEquals("separation", survey.rejection(new BlockPos(10, 100, 10), 1));
        assertEquals("angle", survey.rejection(new BlockPos(-80, 70, 0), 1));
        assertEquals("outside", survey.rejection(new BlockPos(161, 70, 0), 1));
        assertEquals("near_source", survey.rejection(new BlockPos(100, 70, 0), 1));
        assertFalse(survey.record(new BlockPos(-80, 70, 0), 1));
        assertEquals(1, survey.count());
        assertEquals("separation", survey.rejection(new BlockPos(0, 70, 40), 3));
        assertEquals("", survey.rejection(new BlockPos(0, 70, 40), 1));
    }
}
