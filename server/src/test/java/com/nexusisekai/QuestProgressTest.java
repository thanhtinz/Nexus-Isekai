package com.nexusisekai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verify quest progress logic (capping at target, completion) */
public class QuestProgressTest {

    static String computeStatus(int progress, int amount, int target) {
        int newProgress = Math.min(target, progress + amount);
        return newProgress >= target ? "completed" : "in_progress";
    }
    static int computeProgress(int progress, int amount, int target) {
        return Math.min(target, progress + amount);
    }

    @Test
    void progressCapsAtTarget() {
        assertEquals(10, computeProgress(8, 5, 10), "Progress should cap at target");
    }

    @Test
    void completesWhenReachingTarget() {
        assertEquals("completed", computeStatus(9, 1, 10));
        assertEquals("in_progress", computeStatus(5, 2, 10));
    }

    @Test
    void overflowStillCompletes() {
        assertEquals("completed", computeStatus(5, 100, 10));
        assertEquals(10, computeProgress(5, 100, 10));
    }
}
