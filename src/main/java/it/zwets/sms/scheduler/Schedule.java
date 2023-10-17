package it.zwets.sms.scheduler;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Schedule is a list of zero or more time slots.
 * 
 * The time slots are ordered and non-overlapping.
 * 
 * @author zwets
 */
public final class Schedule implements Serializable {

	private static final long serialVersionUID = 1L;

	private final ArrayList<Slot> slots = new ArrayList<Slot>();
	
	/**
	 * Create an empty schedule
	 */
	public Schedule() {
	}

	/**
	 * Create a schedule from an array of longs: from0, until0, ... fromN, untilN.
	 * 
	 * @throws ArrayIndexOutOfBoundsException if the array size is not even
	 * @throws IllegalArgumentException if any slot (pair of values) is invalid
	 * @throws IllegalStateException if there are overlapping slots (pairs of values)
	 */
	public Schedule(long ...times) {
		for (int i = 0; i < times.length; i += 2) {
			addSlot(times[i], times[i+1]);
		}
	}

	/**
	 * Create a schedule from a string that is a space-separated list of longs.
	 * 
	 * @throws NoSuchElementException if the array size is not even or has non-long elements
	 * @throws IllegalArgumentException if any slot (pair of values) is invalid
	 * @throws IllegalStateException if there are overlapping slots (pairs of values)
	 */
	public Schedule(String list) {
		try (Scanner scanner = new Scanner(list)) {
			while (scanner.hasNext()) {
				addSlot(scanner.nextLong(), scanner.nextLong());
			}
		}
	}
	
	/**
	 * Add slot to the schedule.
	 * 
	 * The slot starts at <code>from</code> seconds since the Epoch,
	 * and ends at <code>until</code> seconds since the epoch.
	 * 
	 * @param from the second since the Epoch that the slot starts
	 * @param until the second since the Epoch that the slot ends
	 * @see {@link #addSlot(Slot)}
	 */
	public void addSlot(long from, long until) {
		addSlot(new Slot(from, until));
		
	}
	/**
	 * Add slot to the schedule.
	 * 
	 * The slot will be placed in time order in the existing list of slots.
	 * It must not overlap with any part of an already existing slot.
	 * 
	 * @param slot the slot to add to the schedule
	 * @throws IllegalStateException if slot overlaps an existing slot
	 */
	public void addSlot(Slot slot) {
		
		int pos = 0;
	
		while (pos < slots.size()) {
			
			Slot other = slots.get(pos);
			
			if (slot.from < other.from) {
				// comes before other, check that it does not overlap with other
				if (slot.until > other.from) {
					throw new IllegalStateException("end of slot overlaps start of existing slot");
				}
				break;
			}
			++pos;
		}
		// check that slot does not start before the end of the previous
		if (pos != 0) {
			if (slot.from < slots.get(pos-1).until) {
				throw new IllegalStateException("start of slot overlaps end of existing slot");				
			}
		}
		slots.add(pos, slot);
	}
	
	/**
	 * Get the first available time in the schedule that is no earlier than <code>earliest</code>.
	 * 
	 * @param earliest the time (in seconds since the Epoch) to start searching from
	 * @return null or the first available time in the schedule that is not before earliest
	 */
	public Long getFirstAvailable(long earliest) {
		return slots.stream()
				.dropWhile((s) -> s.until <= earliest)
				.findFirst()
				.map(s -> s.from > earliest ? s.from : earliest)
				.orElse(null);
	}
	
	/**
	 * Get the first available instant in the schedule.
	 * 
	 * @return the earliest instant after now which is in a slot in the schedule; <code>null</code> if no such exists.
	 */
	public Instant getFirstAvailableInstant() {
		return getFirstAvailableInstant(Instant.now());
	}

	/**
	 * Get the first available instant in the schedule at or after a given instant.
	 * @param from the instant to start looking from
	 * @return the earliest instant at or after <code>from</code> which falls in a slot, or <code>null</code> if none.
	 */
	public Instant getFirstAvailableInstant(Instant from) {
		Long earliest = getFirstAvailable(from.getEpochSecond());
		return earliest == null ? null : Instant.ofEpochSecond(earliest.longValue());
	}
	
	public List<Slot> getSlots() {
		return this.slots;
	}

	@Override
	public String toString() {
		return slots.stream().map(t -> t.toString()).collect(Collectors.joining(" "));
	}
	
    /**
     * Slot is a time window in a schedule.
     * 
     * The slot starts at <code>from</code> seconds since the Epoch and ends
     * at <code>until</code> seconds since the Epoch.
     * 
     * Note that units are POSIX seconds since the Epoch (1970-01-01 00:00 UTC)
     * (see <code>date +%s</code>), whereas {@link java.util.Date#getTime()}
     * is <i>milli</i>seconds since the epoch.
     * 
     * @author zwets
     */
    static final class Slot implements Serializable {
    	
    	private static final long serialVersionUID = 1L;
    	
		private long from;
    	private long until;

    	/**
    	 * Default constructor
    	 */
    	public Slot() {
    	}
    	
    	/**
    	 * Time slot starting at <i>from</i> seconds since the epoch,
    	 * and ending at <i>until</i> seconds since the epoch.
    	 * @param from seconds since the epoch
    	 * @param until seconds since the epoch
    	 * @throw IllegalArgumentException if from is not before until
    	 */
    	public Slot(long from, long until) {
    		if (from >= until) {
    			throw new IllegalArgumentException("from must be before until");
    		}
    		this.setFrom(from);
    		this.setUntil(until);
    	}

    	/**
    	 * Start of the interval
    	 * @return second since the Epoch at which the slot starts
    	 */
		public long getFrom() {
			return from;
		}

		/**
		 * Set the start of the interval
		 * @param from the second since the Epoch at which the slot starts
		 */
		public void setFrom(long from) {
			this.from = from;
		}

		/**
		 * End of the interval
		 * @return the second since the Epoch at which the slot ends
		 */
		public long getUntil() {
			return until;
		}

		/**
		 * Set the end of the interval
		 * @param until the second since the Epoch at which the slot ends
		 */
		public void setUntil(long until) {
			this.until = until;
		}
		
		@Override
		public String toString() {
			return String.format("%d %d", from, until);
		}
    }
}
