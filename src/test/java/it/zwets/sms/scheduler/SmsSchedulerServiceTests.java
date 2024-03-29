package it.zwets.sms.scheduler;

import static it.zwets.sms.scheduler.SmsSchedulerConfiguration.Constants.SMS_STATUS_CANCELED;
import static it.zwets.sms.scheduler.SmsSchedulerConfiguration.Constants.SMS_STATUS_ENROUTE;
import static it.zwets.sms.scheduler.SmsSchedulerConfiguration.Constants.SMS_STATUS_NEW;
import static it.zwets.sms.scheduler.SmsSchedulerConfiguration.Constants.SMS_STATUS_SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import it.zwets.sms.scheduler.SmsSchedulerService.SmsStatus;
import it.zwets.sms.scheduler.util.Scheduler;
import it.zwets.sms.scheduler.util.Slot;

@SpringBootTest
class SmsSchedulerServiceTests {
    
    private static final Logger LOG = LoggerFactory.getLogger(SmsSchedulerServiceTests.class);

    private static String CLIENT = "test";
    private static String NOT_CLIENT = "not-test";
    
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
                () -> service.scheduleSms(null, null, null, null, scheduleIn(10), "testNullClientFails"));
    }
    
    @Test
    void testNullScheduleThrows() {
        assertThrows(RuntimeException.class, 
                () -> service.scheduleSms(null, null, null, null, null, "testNullScheduleFails"));
    }
    
    @Test
    void testNullPayloadThrows() {
        assertThrows(RuntimeException.class, 
                () -> service.scheduleSms(null, null, null, null, scheduleIn(10), null));
    }
    
    @Test
    void testBasics() {
        SmsStatus s = service.scheduleSms(CLIENT, "batch", "key", "target", scheduleIn(10), "testBasics");
        
        String id = s.id();
        assertNotNull(id);
        
        assertTrue(SMS_STATUS_NEW.equals(s.status()) || SMS_STATUS_SCHEDULED.equals(s.status()));
        assertEquals(CLIENT, s.client());
        assertEquals("batch", s.batch());
        assertEquals("key", s.key());
        assertEquals("target", s.target());
        assertNotNull(s.started());
        assertNull(s.ended());
        assertEquals(0, s.retries());
        
        SmsStatus r = service.getSmsStatus(id);
        assertNotNull(r);
        
        assertEquals(SMS_STATUS_SCHEDULED, r.status());
        assertEquals(s.id(), r.id());
        assertEquals(s.client(), r.client());
        assertEquals(s.batch(), r.batch());
        assertEquals(s.key(), r.key());
        assertEquals(s.target(), r.target());
        assertEquals(s.started(), r.started());
        assertEquals(s.ended(), r.ended());
        assertEquals(s.retries(), r.retries());
        
        service.deleteInstance(id);
        assertNull(service.getSmsStatus(id));
    }

    @Test
    void testImmediate() {
        SmsStatus s = service.scheduleSms(CLIENT, null, null, null, scheduleNow(), "testImmediate");
        
        String id = s.id();
        assertNotNull(id);
        assertTrue(SMS_STATUS_NEW.equals(s.status()) || SMS_STATUS_SCHEDULED.equals(s.status()));
        
        service.deleteInstance(id);
        assertNull(service.getSmsStatus(id));
    }

    @Test
    void testImmediateWithWait() {
        SmsStatus s = service.scheduleSms(CLIENT, null, null, null, scheduleNow(), "testImmediateWithWait");
        
        String id = s.id();
        assertNotNull(id);
        
        waitForAsync(1);

        SmsStatus r = service.getSmsStatus(id);
        assertNotNull(r);
        assertEquals(SMS_STATUS_ENROUTE, r.status());

        service.deleteInstance(id);
        assertNull(service.getSmsStatus(id));
    }

    @Test
    void testDelayedWithWait() {
        SmsStatus s = service.scheduleSms(CLIENT, null, null, null, scheduleIn(1), "testDelayedWithWait");
        
        String id = s.id();
        assertNotNull(id);

        s = service.getSmsStatus(id);
        assertNotNull(s);
        assertEquals(SMS_STATUS_SCHEDULED, s.status());
        
        waitForAsync(2);

        s = service.getSmsStatus(id);
        assertNotNull(s);
        assertEquals(SMS_STATUS_ENROUTE, s.status());

        service.deleteInstance(id);
        assertNull(service.getSmsStatus(id));
    }

    @Test
    void testCancelSms() {
        SmsStatus s = service.scheduleSms(CLIENT, null, null, null, scheduleIn(5), "testCancelSms");
        
        String id = s.id();
        assertNotNull(id);

        service.cancelSms(id);
       
        s = service.getSmsStatus(id);
        assertNotNull(s);
        assertEquals(SMS_STATUS_CANCELED, s.status());

        service.deleteInstance(id);
        assertNull(service.getSmsStatus(id));
    }

    @Test
    void testCancelBatch() {
        
        final String BATCH = "batch-id";
        final String NOT_BATCH = "not-" + BATCH;
        
        String id0 = service.scheduleSms(NOT_CLIENT, BATCH, null, null, scheduleIn(5), "testCancelBatch0").id();
        String id1 = service.scheduleSms(CLIENT, BATCH, null, null, scheduleIn(5), "testCancelBatch1").id();
        String id2 = service.scheduleSms(CLIENT, NOT_BATCH, null, null, scheduleIn(5), "testCancelBatch2").id();
        String id3 = service.scheduleSms(CLIENT, null, null, null, scheduleIn(5), "testCancelBatch3").id();
        
        service.cancelBatch(CLIENT, BATCH);
       
        assertEquals(SMS_STATUS_SCHEDULED, service.getSmsStatus(id0).status());
        assertEquals(SMS_STATUS_CANCELED, service.getSmsStatus(id1).status());
        assertEquals(SMS_STATUS_SCHEDULED, service.getSmsStatus(id2).status());
        assertEquals(SMS_STATUS_SCHEDULED, service.getSmsStatus(id3).status());
    }

    @Test
    void testCancelByClientKey() {
        
        final String KEY = "cancel-key";
        final String NO_KEY = "not-key";
        
        String id0 = service.scheduleSms(NOT_CLIENT, null, KEY, null, scheduleIn(5), "testCancelByClientKey0").id();
        String id1 = service.scheduleSms(CLIENT, null, KEY, null, scheduleIn(5), "testCancelByClientKey1").id();
        String id2 = service.scheduleSms(CLIENT, null, NO_KEY, null, scheduleIn(5), "testCancelByClientKey2").id();
        String id3 = service.scheduleSms(CLIENT, null, null, null, scheduleIn(5), "testCancelByClientKey3").id();
        
        service.cancelByClientKey(CLIENT, KEY);
       
        assertEquals(SMS_STATUS_SCHEDULED, service.getSmsStatus(id0).status());
        assertEquals(SMS_STATUS_CANCELED, service.getSmsStatus(id1).status());
        assertEquals(SMS_STATUS_SCHEDULED, service.getSmsStatus(id2).status());
        assertEquals(SMS_STATUS_SCHEDULED, service.getSmsStatus(id3).status());
    }

    @Test
    void testCancelAllForTarget() {
        
        final String TARGET = "cancel-target";
        final String NO_TARGET = "not-target";
        
        String id0 = service.scheduleSms(NOT_CLIENT, null, null, TARGET, scheduleIn(5), "testCancelAllForTarget0").id();
        String id1 = service.scheduleSms(CLIENT, null, null, TARGET, scheduleIn(5), "testCancelAllForTarget1").id();
        String id2 = service.scheduleSms(CLIENT, null, null, NO_TARGET, scheduleIn(5), "testCancelAllForTarget2").id();
        String id3 = service.scheduleSms(CLIENT, null, null, null, scheduleIn(5), "testCancelAllForTarget3").id();
        
        service.cancelAllForTarget(CLIENT, TARGET);
       
        assertEquals(SMS_STATUS_SCHEDULED, service.getSmsStatus(id0).status());
        assertEquals(SMS_STATUS_CANCELED, service.getSmsStatus(id1).status());
        assertEquals(SMS_STATUS_SCHEDULED, service.getSmsStatus(id2).status());
        assertEquals(SMS_STATUS_SCHEDULED, service.getSmsStatus(id3).status());
    }

    @Test
    void testCancelAllForClient() {
        
        String id0 = service.scheduleSms(NOT_CLIENT, CLIENT, CLIENT, CLIENT, scheduleIn(5), "testCancelAllForClient0").id();
        String id1 = service.scheduleSms(CLIENT, "b1", "k1", null, scheduleIn(5), "testCancelAllForClient1").id();
        String id2 = service.scheduleSms(CLIENT, null, null, "t2", scheduleIn(5), "testCancelAllForClient2").id();
        String id3 = service.scheduleSms(CLIENT, "b1", null, "t2", scheduleIn(5), "testCancelAllForClient3").id();
        
        service.cancelAllForClient(CLIENT);
       
        assertEquals(SMS_STATUS_SCHEDULED, service.getSmsStatus(id0).status());
        assertEquals(SMS_STATUS_CANCELED, service.getSmsStatus(id1).status());
        assertEquals(SMS_STATUS_CANCELED, service.getSmsStatus(id2).status());
        assertEquals(SMS_STATUS_CANCELED, service.getSmsStatus(id3).status());
    }

    @Test
    void testGetStatusList() {
        
        service.scheduleSms(NOT_CLIENT, null, null, null, scheduleIn(5), "testGetStatusList0").id();
        service.scheduleSms(CLIENT, null, null, null, scheduleIn(5), "testGetStatusList1").id();
        assertEquals(2, service.getStatusList().size());
    }

    @Test
    void testGetStatusListByClient() {
        
        service.scheduleSms(NOT_CLIENT, null, null, null, scheduleIn(5), "testGetStatusListByClient0").id();
        service.scheduleSms(CLIENT, null, null, null, scheduleIn(5), "testGetStatusListByClient1").id();
        service.scheduleSms(CLIENT, null, null, null, scheduleIn(5), "testGetStatusListByClient2").id();
        
        assertEquals(3, service.getStatusList().size());
        assertEquals(2, service.getStatusList(CLIENT).size());
    }

    @Test
    void testGetStatusListByBatch() {
        
        final String BATCH = "the-batch";
        final String NO_BATCH = "not-" + BATCH;
        
        service.scheduleSms(NOT_CLIENT, BATCH, null, null, scheduleIn(5), "testGetStatusListByBatch0").id();
        service.scheduleSms(CLIENT, BATCH, null, null, scheduleIn(5), "testGetStatusListByBatch1").id();
        service.scheduleSms(CLIENT, NO_BATCH, null, null, scheduleIn(5), "testGetStatusListByBatch2").id();
        
        assertEquals(3, service.getStatusList().size());
        assertEquals(2, service.getStatusList(CLIENT).size());
    }

    @Test
    void testGetStatusListByTarget() {
        
        final String TARGET = "the-target";
        final String NO_TARGET = "not-target";
        
        service.scheduleSms(NOT_CLIENT, null, null, TARGET, scheduleIn(5), "testGetStatusListByTarget0").id();
        service.scheduleSms(CLIENT, null, null, TARGET, scheduleIn(5), "testGetStatusListByTarget1").id();
        service.scheduleSms(CLIENT, null, null, NO_TARGET, scheduleIn(5), "testGetStatusListByTarget2").id();
        
        assertEquals(1, service.getStatusListByTarget(CLIENT, TARGET).size());
    }

    @Test
    void testGetStatusListByKey() {

        final String KEY = "cancel-key";
        final String NO_KEY = "not-key";
        
        service.scheduleSms(NOT_CLIENT, null, KEY, null, scheduleIn(5), "testCancelAllForTarget0").id();
        service.scheduleSms(CLIENT, null, KEY, null, scheduleIn(5), "testCancelByClientKey1").id();
        service.scheduleSms(CLIENT, null, NO_KEY, null, scheduleIn(5), "testCancelByClientKey2").id();
        
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
