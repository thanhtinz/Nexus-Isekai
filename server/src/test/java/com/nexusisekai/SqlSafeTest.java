package com.nexusisekai;

import com.nexusisekai.database.SqlSafe;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class SqlSafeTest {

    @Test
    void identWhitelistAllowsValid() {
        Set<String> allowed = Set.of("level", "pvp_rating", "wealth");
        assertEquals("level", SqlSafe.ident("level", allowed, "level"));
        assertEquals("wealth", SqlSafe.ident("wealth", allowed, "level"));
    }

    @Test
    void identWhitelistBlocksInjection() {
        Set<String> allowed = Set.of("level", "pvp_rating");
        // SQL injection attempt → fallback
        assertEquals("level", SqlSafe.ident("level; DROP TABLE characters;--", allowed, "level"));
        assertEquals("level", SqlSafe.ident("1 OR 1=1", allowed, "level"));
    }
}
