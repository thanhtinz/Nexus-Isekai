package com.nexusisekai;

import com.nexusisekai.game.service.AntiCheatService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AntiCheatServiceTest {

    @Test
    void normalMovementPasses() {
        long charId = 1001;
        assertTrue(AntiCheatService.validateMovement(charId, 0, 0));
        // di chuyển hợp lý
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        assertTrue(AntiCheatService.validateMovement(charId, 0.5, 0.5));
        AntiCheatService.cleanup(charId);
    }

    @Test
    void teleportHackBlocked() {
        long charId = 1002;
        AntiCheatService.validateMovement(charId, 0, 0);
        // nhảy 100 tiles ngay lập tức = teleport hack
        assertFalse(AntiCheatService.validateMovement(charId, 100, 100));
        AntiCheatService.cleanup(charId);
    }

    @Test
    void damageHackBlocked() {
        assertTrue(AntiCheatService.validateDamage(2001, 250, 100));  // 2.5x OK
        assertFalse(AntiCheatService.validateDamage(2001, 500, 100)); // 5x hack
    }

    @Test
    void packetFloodBlocked() {
        long charId = 3001;
        boolean blocked = false;
        for (int i = 0; i < 150; i++) {
            if (!AntiCheatService.validatePacketRate(charId)) { blocked = true; break; }
        }
        assertTrue(blocked, "Should block after flood limit");
        AntiCheatService.cleanup(charId);
    }
}
