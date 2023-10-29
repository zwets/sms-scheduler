package it.zwets.sms.scheduler.dto;

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
 * Schedule is a list of zero or more time slots.
 * 
 * The time slots are ordered and non-overlapping.
 * 
 * @author zwets
 */
public final class Schedule implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Slot[] slots;
	
	/**
	 * Create an empty schedule
	 */
	public Schedule() {
	    this.slots = new Slot[0];
	}

	/**
	 * Create a schedule from an array of slots
	 * @param slots array of Slots
	 */
	public Schedule(Slot[] slots) {
	    List<Slot> list = new ArrayList<Slot>();
	    for (Slot s : slots) {
	        list = addSlot(list, s.from, s.till);
        }
	    this.slots = list.toArray(Slot[]::new);
	}

	/**
	 * 
	 * @return
	 */
    public Slot[] getSlots() {
        return slots;
    }

    /**
     * Get the first available time in the schedule that is no earlier than <code>earliest</code>.
     * 
     * @param earliest the time (in seconds since the Epoch) to start searching from
     * @return null or the first available time in the schedule that is not before earliest
     */
    public Long getFirstAvailable(long earliest) {
        for (Slot s : slots) {
            if (earliest < s.till) {
                return s.from > earliest ? s.from : earliest;
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
        Long earliest = getFirstAvailable(from.getEpochSecond());
        return earliest == null ? null : Instant.ofEpochSecond(earliest.longValue());
    }

    /* Private constructor that takes a list produced by #addList,
     * i.e. a list of slots which is already in correct order.
     */
    private Schedule(List<Slot> list) {
        this.slots = list.toArray(Slot[]::new);
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
	
    /**
     * Parse a schedule from its toString representation
     * @param str
     * @return a new Schedule object or RuntimeException
     */
    public static Schedule parse(String str) {
        List<Slot> slots = new ArrayList<Slot>();
        try (Scanner scanner = new Scanner(str)) {
            scanner.useDelimiter(";");
            while (scanner.hasNext()) {
                Slot s = Slot.parse(scanner.next());
                slots = addSlot(slots, s.from, s.till);
            }
        }
        return new Schedule(slots);
    }
    
	@Override
	public String toString() {
		return Arrays.stream(slots).map(Slot::toString).collect(Collectors.joining(";"));
	}

	/**
	 * Parse a schedule from its toString representation
	 * @param str
	 * @return a new Schedule object or RuntimeException
	 */
	public static Schedule parseJson(String json) {
	    try {
	        ObjectMapper mapper = new ObjectMapper();
            Slot[] slots = mapper.readerFor(Slot[].class).readValue(json);
            return new Schedule(slots);
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

	/**
	 * Old way. Remove if the above works.
	 * @return
	 */
	public String manualToJson() {
	    StringBuffer b = new StringBuffer();
	    b.append('[');
	    for (int i = 0; i < slots.length; ++i) {
	        if (i > 0) b.append(',');
	        b.append(' ');
	        b.append(slots[i].toJson());
	    }
	    b.append(" ]");
	    return b.toString();
	}
}