package com.nexusisekai;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class SafeCrudTest {
    static final Set<String> ALLOWED = Set.of("items","classes","quests","shop_items");

    static boolean tableAllowed(String t) { return ALLOWED.contains(t); }
    static boolean colSafe(String col) { return col.matches("[a-zA-Z_]+"); }

    @Test void allowedTablePasses() { assertTrue(tableAllowed("items")); }
    @Test void injectionTableBlocked() {
        assertFalse(tableAllowed("items; DROP TABLE accounts;--"));
        assertFalse(tableAllowed("users UNION SELECT password"));
    }
    @Test void safeColumnPasses() { assertTrue(colSafe("display_name")); assertTrue(colSafe("price_vnd")); }
    @Test void injectionColumnBlocked() {
        assertFalse(colSafe("name=1; DROP"));
        assertFalse(colSafe("1 OR 1=1"));
        assertFalse(colSafe("col`; DELETE"));
    }
}
