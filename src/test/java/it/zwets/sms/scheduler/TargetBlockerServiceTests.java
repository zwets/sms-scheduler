package it.zwets.sms.scheduler;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TargetBlockerServiceTests {
   
    private static final String CLIENT = "some-client";
    private static final String TARGET = "the-target";
 
    @Autowired
    private TargetBlockerService service;

    @Test
    void testBasics() {
        service.blockTarget(CLIENT, TARGET);
        assertTrue(service.isTargetBlocked(CLIENT, TARGET));
        assertFalse(service.isTargetBlocked(CLIENT, "not-" + TARGET));
        assertFalse(service.isTargetBlocked("not-" + CLIENT, TARGET));
        service.unblockTarget(CLIENT, TARGET);
        assertFalse(service.isTargetBlocked(CLIENT, TARGET));
    }

    @Test
    void testBlockTwice() {
        service.blockTarget(CLIENT, TARGET);
        service.blockTarget(CLIENT, TARGET);
        assertTrue(service.isTargetBlocked(CLIENT, TARGET));
    }

    @Test
    void testUnblockTwice() {
        service.blockTarget(CLIENT, TARGET);
        service.unblockTarget(CLIENT, TARGET);
        assertFalse(service.isTargetBlocked(CLIENT, TARGET));
        service.unblockTarget(CLIENT, TARGET);
        assertFalse(service.isTargetBlocked(CLIENT, TARGET));
    }

}
