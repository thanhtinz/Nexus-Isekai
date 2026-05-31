package com.nexusisekai;

import com.nexusisekai.game.service.PvpService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PvpServiceTest {

    @Test
    void equalEloWinnerGainsLoserLoses() {
        int[] result = PvpService.calculateElo(1000, 1000);
        assertTrue(result[0] > 1000, "Winner should gain ELO");
        assertTrue(result[1] < 1000, "Loser should lose ELO");
        // Đối xứng: tổng thay đổi gần bằng 0
        assertEquals(16, result[0] - 1000);
        assertEquals(-16, result[1] - 1000);
    }

    @Test
    void underdogWinGainsMore() {
        int[] upset = PvpService.calculateElo(800, 1200);   // yếu thắng mạnh
        int[] expected = PvpService.calculateElo(1200, 800); // mạnh thắng yếu
        int upsetGain = upset[0] - 800;
        int expectedGain = expected[0] - 1200;
        assertTrue(upsetGain > expectedGain, "Underdog win should gain more ELO");
    }

    @Test
    void eloNeverNegative() {
        int[] result = PvpService.calculateElo(2400, 10);
        assertTrue(result[1] >= 0, "ELO should never go negative");
    }

    @Test
    void tierBoundaries() {
        assertEquals("Bronze", PvpService.tierFromElo(0));
        assertEquals("Bronze", PvpService.tierFromElo(899));
        assertEquals("Silver", PvpService.tierFromElo(900));
        assertEquals("Gold", PvpService.tierFromElo(1200));
        assertEquals("Grandmaster", PvpService.tierFromElo(2400));
        assertEquals("Grandmaster", PvpService.tierFromElo(9999));
    }
}
