package it.zwets.sms.scheduler.util;

import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helps with the parsing, formattting and conversion of dates.
 * 
 * The application configuration produces a dateHelper bean set
 * for the (configurable) application's time zone.  This bean will
 * format datetimes as ISO-8601 instants with the given offset.
 * 
 * <b>Note:<b> this does <i>not</i> mean that the application works
 * with local (i.e. zoneless) times anywhere.  Just that as a
 * convenience to the end user, times are printed with their offset.
 * 
 * @author zwets
 */
public class DateHelper {
	
	public final Logger LOG = LoggerFactory.getLogger(DateHelper.class);
	
	private final DateTimeFormatter formatter;
	
	/**
	 * Produce a DateHelper that formats datetime with the given zoneOffset.
	 * @param zoneOffset
	 */
	public DateHelper(ZoneOffset zoneOffset) {
	    this.formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zoneOffset);
	}

	/**
	 * Wrapper for new Date() so it works in expressions.
	 * @return the now moment
	 */
	public Date now() {
		return new Date();
	}

    /**
     * Formats instant as ISO-8601 with the zone offset set in the constructor
     * @param t an instant or null
     * @return null or ISO 8601 string with offset
     */
    public String format(Instant t) {
        return t == null ? null : formatter.format(t.truncatedTo(SECONDS));
    }
    
	/**
	 * Formats date as ISO-8601 with the zone offset set in the constructor
	 * @param d a date or null
	 * @return null or ISO 8601 string with offset
	 */
	public String format(Date d) {
	    return d == null ? null : formatter.format(d.toInstant().truncatedTo(SECONDS));
	}
	
	/**
	 * Parses ISO 8601 datetime to Date object
	 * @param iso the ISO string
	 * @return the Date or null if iso is null
	 */
	public Date parseDate(String iso) {
	    return iso == null ? null : Date.from(Instant.from(formatter.parse(iso)));
	}
}
