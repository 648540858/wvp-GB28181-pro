package com.genersoft.iot.vmp.gb28181.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SSRCFactoryTest {

    private SSRCFactory ssrcFactory;

    private static final String DOMAIN_PART = "20000";
    private static final String SERVER_ID = "test-server";

    @BeforeEach
    void setUp() throws Exception {
        ssrcFactory = new SSRCFactory();
        ReflectionTestUtils.setField(ssrcFactory, "domainPart", DOMAIN_PART);

        Field schedulerField = SSRCFactory.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        java.util.concurrent.ScheduledExecutorService scheduler =
                (java.util.concurrent.ScheduledExecutorService) schedulerField.get(ssrcFactory);
        scheduler.shutdownNow();
    }

    @Test
    void getPlaySsrc_shouldReturnCorrectFormat() {
        String ssrc = ssrcFactory.getPlaySsrc(SERVER_ID);
        assertNotNull(ssrc);
        assertEquals(10, ssrc.length(), "SSRC should be 10 characters: prefix(1) + domain(5) + seq(4)");
        assertTrue(ssrc.startsWith("0"), "Play SSRC should start with '0'");
        assertTrue(ssrc.substring(1).startsWith(DOMAIN_PART), "SSRC should contain domain part");
        assertTrue(ssrc.matches("0" + DOMAIN_PART + "\\d{4}"), "SSRC format: 0" + DOMAIN_PART + "NNNN");
    }

    @Test
    void getPlayBackSsrc_shouldReturnCorrectFormat() {
        String ssrc = ssrcFactory.getPlayBackSsrc(SERVER_ID);
        assertNotNull(ssrc);
        assertEquals(10, ssrc.length(), "SSRC should be 10 characters: prefix(1) + domain(5) + seq(4)");
        assertTrue(ssrc.startsWith("1"), "PlayBack SSRC should start with '1'");
        assertTrue(ssrc.substring(1).startsWith(DOMAIN_PART), "SSRC should contain domain part");
        assertTrue(ssrc.matches("1" + DOMAIN_PART + "\\d{4}"), "SSRC format: 1" + DOMAIN_PART + "NNNN");
    }

    @Test
    void allocations_withinSameServer_shouldBeUnique() {
        Set<String> allocated = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String ssrc = ssrcFactory.getPlaySsrc(SERVER_ID);
            assertNotNull(ssrc, "Should allocate SSRC #" + i);
            assertTrue(allocated.add(ssrc), "SSRC should be unique: " + ssrc);
        }
        assertEquals(1000, allocated.size());
    }

    @Test
    void allocations_forDifferentServers_shouldBeIndependent() {
        String serverA = "server-a";
        String serverB = "server-b";

        for (int i = 0; i < 10000; i++) {
            assertNotNull(ssrcFactory.getPlaySsrc(serverA), "Server A should allocate SSRC #" + i);
        }
        assertNull(ssrcFactory.getPlaySsrc(serverA), "Server A should be exhausted");

        for (int i = 0; i < 1000; i++) {
            assertNotNull(ssrcFactory.getPlaySsrc(serverB), "Server B should allocate SSRC #" + i);
        }
    }

    @Test
    void exhaustion_shouldReturnNull() {
        for (int i = 0; i < 10000; i++) {
            assertNotNull(ssrcFactory.getPlaySsrc(SERVER_ID), "iteration " + i);
        }
        assertNull(ssrcFactory.getPlaySsrc(SERVER_ID), "Should return null when exhausted");
        assertNull(ssrcFactory.getPlayBackSsrc(SERVER_ID), "Should return null for PlayBack too");
    }

    @Test
    @Disabled("Needs mocked mediaServerService for ZLM query")
    void rebuild_shouldResetUsage() {
        for (int i = 0; i < 500; i++) {
            ssrcFactory.getPlaySsrc(SERVER_ID);
        }
        ssrcFactory.rebuild();

        for (int i = 0; i < 500; i++) {
            String ssrc = ssrcFactory.getPlaySsrc(SERVER_ID);
            assertNotNull(ssrc, "After rebuild should allocate SSRC #" + i);
        }
    }

    @Test
    void allocateAll_shouldUseAll10000Slots() {
        Set<String> allocated = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            String ssrc = ssrcFactory.getPlaySsrc(SERVER_ID);
            assertNotNull(ssrc, "Should allocate at iteration " + i);
            allocated.add(ssrc);
        }
        assertEquals(10000, allocated.size(), "All 10000 slots should be unique");
    }

    @Test
    void twoPrefixes_shareSamePool() throws Exception {
        for (int i = 0; i < 5000; i++) {
            assertNotNull(ssrcFactory.getPlaySsrc(SERVER_ID), "play #" + i);
            assertNotNull(ssrcFactory.getPlayBackSsrc(SERVER_ID), "playback #" + i);
        }

        Field usedMapField = SSRCFactory.class.getDeclaredField("usedMap");
        usedMapField.setAccessible(true);
        java.util.concurrent.ConcurrentHashMap<String, java.util.BitSet> usedMap =
                (java.util.concurrent.ConcurrentHashMap<String, java.util.BitSet>) usedMapField.get(ssrcFactory);
        java.util.BitSet bits = usedMap.get(SERVER_ID);
        assertNotNull(bits);
        assertEquals(10000, bits.cardinality(), "All 10000 bits should be set");
    }

    @Test
    void multipleServers_shouldNotAffectEachOther() {
        String server1 = "server-1";
        String server2 = "server-2";
        String server3 = "server-3";

        for (int i = 0; i < 10000; i++) {
            ssrcFactory.getPlaySsrc(server1);
        }
        assertNull(ssrcFactory.getPlaySsrc(server1));

        assertNotNull(ssrcFactory.getPlaySsrc(server2));
        assertNotNull(ssrcFactory.getPlaySsrc(server3));

        for (int i = 0; i < 100; i++) {
            ssrcFactory.getPlaySsrc(server2);
            ssrcFactory.getPlaySsrc(server3);
        }
        assertNull(ssrcFactory.getPlaySsrc(server1));
    }

    @Test
    void linearProbe_skipsUsedSlots() throws Exception {
        Field usedMapField = SSRCFactory.class.getDeclaredField("usedMap");
        usedMapField.setAccessible(true);
        java.util.concurrent.ConcurrentHashMap<String, java.util.BitSet> usedMap =
                (java.util.concurrent.ConcurrentHashMap<String, java.util.BitSet>) usedMapField.get(ssrcFactory);
        java.util.BitSet bits = new java.util.BitSet(10000);
        for (int i = 0; i < 100; i++) {
            bits.set(i);
        }
        usedMap.put(SERVER_ID, bits);

        String ssrc = ssrcFactory.getPlaySsrc(SERVER_ID);
        assertNotNull(ssrc, "Should find a free slot via linear probe");
        int suffix = Integer.parseInt(ssrc.substring(6));
        assertTrue(suffix >= 100, "Should skip used slots 0-99, got suffix " + suffix);
    }

    @Test
    void ssrc_shouldBeDifferentEachCall() {
        Set<String> results = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            results.add(ssrcFactory.getPlaySsrc(SERVER_ID));
        }
        assertEquals(100, results.size(), "All 100 calls should return different SSRCs");
    }
}
