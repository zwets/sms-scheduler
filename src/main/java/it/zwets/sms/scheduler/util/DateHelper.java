package it.zwets.sms.scheduler.util;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.zwets.sms.scheduler.Constants;

/**
 * Helps convert between date types and do date maths.
 * @author zwets
 */
@Component
public class DateHelper {
	
	public final Logger LOG = LoggerFactory.getLogger(DateHelper.class);
	
	/**
	 * Wrapper for new Date() so it works in expressions.
	 * @return the now moment
	 */
	public Date now() {
		return new Date();
	}
	
	/**
	 * Return a new Date object that is d with its date and time set to noon EAT
	 * @param d the date object to operate on, or null
	 * @return null if d is null, or a new Date object
	 */
	public Date toNoon(Date d) {
		return toTime(d, 12, 0);
	}

	public String toISO8601(Date d) {
		return "TODO";
	}
	
	/**
	 * Return a new Date object that is d with its time changed.
	 * @param d the date object to operate on, or null
	 * @return null if d is null, or a new Date object
	 */
	public Date toTime(Date d, int hour, int min) {
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone(Constants.APP_TIME_ZONE));
		c.setTime(d);
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.MINUTE, min);
		c.set(Calendar.SECOND, 0);

		return c.getTime();
	}

	/**
	 * Return the number of days since date.
	 * @param date, not null
	 * @return the number of days, rounding to nearest whole day
	 */
	public long daysSince(Date date) {

		final double millisPerDay = 24 * 60 * 60 * 1000;
		double millis = (new Date()).getTime() - date.getTime();

		return Math.round(millis / millisPerDay);
	}
}
