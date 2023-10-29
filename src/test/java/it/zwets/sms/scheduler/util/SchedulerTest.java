package it.zwets.sms.scheduler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SchedulerTest {

    private Scheduler makeScheduler(long... times) {
        Slot[] slots = new Slot[times.length / 2];
        for (int i = 0; i < times.length / 2; ++i) {
            slots[i] = new Slot(times[2*i], times[2*i+1]);
        }
        return new Scheduler(slots);
    }

	@Test
	void testEmptySchedule() {
		Scheduler s = makeScheduler();
		assertEquals(0, s.getSlots().length);
	}
	
	@Test
	void testParseText() {
	    Scheduler s = makeScheduler(10, 20, 30, 35, 40, 60);
	    assertEquals("10-20;30-35;40-60", s.toString());
	    Scheduler n = new Scheduler("10-20;30-35;40-60");
            for (int i = 0; i < s.getSlots().length; ++i) {
                assertEquals(s.getSlots()[i].getFrom(), n.getSlots()[i].getFrom());
            }
	}
	
    @Test
    void testParseJson() {
        Scheduler s = makeScheduler(10, 20, 30, 35, 40, 60);
        assertEquals("10-20;30-35;40-60", s.toString());
    }

	@Test
	void testAddSlot1() {
		Scheduler s = makeScheduler(10, 20);
		assertEquals(1, s.getSlots().length);
		assertEquals(10, s.getSlots()[0].getFrom());
		assertEquals(20, s.getSlots()[0].getTill());
	}
	
	@Test
	void testAddTwoSlots() {
		Scheduler s = makeScheduler(10, 20, 30, 40);
		assertEquals(2, s.getSlots().length);
		assertEquals(10, s.getSlots()[0].getFrom());
		assertEquals(20, s.getSlots()[0].getTill());
		assertEquals(30, s.getSlots()[1].getFrom());
		assertEquals(40, s.getSlots()[1].getTill());
	}
	
	@Test
	void testSlotsGetOrdered() {
		Scheduler s = makeScheduler(30, 40, 10, 20);
		assertEquals(2, s.getSlots().length);
		assertEquals(10, s.getSlots()[0].getFrom());
		assertEquals(20, s.getSlots()[0].getTill());
		assertEquals(30, s.getSlots()[1].getFrom());
		assertEquals(40, s.getSlots()[1].getTill());
	}
	
	@Test
	void testArrayConstructor() {
		Scheduler s = makeScheduler(30, 40, 10, 20);
		assertEquals(2, s.getSlots().length);
		assertEquals(10, s.getSlots()[0].getFrom());
		assertEquals(20, s.getSlots()[0].getTill());
		assertEquals(30, s.getSlots()[1].getFrom());
		assertEquals(40, s.getSlots()[1].getTill());
	}

	@Test
	void testStringParser() {
		Scheduler s = new Scheduler("30-40;10-20");
		assertEquals(2, s.getSlots().length);
		assertEquals(10, s.getSlots()[0].getFrom());
		assertEquals(20, s.getSlots()[0].getTill());
		assertEquals(30, s.getSlots()[1].getFrom());
		assertEquals(40, s.getSlots()[1].getTill());
	}

	@Test
	void testScheduleToString() {
		Scheduler s = makeScheduler(30, 40, 10, 20);
		assertEquals("10-20;30-40", s.toString());
	}

	@Test
	void testStringConstructorAndBack() {
		Scheduler s1 = makeScheduler(30, 40, 10, 20);
		Scheduler s2 = new Scheduler(s1.toString());
		
		assertEquals(s1.getSlots().length, s2.getSlots().length);
		assertEquals(s2.getSlots()[0].getFrom(), s2.getSlots()[0].getFrom());
		assertEquals(s1.getSlots()[0].getTill(), s2.getSlots()[0].getTill());
		assertEquals(s1.getSlots()[1].getFrom(), s2.getSlots()[1].getFrom());
		assertEquals(s1.getSlots()[1].getTill(), s2.getSlots()[1].getTill());
	}

	@Test
	void testFrontOverlap() {
		Scheduler s = makeScheduler(10, 20, 5, 15);
        assertEquals(1, s.getSlots().length);
        assertEquals(5, s.getSlots()[0].getFrom());
        assertEquals(20, s.getSlots()[0].getTill());
	}

	@Test
	void testAbuttingFront() {
		Scheduler s = makeScheduler(10, 20, 5, 10);
		assertEquals(1, s.getSlots().length);
		assertEquals(5, s.getSlots()[0].getFrom());
		assertEquals(20, s.getSlots()[0].getTill());
	}

	@Test
	void testBackOverlap() {
		Scheduler s = makeScheduler(10, 20, 15, 20);
        assertEquals(1, s.getSlots().length);
        assertEquals(10, s.getSlots()[0].getFrom());
        assertEquals(20, s.getSlots()[0].getTill());
	}

	@Test
	void testAbuttingEnd() {
		Scheduler s = makeScheduler(10, 20, 20, 30);
		assertEquals(1, s.getSlots().length);
		assertEquals(10, s.getSlots()[0].getFrom());
		assertEquals(30, s.getSlots()[0].getTill());
	}
	
	@Test
	void testEmptyGivesNotAvailableNow() {
		Scheduler schedule = makeScheduler();
		assertNull(schedule.getFirstAvailableInstant());
	}

	@Test
	void testEmptyGivesNotAvailableLater() {
		Scheduler schedule = makeScheduler();
		assertNull(schedule.getFirstAvailableInstant(Instant.now().plusSeconds(10)));
	}

	@Test
	void testEmptyGivesNotAvailableForZero() {
		Scheduler schedule = makeScheduler();
		assertNull(schedule.getFirstAvailableInstant(Instant.ofEpochSecond(0)));
	}
	
	@Test
	void testAvailableInstantAfterTooEarly() {
		long now = Instant.now().getEpochSecond();
		Scheduler schedule = makeScheduler(now+10, now+20, now+30, now+40);
		assertEquals(Instant.ofEpochSecond(now + 10), schedule.getFirstAvailableInstant(Instant.ofEpochSecond(now + 5)));
	}
	
	@Test
	void testNotAvailableInstantAfterTooLate() {
		long now = Instant.now().getEpochSecond();
		Scheduler schedule = makeScheduler(now+10, now+20, now+30, now+40);
		assertNull(schedule.getFirstAvailableInstant(Instant.ofEpochSecond(now + 50)));
	}

	@Test
	void testAvailableInstantInsideSlot() {
		long now = Instant.now().getEpochSecond();
		Scheduler schedule = makeScheduler(now+10, now+20, now+30, now+40);
		assertEquals(Instant.ofEpochSecond(now + 15), schedule.getFirstAvailableInstant(Instant.ofEpochSecond(now + 15)));
	}
	
	@Test
	void testAvailableInstantWhenBetweenSlots() {
		long now = Instant.now().getEpochSecond();
		Scheduler schedule = makeScheduler(now+10, now+20, now+30, now+40);
		assertEquals(Instant.ofEpochSecond(now + 30), schedule.getFirstAvailableInstant(Instant.ofEpochSecond(now + 25)));
	}

   @Test
    void testDeadlineInstantAfterTooEarly() {
        long now = Instant.now().getEpochSecond();
        Scheduler schedule = makeScheduler(now+10, now+20, now+30, now+40);
        assertEquals(Instant.ofEpochSecond(now + 20), schedule.getDeadlineInstant(Instant.ofEpochSecond(now + 5)));
    }
    
    @Test
    void testDeadlineNullForNotAvailableInstant() {
        long now = Instant.now().getEpochSecond();
        Scheduler schedule = makeScheduler(now+10, now+20, now+30, now+40);
        assertNull(schedule.getDeadlineInstant(Instant.ofEpochSecond(now + 50)));
    }

    @Test
    void testDeadlineForInstantInsideSlot() {
        long now = Instant.now().getEpochSecond();
        Scheduler schedule = makeScheduler(now+10, now+20, now+30, now+40);
        assertEquals(Instant.ofEpochSecond(now + 20), schedule.getDeadlineInstant(Instant.ofEpochSecond(now + 15)));
    }
    
    @Test
    void testDeadlineWhenBetweenSlots() {
        long now = Instant.now().getEpochSecond();
        Scheduler schedule = makeScheduler(now+10, now+20, now+30, now+40);
        assertEquals(Instant.ofEpochSecond(now + 40), schedule.getDeadlineInstant(Instant.ofEpochSecond(now + 25)));
    }

}
