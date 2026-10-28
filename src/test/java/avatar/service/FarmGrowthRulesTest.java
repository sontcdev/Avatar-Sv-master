package avatar.service;

import org.junit.Test;
import static org.junit.Assert.*;

public class FarmGrowthRulesTest {
    @Test public void durationBandsMatchDonor() {
        assertEquals(60, FarmGrowthRules.waterIntervalMinutes(359));
        assertEquals(90, FarmGrowthRules.waterIntervalMinutes(360));
        assertEquals(120, FarmGrowthRules.waterIntervalMinutes(720));
        assertEquals(300, FarmGrowthRules.waterIntervalMinutes(1440));
        assertEquals(30, FarmGrowthRules.pestIntervalMinutes(359));
        assertEquals(60, FarmGrowthRules.pestIntervalMinutes(360));
        assertEquals(90, FarmGrowthRules.pestIntervalMinutes(720));
        assertEquals(180, FarmGrowthRules.pestIntervalMinutes(1440));
    }
    @Test public void healthMatchesDonorStep() {
        assertEquals(100, FarmGrowthRules.nextHealth(99, false, false, false));
        assertEquals(45, FarmGrowthRules.nextHealth(40, false, false, false));
        assertEquals(97, FarmGrowthRules.nextHealth(100, true, true, true));
        assertEquals(40, FarmGrowthRules.nextHealth(40, true, true, true));
        assertTrue(FarmGrowthRules.conditionTriggered(9));
        assertFalse(FarmGrowthRules.conditionTriggered(10));
    }
    @Test public void neverAdvancePartialOrPastDeadline() {
        long start = 100000000L;
        long tick = 600000L;
        assertEquals(0, FarmGrowthRules.completeTicks(start,start,start+tick-1,40));
        assertEquals(3, FarmGrowthRules.completeTicks(start,start,start+3*tick,40));
        assertEquals(4, FarmGrowthRules.completeTicks(start,start,start+10*tick,40));
        assertEquals(0, FarmGrowthRules.completeTicks(start,start+4*tick,start+10*tick,40));
    }
}
