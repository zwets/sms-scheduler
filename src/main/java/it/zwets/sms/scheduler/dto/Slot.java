package it.zwets.sms.scheduler.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.Scanner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
* Slot is a time window in a schedule.
* 
* The slot starts at <code>from</code> seconds since the Epoch and ends
* at <code>till</code> seconds since the Epoch.
* 
* Note that units are POSIX seconds since the Epoch (1970-01-01 00:00 UTC)
* (see <code>date +%s</code>), whereas {@link java.util.Date#getTime()}
* is <i>milli</i>seconds since the epoch.
* 
* @author zwets
*/

public class Slot implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final long from;
    protected final long till;
    
    public Slot() {
        from = 0;
        till = 1;
    }
    
    /**
     * A time slot that starts at <i>from</i> seconds since the epoch,
     * and ends at <i>till</i> seconds in the epoch.
     * 
     * Zero-length or negative slots cannot exist, that is <i>from</i>
     * must be less than <i>till</i>.
     * 
     * @param from the first second that is in the time slot
     * @param till the first second that is not in the time slot
     * @throws IllegalArgumentException if the slot has 0 or negative length
     */
    public Slot(final long from, final long till) {
        if (from >= till) {
            throw new IllegalArgumentException("time slot must end after it starts");
        }
        this.from = from;
        this.till = till;
    }

    /**
     * Construct a slot from two {@link java.util.Date}
     * @param from the first second that is in the time slot
     * @param till the first second that is not in the time slot
     * @throws IllegalArgumentException if the slot has 0 or negative length
     * @throws NullPointerException if either date is null
     */
    public Slot(final Date from, final Date till) {
        this(from.getTime() / 1000, till.getTime() / 1000);
    }

    /**
     * Construct a slot from two {@link java.time.Instant}
     * @param from the first second that is in the time slot
     * @param till the first second that is not in the time slot
     * @throws IllegalArgumentException if the slot has 0 or negative length
     * @throws NullPointerException if either date is null
     */
    public Slot(final Instant from, final Instant till) {
        this(from.getEpochSecond(), till.getEpochSecond());
    }

    /**
     * The first second since the epoch that is in the time slot.
     * @return seconds since the epoch
     */
    public long getFrom() {
        return from;
    }

    /**
     * One past the last second since the epoch that is in the time slot.
     * @return seconds since the epoch
     */
    public long getTill() {
        return till;
    }
    
    @Override
    public String toString() {
        return "%d-%d".formatted(from, till);
    }
    
    /**
     * Parse a slot from its toString representation.
     * @param str string produced by toString
     * @return a Slot or throws RuntimeException
     */
    public static Slot parse(String str) {
        try (Scanner scanner = new Scanner(str)) {
            scanner.useDelimiter("-");
            return new Slot(scanner.nextLong(), scanner.nextLong());
        }
    }
    
    /**
     * Write JSON representation of the Schedule as an array of Slot
     * @return String
     */
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert Slot to Json");
        }
    }

    /**
     * Return a Json string representing the Slot
     * @return a Json format object with fields "from" and "till"
     */
    public String manualToJson() {
        return "{ \"from\": %d, \"till\": %d }".formatted(from, till);
    }
    
    /**
     * Parse a Slot from its json representation
     * @param json
     * @return a Slot or throws RuntimeException
     */
    public static Slot parseJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, Slot.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to parse valid Slot from JSON", e);
        }
    }
}
