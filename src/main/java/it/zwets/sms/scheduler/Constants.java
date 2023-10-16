package it.zwets.sms.scheduler;

/**
 * Defines the string constants used in the SmsScheduler model.
 * @author zwets
 */
public class Constants {

	// Process names and globals
	
	public static final String APP_PROCESS_NAME = "smsSchedulerProcess";
	public static final String APP_TIME_ZONE = "GMT+03:00";
	
	// Variable names

	public static final String VAR_CLIENT_ID = "clientId";
	public static final String VAR_TARGET_ID = "targetId";
	public static final String VAR_UNIQUE_ID = "uniqueId";

	public static final String VAR_SMS_SCHEDULE = "smsSchedule";
	public static final String VAR_SMS_DUETIME = "smsDueTime";
	public static final String VAR_SMS_STATUS = "smsStatus";
	public static final String VAR_RETRY_COUNT = "smsRetryCount";
	
	// Values for VAR_SMS_STATUS
	
	public static final String SMS_STATUS_SCHEDULED = "SCHEDULED";
	public static final String SMS_STATUS_ENROUTE = "ENROUTE";
	public static final String SMS_STATUS_EXPIRED = "EXPIRED";
	public static final String SMS_STATUS_SENT = "SENT";
	public static final String SMS_STATUS_DELIVERED = "DELIVERED";
}
