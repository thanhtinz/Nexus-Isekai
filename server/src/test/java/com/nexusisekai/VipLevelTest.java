package com.nexusisekai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VipLevelTest {
    static final long[] T = {0,100000,500000,1000000,3000000,5000000,10000000,20000000,50000000,100000000};

    static int vipFor(long total) {
        int vip = 0;
        for (int i = T.length - 1; i >= 0; i--) if (total >= T[i]) { vip = i; break; }
        return vip;
    }

    @Test void noTopupVip0() { assertEquals(0, vipFor(0)); }
    @Test void smallTopupVip1() { assertEquals(1, vipFor(100000)); assertEquals(1, vipFor(499999)); }
    @Test void midTopupVip3() { assertEquals(3, vipFor(1000000)); }
    @Test void maxVip9() { assertEquals(9, vipFor(100000000)); assertEquals(9, vipFor(999999999)); }
    @Test void monotonic() { for (int i=0;i<T.length-1;i++) assertTrue(vipFor(T[i]) < vipFor(T[i+1])); }
}
