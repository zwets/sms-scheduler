package it.zwets.sms.scheduler.delegate;

import java.time.Instant;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.zwets.sms.scheduler.Constants;
import it.zwets.sms.scheduler.Schedule;
import it.zwets.sms.scheduler.util.DateHelper;


/**
 * Triages incoming SMS
 * 
 * @author zwets
 */
@Component
public class TriageDelegate implements JavaDelegate {

	private static final Logger LOG = LoggerFactory.getLogger(TriageDelegate.class);

	@Autowired
	private DateHelper dateHelper;
	
	@Override
	public void execute(DelegateExecution execution) {

		LOG.debug("start Schedule SMS Triage");

			// Retrieve execution variables
		
		String clientId = execution.getVariable(Constants.VAR_CLIENT_ID, String.class);
		
		String instanceId = execution.getProcessInstanceBusinessKey();
		
		String smsStatus = execution.hasVariable(Constants.VAR_SMS_STATUS) ?
				execution.getVariable(Constants.VAR_SMS_STATUS, String.class) : Constants.SMS_STATUS_UNBORN;
				
		int retryCount = execution.hasVariable(Constants.VAR_SMS_RETRIES) ?
				execution.getVariable(Constants.VAR_SMS_RETRIES, Integer.class) : -1;
		
		Schedule smsSchedule = execution.getVariable(Constants.VAR_SMS_SCHEDULE, Schedule.class);

		Instant dueTime;
		if (retryCount == -1) { // first time around
			dueTime = smsSchedule.getFirstAvailableInstant();
		}
		else {
			Instant prevDueTime = execution.getVariable(Constants.VAR_SMS_DUETIME, Instant.class);
			dueTime = prevDueTime.plusSeconds(15 * 60);
		}		
		
            // Set the result ID on the execution
        
		execution.setVariable(Constants.VAR_SMS_RETRIES, retryCount + 1);
		execution.setVariable(Constants.VAR_SMS_DUETIME, dueTime);
		
//        	LOG.info("Schedule SMS triaged: {}, count {}", 
//                VAR_RPT_CONTENT_ID, reportItem.getId(), reportItem.getContentStoreId() );
	}
}
