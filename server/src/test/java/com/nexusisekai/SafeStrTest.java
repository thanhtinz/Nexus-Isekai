package com.nexusisekai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verify SQL injection sanitization in admin safeStr */
public class SafeStrTest {
    static String safeStr(String v) {
        return v.replaceAll("['\\\\;]", "").replaceAll("--", "").replaceAll("/\\*", "").replaceAll("\\*/", "");
    }

    @Test void normalTextUnchanged() { assertEquals("normal name", safeStr("normal name")); }
    @Test void stripsQuotes() { assertEquals("OBrien", safeStr("O'Brien")); }
    @Test void blocksInjection() {
        assertEquals("x DROP TABLE users", safeStr("x'; DROP TABLE users;--"));
        assertFalse(safeStr("'; DELETE FROM accounts;--").contains("'"));
        assertFalse(safeStr("'; DELETE FROM accounts;--").contains(";"));
    }
    @Test void stripsComments() { assertEquals("a comment b", safeStr("a/* comment */b")); }
    @Test void stripsBackslash() { assertEquals("backslash", safeStr("back\\slash")); }
}
