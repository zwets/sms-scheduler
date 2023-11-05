package it.zwets.sms.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import it.zwets.sms.scheduler.SmsSchedulerConfiguration.Constants;
import it.zwets.sms.scheduler.SmsSchedulerService.SmsStatus;
import it.zwets.sms.scheduler.util.Scheduler;
import it.zwets.sms.scheduler.util.Slot;

@SpringBootTest
class SmsSchedulerServiceTests {
    
    private static final Logger LOG = LoggerFactory.getLogger(SmsSchedulerService.class);

    @Autowired
    private SmsSchedulerService service;
    
    @Test
    void testNullClientThrows() {
        assertThrows(RuntimeException.class, 
                () -> service.scheduleSms(null, null, null, scheduleIn(10), "testNullClientFails"));
    }
    
    @Test
    void testNullScheduleThrows() {
        assertThrows(RuntimeException.class, 
                () -> service.scheduleSms(null, null, null, null, "testNullScheduleFails"));
    }
    
    @Test
    void testNullPayloadThrows() {
        assertThrows(RuntimeException.class, 
                () -> service.scheduleSms(null, null, null, scheduleIn(10), null));
    }
    
    @Test
    void testBasics() {
        SmsStatus s = service.scheduleSms("client", "target", "key", scheduleIn(10), "testBasics");
        
        String id = s.id();
        assertNotNull(id);
        
        assertEquals(Constants.SMS_STATUS_NEW, s.status());
        assertEquals("client", s.client());
        assertEquals("target", s.target());
        assertEquals("key", s.key());
        assertNotNull(s.started());
        assertNull(s.ended());
        assertEquals(0, s.retries());
        
        SmsStatus r = service.getSmsStatus(id);
        assertNotNull(r);
        
        assertEquals(Constants.SMS_STATUS_SCHEDULED, r.status());
        assertEquals(s.id(), r.id());
        assertEquals(s.client(), r.client());
        assertEquals(s.target(), r.target());
        assertEquals(s.key(), r.key());
        assertEquals(s.started(), r.started());
        assertEquals(s.ended(), r.ended());
        assertEquals(s.retries(), r.retries());
        
        service.deleteInstance(id);
        assertNull(service.getSmsStatus(id));
    }

    @Test
    void testImmediate() {
        SmsStatus s = service.scheduleSms("client", null, null, scheduleNow(), "testImmediate");
        
        String id = s.id();
        assertNotNull(id);
        assertEquals("NEW", s.status());
        
        service.deleteInstance(id);
        assertNull(service.getSmsStatus(id));
    }

    @Test
    void testImmediateWithWait() throws Exception {
        SmsStatus s = service.scheduleSms("client", null, null, scheduleNow(), "testImmediateWithWait");
        
        String id = s.id();
        assertNotNull(id);
        
        LOG.debug("Waiting for executor");
        Thread.sleep(3000);

        SmsStatus r = service.getSmsStatus(id);
        assertNotNull(r);
        assertEquals(Constants.SMS_STATUS_ENROUTE, r.status());

        service.deleteInstance(id);
        assertNull(service.getSmsStatus(id));
    }

    @Test
    void testDelayedWithWait() throws Exception {
        SmsStatus s = service.scheduleSms("client", null, null, scheduleIn(1), "testDelayedWithWait");
        
        String id = s.id();
        assertNotNull(id);

        s = service.getSmsStatus(id);
        assertNotNull(s);
        assertEquals(Constants.SMS_STATUS_SCHEDULED, s.status());
        
        LOG.debug("Waiting for executor");
        Thread.sleep(4000);

        s = service.getSmsStatus(id);
        assertNotNull(s);
        assertEquals(Constants.SMS_STATUS_ENROUTE, s.status());

        service.deleteInstance(id);
        assertNull(service.getSmsStatus(id));
    }

    @Test
    void testCancelSms() {
//        fail("Not yet implemented");
    }

    @Test
    void testCancelAllForClient() {
//        fail("Not yet implemented");
    }

    @Test
    void testCancelAllForTarget() {
//        fail("Not yet implemented");
    }

    @Test
    void testGetSmsStatus() {
//        fail("Not yet implemented");
    }

    @Test
    void testGetStatusList() {
//        fail("Not yet implemented");
    }

    @Test
    void testGetStatusListString() {
//        fail("Not yet implemented");
    }

    @Test
    void testGetStatusListByTarget() {
//        fail("Not yet implemented");
    }

    @Test
    void testGetStatusListByKey() {
//        fail("Not yet implemented");
    }

    /* Return schedule string starting now and ending in 3 seconds. */
    private String scheduleNow() {
        return scheduleInFor(0, 3);
    }

    /* Return schedule string starting in seconds seconds and ending 3 seconds later. */
    private String scheduleIn(int seconds) {
        return scheduleInFor(seconds, 3);
    }
    
    /* Return schedule string starting secondsAfternow and ending secondsLength after that. */
    private String scheduleInFor(int secondsAfterNow, int secondsLength) {
        
        Instant due = Instant.now().plusSeconds(secondsAfterNow);
        Instant till = due.plusSeconds(secondsLength);
        
        return new Scheduler(new Slot[] { new Slot(due.getEpochSecond(), till.getEpochSecond()) }).toString();
    }    
}
