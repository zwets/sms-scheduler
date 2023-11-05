package it.zwets.sms.scheduler;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TargetBlockerServiceTests {
    
    @Autowired
    private TargetBlockerService service;

    @Test
    void testBasics() {
        service.blockTarget("test-client", "test-target");
        assertTrue(service.isTargetBlocked("test-client", "test-target"));
        assertFalse(service.isTargetBlocked("test-client", "not-test-target"));
        assertFalse(service.isTargetBlocked("not-test-client", "test-target"));
        service.unblockTarget("test-client", "test-target");
        assertFalse(service.isTargetBlocked("test-client", "test-target"));
    }

    @Test
    void testBlockTwice() {
        service.blockTarget("test-client", "test-target");
        service.blockTarget("test-client", "test-target");
        assertTrue(service.isTargetBlocked("test-client", "test-target"));
    }

    @Test
    void testUnblockTwice() {
        service.blockTarget("test-client", "test-target");
        service.unblockTarget("test-client", "test-target");
        assertFalse(service.isTargetBlocked("test-client", "test-target"));
        service.unblockTarget("test-client", "test-target");
        assertFalse(service.isTargetBlocked("test-client", "test-target"));
    }

}
