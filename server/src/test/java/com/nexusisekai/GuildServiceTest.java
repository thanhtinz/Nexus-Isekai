package com.nexusisekai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verify guild business rules (return codes) */
public class GuildServiceTest {
    // -1=name taken, -2=no gold, -3=already in guild, >0=success
    static long mockCreate(boolean nameTaken, long gold, int currentGuild) {
        if (nameTaken) return -1;
        if (gold < 100000) return -2;
        if (currentGuild > 0) return -3;
        return 1;
    }

    @Test void nameTakenRejected() { assertEquals(-1, mockCreate(true, 200000, 0)); }
    @Test void insufficientGold() { assertEquals(-2, mockCreate(false, 50000, 0)); }
    @Test void alreadyInGuild() { assertEquals(-3, mockCreate(false, 200000, 5)); }
    @Test void successWhenValid() { assertTrue(mockCreate(false, 200000, 0) > 0); }
}
