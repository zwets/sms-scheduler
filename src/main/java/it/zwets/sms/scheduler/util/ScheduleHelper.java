package it.zwets.sms.scheduler.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the SMS schedule specified by the invoker.
 * 
 * The SmsSchedule is an ordered list of timeslots in which the
 * SMS may be sent.
 * 
 * @author zwets
 */
public class ScheduleHelper {
	
	/**
	 * Create SmsSchedule for the given string, which must give a
	 * comma-separated list of space-separated epoch numbers, i.e.
	 * seconds since 1-1-1970.
	 * 
	 * Note: java.util.Data is MILLIseconds in the epoch!.
	 * 
	 * @param scheduleSpec
	 */
	public ScheduleHelper(String scheduleSpec) {
		
		Pattern p = Pattern.compile("^(?:\\s*(\\d+)\\s+(\\d+))(?:\\s+(\\d+)\\s+(\\d+))*$");
		Matcher m = p.matcher(scheduleSpec);
		
		if (!m.matches()) {
			throw new RuntimeException("not a valid SMS schedule: " + scheduleSpec);
		}

		for (int i = 1; i <= m.groupCount(); i += 2) {
			
			long from = Long.parseLong(m.group(i)) * 1000;
			long upto = Long.parseLong(m.group(i+1)) * 1000;
			
			if (upto < from) {
				throw new RuntimeException("not a valid SMS schedule: inverted time slot");
			}
			
			int t = 0;
			while (t < timeSlots.size()) {
				TimeSlot s = timeSlots.get(t);
				if (from < s.from) {
					if (upto >= s.from) {
						throw new RuntimeException("not a valid SMS schedule: overlapping slots");
					}
					break;
				}
				++t;
			}
			timeSlots.add(t, new TimeSlot(from, upto));
		}
	}

	/**
	 * Return next available instant in the schedule,
	 * which may be now (we add 1s), the start of the
	 * next available slot.
	 * 
	 * @return
	 */
	public Date nextAvailable() {
		
		long now = new Date().getTime();
		
		int t = 0;
		while (t < timeSlots.size()) {
			TimeSlot s = timeSlots.get(t);
			if (s.from <= now && now <= s.upto)
				return new Date(now + 1000);
			else if (now < s.from)
				return new Date(s.from);
			++t;
		}
		
		return null; // no slots
	}
	
	final class TimeSlot {
		long from, upto;
		TimeSlot(long from, long upto) {
			this.from = from;
			this.upto= upto;
		}
	}
	
	private List<TimeSlot> timeSlots = new ArrayList<TimeSlot>();
}
