package it.zwets.sms.scheduler.util;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Scheduler manages a schedule which is a list of zero or more {@link Slot}s.
 * 
 * Scheduler can parse two String formats.  Both are semicolon separated lists
 * of Slots.  Slots can be parsed as either long-long or iso/iso, where long
 * are seconds since the Epoch, and iso is a ISO8601 instant (with time offset).
 * 
 * Scheduler orders and merges the slots upon construction.
 * 
 * @author zwets
 */
public final class Scheduler implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Slot[] slots;
	
	/**
	 * Create a scheduler with an empty schedule
	 */
	public Scheduler() {
	    this.slots = new Slot[0];
	}

	/**
	 * Create a scheduler with an array of slots
	 * @param slots array of Slots, will be ordered and merged
	 */
	public Scheduler(Slot[] slots) {
	    List<Slot> list = new ArrayList<Slot>();
	    for (Slot s : slots) {
	        list = addSlot(list, s.from, s.till);
        }
	    this.slots = list.toArray(Slot[]::new);
	}

	/**
	 * Create a scheduler from a string representation of a list of slots
	 * @param schedule a semicolon-separated string as produced by toString
	 */
    public Scheduler(String schedule) {
        List<Slot> list = new ArrayList<Slot>();
        try (Scanner scanner = new Scanner(schedule)) {
            scanner.useDelimiter(";");
            while (scanner.hasNext()) {
                Slot s = Slot.parse(scanner.next());
                list = addSlot(list, s.from, s.till);
            }
        }
        this.slots = list.toArray(Slot[]::new);
    }
    
    /**
     * Getter for the array of slots
     * @return
     */
    public Slot[] getSlots() {
        return slots;
    }

    /* Private helper to get first slot that either contains from or is after it. */
    private Slot getFirstAvailableSlot(long from) {
        for (Slot s : slots) {
            if (from < s.till) {
                return s;
            }
        }
        return null;
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
        Instant result = null;
        if (from != null) {
            long earliest = from.getEpochSecond();
            Slot s = getFirstAvailableSlot(earliest);
            result = s == null ? null : Instant.ofEpochSecond(earliest < s.from ? s.from : earliest);
        }
        return result;
    }

    /**
     * Get the end of the first available slot for instant from.
     * @param from the instant to start looking for
     * @return the end of the slot containing <code>from</code>, or <code>null</code> if none.
     */
    public Instant getDeadlineInstant(Instant from) {
        Instant result = null;
        if (from != null) {
            long after = from.getEpochSecond();
            Slot s = getFirstAvailableSlot(after);
            result = s == null ? null : Instant.ofEpochSecond(s.till);
        }
        return result;
    }
    
    /* Private static method to add a slot to a list, keeping the list
     * sorted and merging slots necessary. 
     */
	private static List<Slot> addSlot(List<Slot> list, long from, long till) {

        List<Slot> result = new ArrayList<Slot>();
        
        int i = 0; // advance i past all slots strictly before us
        while (i < list.size() && from > list.get(i).till) {
            result.add(list.get(i++));
        }
        
        // i now is slot before or in which we start; pick earliest from
        if (i < list.size() && list.get(i).from < from) {
            from = list.get(i).from; // from is start of slot we are in
        }

        while (i < list.size() && till > list.get(i).till) {
            ++i; // advance i to the first slot strictly after us
        }

        // i now is slot before or in which we end
        if (i < list.size() && till >= list.get(i).from) {
            till = list.get(i++).till; // till is end of slot we were in
        }

        result.add(new Slot(from, till));
        
        // add the remaining slots, which are strictly after us
        while (i < list.size()) {
            result.add(list.get(i++));
        }

        return result;
	}
	
	@Override
	public String toString() {
		return Arrays.stream(slots).map(Slot::toString).collect(Collectors.joining(";"));
	}

	/**
	 * Parse a schedule from its JSON representation
	 * @param json
	 * @return a new Schedule object or RuntimeException
	 */
	public static Scheduler parseJson(String json) {
	    try {
	        ObjectMapper mapper = new ObjectMapper();
            Slot[] slots = mapper.readerFor(Slot[].class).readValue(json);
            return new Scheduler(slots);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON array of Slots");
        }
	}
	
	/**
	 * Write JSON representation of the Schedule as an array of Slot
	 * @return String
	 */
	public String toJson() {
	    try {
	        ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(slots);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert to JSON array of Slots");
        }
	}
}