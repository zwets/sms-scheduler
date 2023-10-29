package it.zwets.sms.scheduler.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ScheduleTest {
    
    private Schedule makeSchedule(long... times) {
        Slot[] slots = new Slot[times.length / 2];
        for (int i = 0; i < times.length / 2; ++i) {
            slots[i] = new Slot(times[2*i], times[2*i+1]);
        }
        return new Schedule(slots);
    }

	@Test
	void testEmptySchedule() {
		Schedule s = makeSchedule();
		assertEquals(0, s.getSlots().length);
	}
	
	@Test
	void testParseText() {
	    Schedule s = makeSchedule(10, 20, 30, 35, 40, 60);
	    assertEquals("10-20;30-35;40-60", s.toString());
	    Schedule n = Schedule.parse("10-20;30-35;40-60");
	    assertEquals(s.getSlots(), n.getSlots());
	}
	
    @Test
    void testParseJson() {
        Schedule s = makeSchedule(10, 20, 30, 35, 40, 60);
        assertEquals("10-20;30-35;40-60", s.toString());
    }
    
	@Test
	void testAddSlot1() {
		Schedule s = makeSchedule(10, 20);
		assertEquals(1, s.getSlots().length);
		assertEquals(10, s.getSlots()[0].getFrom());
		assertEquals(20, s.getSlots()[0].getTill());
	}
	
	@Test
	void testAddTwoSlots() {
		Schedule s = makeSchedule(10, 20, 30, 40);
		assertEquals(2, s.getSlots().length);
		assertEquals(10, s.getSlots()[0].getFrom());
		assertEquals(20, s.getSlots()[0].getTill());
		assertEquals(30, s.getSlots()[1].getFrom());
		assertEquals(40, s.getSlots()[1].getTill());
	}
	
	@Test
	void testSlotsGetOrdered() {
		Schedule s = makeSchedule(30, 40, 10, 20);
		assertEquals(2, s.getSlots().length);
		assertEquals(10, s.getSlots()[0].getFrom());
		assertEquals(20, s.getSlots()[0].getTill());
		assertEquals(30, s.getSlots()[1].getFrom());
		assertEquals(40, s.getSlots()[1].getTill());
	}
	
	@Test
	void testArrayConstructor() {
		Schedule s = makeSchedule(30, 40, 10, 20);
		assertEquals(2, s.getSlots().length);
		assertEquals(10, s.getSlots()[0].getFrom());
		assertEquals(20, s.getSlots()[0].getTill());
		assertEquals(30, s.getSlots()[1].getFrom());
		assertEquals(40, s.getSlots()[1].getTill());
	}

	@Test
	void testStringParser() {
		Schedule s = Schedule.parse("30-40;10-20");
		assertEquals(2, s.getSlots().length);
		assertEquals(10, s.getSlots()[0].getFrom());
		assertEquals(20, s.getSlots()[0].getTill());
		assertEquals(30, s.getSlots()[1].getFrom());
		assertEquals(40, s.getSlots()[1].getTill());
	}

	@Test
	void testScheduleToString() {
		Schedule s = makeSchedule(30, 40, 10, 20);
		assertEquals("10-20;30-40", s.toString());
	}

	@Test
	void testStringConstructorAndBack() {
		Schedule s1 = makeSchedule(30, 40, 10, 20);
		Schedule s2 = Schedule.parse(s1.toString());
		
		assertEquals(s1.getSlots().length, s2.getSlots().length);
		assertEquals(s2.getSlots()[0].getFrom(), s2.getSlots()[0].getFrom());
		assertEquals(s1.getSlots()[0].getTill(), s2.getSlots()[0].getTill());
		assertEquals(s1.getSlots()[1].getFrom(), s2.getSlots()[1].getFrom());
		assertEquals(s1.getSlots()[1].getTill(), s2.getSlots()[1].getTill());
	}

	@Test
	void testFrontOverlap() {
		Schedule s = makeSchedule(10, 20, 5, 15);
        assertEquals(1, s.getSlots().length);
        assertEquals(5, s.getSlots()[0].getFrom());
        assertEquals(20, s.getSlots()[0].getTill());
	}

	@Test
	void testAbuttingFront() {
		Schedule s = makeSchedule(10, 20, 5, 10);
		assertEquals(1, s.getSlots().length);
		assertEquals(5, s.getSlots()[0].getFrom());
		assertEquals(20, s.getSlots()[0].getTill());
	}

	@Test
	void testBackOverlap() {
		Schedule s = makeSchedule(10, 20, 15, 20);
        assertEquals(1, s.getSlots().length);
        assertEquals(10, s.getSlots()[0].getFrom());
        assertEquals(20, s.getSlots()[0].getTill());
	}

	@Test
	void testAbuttingEnd() {
		Schedule s = makeSchedule(10, 20, 20, 30);
		assertEquals(1, s.getSlots().length);
		assertEquals(10, s.getSlots()[0].getFrom());
		assertEquals(30, s.getSlots()[0].getTill());
	}
	
	@Test
	void testEmptyGivesNotAvailableNow() {
		Schedule schedule = makeSchedule();
		assertNull(schedule.getFirstAvailableInstant());
	}

	@Test
	void testEmptyGivesNotAvailableLater() {
		Schedule schedule = makeSchedule();
		assertNull(schedule.getFirstAvailableInstant(Instant.now().plusSeconds(10)));
	}

	@Test
	void testEmptyGivesNotAvailableForZero() {
		Schedule schedule = makeSchedule();
		assertNull(schedule.getFirstAvailableInstant(Instant.ofEpochSecond(0)));
	}
	
	@Test
	void testAvailableAfterTooEarly() {
		Schedule schedule = makeSchedule(10, 20, 30, 40);
		assertEquals(10, schedule.getFirstAvailable(5));
	}
	
	@Test
	void testNotAvailableAfterTooLate() {
		Schedule schedule = makeSchedule(10, 20, 30, 40);
		assertNull(schedule.getFirstAvailable(50));
	}

	@Test
	void testAvailableInsideSlot() {
		Schedule schedule = makeSchedule(10, 20, 30, 40);
		assertEquals(15, schedule.getFirstAvailable(15));
	}
	
	@Test
	void testAvailableWhenBetweenSlots() {
		Schedule schedule = makeSchedule(10, 20, 30, 40);
		assertEquals(30, schedule.getFirstAvailable(25));
	}

	@Test
	void testAvailableInstantAfterTooEarly() {
		long now = Instant.now().getEpochSecond();
		Schedule schedule = makeSchedule(now+10, now+20, now+30, now+40);
		assertEquals(Instant.ofEpochSecond(now + 10), schedule.getFirstAvailableInstant(Instant.ofEpochSecond(now + 5)));
	}
	
	@Test
	void testNotAvailableInstantAfterTooLate() {
		long now = Instant.now().getEpochSecond();
		Schedule schedule = makeSchedule(now+10, now+20, now+30, now+40);
		assertNull(schedule.getFirstAvailableInstant(Instant.ofEpochSecond(now + 50)));
	}

	@Test
	void testAvailableInstantInsideSlot() {
		long now = Instant.now().getEpochSecond();
		Schedule schedule = makeSchedule(now+10, now+20, now+30, now+40);
		assertEquals(Instant.ofEpochSecond(now + 15), schedule.getFirstAvailableInstant(Instant.ofEpochSecond(now + 15)));
	}
	
	@Test
	void testAvailableInstantWhenBetweenSlots() {
		long now = Instant.now().getEpochSecond();
		Schedule schedule = makeSchedule(now+10, now+20, now+30, now+40);
		assertEquals(Instant.ofEpochSecond(now + 30), schedule.getFirstAvailableInstant(Instant.ofEpochSecond(now + 25)));
	}
}
