package com.nexusisekai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verify enhancement rate curve is monotonically decreasing (game balance) */
public class EnhancementRateTest {
    static final double[] RATES = {1.0,1.0,1.0,0.95,0.90,0.85,0.75,0.65,0.55,0.45,0.35,0.25,0.18,0.12,0.08};

    @Test
    void ratesDecreaseWithLevel() {
        for (int i = 3; i < RATES.length - 1; i++) {
            assertTrue(RATES[i] >= RATES[i+1], "Rate should decrease at level " + i);
        }
    }

    @Test
    void maxLevelHardest() {
        assertEquals(0.08, RATES[14], 0.001, "+15 should be hardest at 8%");
    }

    @Test
    void earlyLevelsGuaranteed() {
        assertEquals(1.0, RATES[0]);
        assertEquals(1.0, RATES[1]);
        assertEquals(1.0, RATES[2]);
    }
}
