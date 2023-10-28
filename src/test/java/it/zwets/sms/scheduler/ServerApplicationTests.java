package it.zwets.sms.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.flowable.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ServerApplicationTests {
    
    private static final Logger LOG = LoggerFactory.getLogger(ServerApplicationTests.class);

	@Autowired
	private RuntimeService runtimeService;
	
	@Autowired
	private SmsSchedulerService smsSchedulerService;
	
	@Test
	void contextLoads() {
	}

	@Test
	void directStartProcessTest() {
//		runtimeService.startProcessInstanceByKey(Constants.APP_PROCESS_NAME);
//		assertEquals(0, runtimeService.createProcessInstanceQuery().count());
	}
	
	@Test
	void serviceScheduleSmsTest() {
		
		Schedule schedule = new Schedule();
		long now = Instant.now().getEpochSecond();
		schedule.addSlot(now + 20, now + 30);
		schedule.addSlot(now, now + 10);
		
		smsSchedulerService.scheduleSms("client-id", "target-id", "unique-id", schedule, "DUMMY PAYLOAD");
		assertEquals(1, runtimeService.createProcessInstanceQuery().count());
	}
}
