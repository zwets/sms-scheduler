package it.zwets.sms.scheduler;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import it.zwets.sms.scheduler.SmsSchedulerService.SmsStatus;
import it.zwets.sms.scheduler.dto.Schedule;
import it.zwets.sms.scheduler.dto.Slot;

@SpringBootTest
class SmsSchedulerServiceTests {
    
    private static final Logger LOG = LoggerFactory.getLogger(SmsSchedulerService.class);

    @Autowired
    private SmsSchedulerService service;
    
    @Test
    void testScheduleSmsEmptyClient() {

        long now = Instant.now().getEpochSecond();
        Slot[] slots = new Slot[] { new Slot(now+20, now+30), new Slot(now, now+10) };
        Schedule schedule = new Schedule(slots);
        
        SmsStatus s = service.scheduleSms("client", "target", "bizkey", schedule, "DUMMY PAYLOAD");
        LOG.debug("id: {}, client: {}, target: {}, key: {}, status: {}, started: {}, ended: {}, retries: {}",
                s.id(), s.client(), s.target(), s.key(), s.status(), s.started(), s.ended(), s.retries());
        
        assertNull(s.ended());
        
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
    void testGetStatusListByBusinessKey() {
//        fail("Not yet implemented");
    }

}
