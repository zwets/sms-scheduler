package it.zwets.sms.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
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
    
    private static final Logger LOG = LoggerFactory.getLogger(SmsSchedulerServiceTests.class);

    private static String CLIENT = "client";
    private static String NOT_CLIENT = "not-client";
    
    @Autowired
    private SmsSchedulerService service;
    
    @AfterEach
    void afterEach() {
        service.deleteAllForClient(CLIENT);
        service.deleteAllForClient(NOT_CLIENT);
        assertEquals(0, service.getStatusList().size());
    }
    
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
        SmsStatus s = service.scheduleSms(CLIENT, "target", "key", scheduleIn(10), "testBasics");
        
        String id = s.id();
        assertNotNull(id);
        
        assertEquals(Constants.SMS_STATUS_NEW, s.status());
        assertEquals(CLIENT, s.client());
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
        SmsStatus s = service.scheduleSms(CLIENT, null, null, scheduleNow(), "testImmediate");
        
        String id = s.id();
        assertNotNull(id);
        assertEquals("NEW", s.status());
        
        service.deleteInstance(id);
        assertNull(service.getSmsStatus(id));
    }

    @Test
    void testImmediateWithWait() {
        SmsStatus s = service.scheduleSms(CLIENT, null, null, scheduleNow(), "testImmediateWithWait");
        
        String id = s.id();
        assertNotNull(id);
        
        waitForAsync(1);

        SmsStatus r = service.getSmsStatus(id);
        assertNotNull(r);
        assertEquals(Constants.SMS_STATUS_ENROUTE, r.status());

        service.deleteInstance(id);
        assertNull(service.getSmsStatus(id));
    }

    @Test
    void testDelayedWithWait() {
        SmsStatus s = service.scheduleSms(CLIENT, null, null, scheduleIn(1), "testDelayedWithWait");
        
        String id = s.id();
        assertNotNull(id);

        s = service.getSmsStatus(id);
        assertNotNull(s);
        assertEquals(Constants.SMS_STATUS_SCHEDULED, s.status());
        
        waitForAsync(2);

        s = service.getSmsStatus(id);
        assertNotNull(s);
        assertEquals(Constants.SMS_STATUS_ENROUTE, s.status());

        service.deleteInstance(id);
        assertNull(service.getSmsStatus(id));
    }

    @Test
    void testCancelSms() {
        SmsStatus s = service.scheduleSms(CLIENT, null, null, scheduleIn(5), "testCancelSms");
        
        String id = s.id();
        assertNotNull(id);

        service.cancelSms(id);
       
        s = service.getSmsStatus(id);
        assertNotNull(s);
        assertEquals(Constants.SMS_STATUS_CANCELED, s.status());

        service.deleteInstance(id);
        assertNull(service.getSmsStatus(id));
    }

    @Test
    void testCancelByClientKey() {
        
        final String KEY = "cancel-key";
        final String NO_KEY = "not-key";
        
        String id0 = service.scheduleSms(NOT_CLIENT, null, KEY, scheduleIn(5), "testCancelAllForTarget0").id();
        String id1 = service.scheduleSms(CLIENT, null, KEY, scheduleIn(5), "testCancelByClientKey1").id();
        String id2 = service.scheduleSms(CLIENT, null, NO_KEY, scheduleIn(5), "testCancelByClientKey2").id();
        
        service.cancelByClientKey(CLIENT, KEY);
       
        assertEquals(Constants.SMS_STATUS_SCHEDULED, service.getSmsStatus(id0).status());
        assertEquals(Constants.SMS_STATUS_CANCELED, service.getSmsStatus(id1).status());
        assertEquals(Constants.SMS_STATUS_SCHEDULED, service.getSmsStatus(id2).status());
    }

    @Test
    void testCancelAllForTarget() {
        
        final String TARGET = "cancel-target";
        final String NO_TARGET = "not-target";
        
        String id0 = service.scheduleSms(NOT_CLIENT, TARGET, null, scheduleIn(5), "testCancelAllForTarget0").id();
        String id1 = service.scheduleSms(CLIENT, TARGET, null, scheduleIn(5), "testCancelAllForTarget1").id();
        String id2 = service.scheduleSms(CLIENT, NO_TARGET, null, scheduleIn(5), "testCancelAllForTarget2").id();
        
        service.cancelAllForTarget(CLIENT, TARGET);
       
        assertEquals(Constants.SMS_STATUS_SCHEDULED, service.getSmsStatus(id0).status());
        assertEquals(Constants.SMS_STATUS_CANCELED, service.getSmsStatus(id1).status());
        assertEquals(Constants.SMS_STATUS_SCHEDULED, service.getSmsStatus(id2).status());
    }

    @Test
    void testCancelAllForClient() {
        
        String id0 = service.scheduleSms(NOT_CLIENT, null, null, scheduleIn(5), "testCancelAllForClient0").id();
        String id1 = service.scheduleSms(CLIENT, null, null, scheduleIn(5), "testCancelAllForClient1").id();
        String id2 = service.scheduleSms(CLIENT, null, null, scheduleIn(5), "testCancelAllForClient2").id();
        String id3 = service.scheduleSms(CLIENT, null, null, scheduleIn(5), "testCancelAllForClient2").id();
        
        service.cancelAllForClient(CLIENT);
       
        assertEquals(Constants.SMS_STATUS_SCHEDULED, service.getSmsStatus(id0).status());
        assertEquals(Constants.SMS_STATUS_CANCELED, service.getSmsStatus(id1).status());
        assertEquals(Constants.SMS_STATUS_CANCELED, service.getSmsStatus(id2).status());
        assertEquals(Constants.SMS_STATUS_CANCELED, service.getSmsStatus(id3).status());
    }

    @Test
    void testGetStatusList() {
        
        service.scheduleSms(NOT_CLIENT, null, null, scheduleIn(5), "testCancelAllForClient0").id();
        service.scheduleSms(CLIENT, null, null, scheduleIn(5), "testCancelAllForClient1").id();
        assertEquals(2, service.getStatusList().size());
    }

    @Test
    void testGetStatusListByClient() {
        
        service.scheduleSms(NOT_CLIENT, null, null, scheduleIn(5), "testCancelAllForTarget0").id();
        service.scheduleSms(CLIENT, null, null, scheduleIn(5), "testCancelByClientKey1").id();
        service.scheduleSms(CLIENT, null, null, scheduleIn(5), "testCancelByClientKey2").id();
        
        assertEquals(3, service.getStatusList().size());
        assertEquals(2, service.getStatusList(CLIENT).size());
    }

    @Test
    void testGetStatusListByTarget() {
        
        final String TARGET = "the-target";
        final String NO_TARGET = "not-target";
        
        service.scheduleSms(NOT_CLIENT, TARGET, null, scheduleIn(5), "testCancelAllForTarget0").id();
        service.scheduleSms(CLIENT, TARGET, null, scheduleIn(5), "testCancelAllForTarget1").id();
        service.scheduleSms(CLIENT, NO_TARGET, null, scheduleIn(5), "testCancelAllForTarget2").id();
        
        assertEquals(1, service.getStatusListByTarget(CLIENT, TARGET).size());
    }

    @Test
    void testGetStatusListByKey() {

        final String KEY = "cancel-key";
        final String NO_KEY = "not-key";
        
        service.scheduleSms(NOT_CLIENT, null, KEY, scheduleIn(5), "testCancelAllForTarget0").id();
        service.scheduleSms(CLIENT, null, KEY, scheduleIn(5), "testCancelByClientKey1").id();
        service.scheduleSms(CLIENT, null, NO_KEY, scheduleIn(5), "testCancelByClientKey2").id();
        
        assertEquals(1, service.getStatusListByClientKey(CLIENT, KEY).size());
    }

    // Helpers ------------------------------------------------------------------------------------
    
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
    
    private void waitForAsync(double seconds) {
        LOG.debug("waiting for job executor {}s", seconds);

        try {
            Thread.sleep(Math.round(1000 * seconds));
        }
        catch (InterruptedException e) {
            // ignore
        }
    }
}
