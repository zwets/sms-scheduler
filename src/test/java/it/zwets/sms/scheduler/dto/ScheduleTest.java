package it.zwets.sms.scheduler.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import it.zwets.sms.scheduler.dto.Schedule.Slot;

class ScheduleTest {

	@Test
	void testValidSlot() {
		Schedule.Slot slot = new Schedule.Slot(10, 20);
		assertEquals(10, slot.getFrom());
		assertEquals(20, slot.getUntil());
	}
	
	@Test
	void testInvalidZeroSizeSlot() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			new Schedule.Slot(10, 10);
		});
		assertEquals("from must be before until", e.getMessage());
	}
	
	@Test
	void testInvalidInverseSlot() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			new Schedule.Slot(20, 10);
		});
		assertEquals("from must be before until", e.getMessage());
	}
	
	@Test
	void testSlotToString() {
		assertEquals("10 20", new Schedule.Slot(10, 20).toString());
	}

	@Test
	void testEmptySchedule() {
		Schedule s = new Schedule();
		assertEquals(0, s.getSlots().size());
	}
	
	@Test
	void testAddSlot1() {
		Schedule s = new Schedule();
		s.addSlot(new Schedule.Slot(10, 20));
		assertEquals(1, s.getSlots().size());
		assertEquals(10, s.getSlots().get(0).getFrom());
		assertEquals(20, s.getSlots().get(0).getUntil());
	}
	
	@Test
	void testAddSlot2() {
		Schedule s = new Schedule();
		s.addSlot(10, 20);
		assertEquals(1, s.getSlots().size());
		assertEquals(10, s.getSlots().get(0).getFrom());
		assertEquals(20, s.getSlots().get(0).getUntil());
	}
	
	@Test
	void testAddTwoSlots() {
		Schedule s = new Schedule();
		s.addSlot(new Schedule.Slot(10, 20));
		s.addSlot(30, 40);
		assertEquals(2, s.getSlots().size());
		assertEquals(10, s.getSlots().get(0).getFrom());
		assertEquals(20, s.getSlots().get(0).getUntil());
		assertEquals(30, s.getSlots().get(1).getFrom());
		assertEquals(40, s.getSlots().get(1).getUntil());
	}
	
	@Test
	void testSlotsGetOrdered() {
		Schedule s = new Schedule();
		s.addSlot(30, 40);
		s.addSlot(10, 20);
		assertEquals(2, s.getSlots().size());
		assertEquals(10, s.getSlots().get(0).getFrom());
		assertEquals(20, s.getSlots().get(0).getUntil());
		assertEquals(30, s.getSlots().get(1).getFrom());
		assertEquals(40, s.getSlots().get(1).getUntil());
	}
	
	@Test
	void testArrayConstructor() {
		Schedule s = new Schedule(30, 40, 10, 20);
		assertEquals(2, s.getSlots().size());
		assertEquals(10, s.getSlots().get(0).getFrom());
		assertEquals(20, s.getSlots().get(0).getUntil());
		assertEquals(30, s.getSlots().get(1).getFrom());
		assertEquals(40, s.getSlots().get(1).getUntil());
	}

	@Test
	void testArrayConstructorWrongSize() {
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
			new Schedule(10, 20, 30);
		});
	}

	@Test
	void testStringConstructor() {
		Schedule s = new Schedule("30 40 10 20");
		assertEquals(2, s.getSlots().size());
		assertEquals(10, s.getSlots().get(0).getFrom());
		assertEquals(20, s.getSlots().get(0).getUntil());
		assertEquals(30, s.getSlots().get(1).getFrom());
		assertEquals(40, s.getSlots().get(1).getUntil());
	}

	@Test
	void testStringConstructorWrongSize() {
		assertThrows(NoSuchElementException.class, () -> {
			new Schedule("10 20 30");
		});
	}

	@Test
	void testStringConstructorInvalidType() {
		assertThrows(NoSuchElementException.class, () -> {
			new Schedule("10 20 A 10");
		});
	}

	@Test
	void testScheduleToString() {
		Schedule s = new Schedule(30, 40, 10, 20);
		assertEquals("10 20 30 40", s.toString());
	}

	@Test
	void testStringConstructorAndBack() {
		Schedule s1 = new Schedule(30, 40, 10, 20);
		Schedule s2 = new Schedule(s1.toString());
		
		assertEquals(s1.getSlots().size(), s2.getSlots().size());
		assertEquals(s2.getSlots().get(0).getFrom(), s2.getSlots().get(0).getFrom());
		assertEquals(s1.getSlots().get(0).getUntil(), s2.getSlots().get(0).getUntil());
		assertEquals(s1.getSlots().get(1).getFrom(), s2.getSlots().get(1).getFrom());
		assertEquals(s1.getSlots().get(1).getUntil(), s2.getSlots().get(1).getUntil());
	}

	@Test
	void testNoFrontOverlap() {
		Schedule s = new Schedule();
		s.addSlot(10, 20);
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
			s.addSlot(5,15);
		});
		assertEquals("end of slot overlaps start of existing slot", e.getMessage());
	}

	@Test
	void testAbuttingFront() {
		Schedule s = new Schedule();
		s.addSlot(10, 20);
		s.addSlot(5, 10);
		assertEquals(2, s.getSlots().size());
		assertEquals(5, s.getSlots().get(0).getFrom());
		assertEquals(10, s.getSlots().get(0).getUntil());
		assertEquals(10, s.getSlots().get(1).getFrom());
		assertEquals(20, s.getSlots().get(1).getUntil());
	}

	@Test
	void testBackOverlap() {
		Schedule s = new Schedule();
		s.addSlot(10, 20);
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
			s.addSlot(15, 20);
		});
		assertEquals("start of slot overlaps end of existing slot", e.getMessage());
	}

	@Test
	void testAbuttingEnd() {
		Schedule s = new Schedule();
		s.addSlot(10, 20);
		s.addSlot(20, 30);
		assertEquals(2, s.getSlots().size());
		assertEquals(10, s.getSlots().get(0).getFrom());
		assertEquals(20, s.getSlots().get(0).getUntil());
		assertEquals(20, s.getSlots().get(1).getFrom());
		assertEquals(30, s.getSlots().get(1).getUntil());
	}
	
	@Test
	void testEmptyGivesNotAvailableNow() {
		Schedule schedule = new Schedule();
		assertNull(schedule.getFirstAvailableInstant());
	}

	@Test
	void testEmptyGivesNotAvailableLater() {
		Schedule schedule = new Schedule();
		assertNull(schedule.getFirstAvailableInstant(Instant.now().plusSeconds(10)));
	}

	@Test
	void testEmptyGivesNotAvailableForZero() {
		Schedule schedule = new Schedule();
		assertNull(schedule.getFirstAvailableInstant(Instant.ofEpochSecond(0)));
	}
	
	@Test
	void testAvailableAfterTooEarly() {
		Schedule schedule = new Schedule(10, 20, 30, 40);
		assertEquals(10, schedule.getFirstAvailable(5));
	}
	
	@Test
	void testNotAvailableAfterTooLate() {
		Schedule schedule = new Schedule(10, 20, 30, 40);
		assertNull(schedule.getFirstAvailable(50));
	}

	@Test
	void testAvailableInsideSlot() {
		Schedule schedule = new Schedule(10, 20, 30, 40);
		assertEquals(15, schedule.getFirstAvailable(15));
	}
	
	@Test
	void testAvailableWhenBetweenSlots() {
		Schedule schedule = new Schedule(10, 20, 30, 40);
		assertEquals(30, schedule.getFirstAvailable(25));
	}

	@Test
	void testAvailableInstantAfterTooEarly() {
		long now = Instant.now().getEpochSecond();
		Schedule schedule = new Schedule(now+10, now+20, now+30, now+40);
		assertEquals(Instant.ofEpochSecond(now + 10), schedule.getFirstAvailableInstant(Instant.ofEpochSecond(now + 5)));
	}
	
	@Test
	void testNotAvailableInstantAfterTooLate() {
		long now = Instant.now().getEpochSecond();
		Schedule schedule = new Schedule(now+10, now+20, now+30, now+40);
		assertNull(schedule.getFirstAvailableInstant(Instant.ofEpochSecond(now + 50)));
	}

	@Test
	void testAvailableInstantInsideSlot() {
		long now = Instant.now().getEpochSecond();
		Schedule schedule = new Schedule(now+10, now+20, now+30, now+40);
		assertEquals(Instant.ofEpochSecond(now + 15), schedule.getFirstAvailableInstant(Instant.ofEpochSecond(now + 15)));
	}
	
	@Test
	void testAvailableInstantWhenBetweenSlots() {
		long now = Instant.now().getEpochSecond();
		Schedule schedule = new Schedule(now+10, now+20, now+30, now+40);
		assertEquals(Instant.ofEpochSecond(now + 30), schedule.getFirstAvailableInstant(Instant.ofEpochSecond(now + 25)));
	}
}
