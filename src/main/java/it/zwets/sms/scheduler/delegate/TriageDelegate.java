package it.zwets.sms.scheduler.delegate;

import java.util.Date;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.zwets.sms.scheduler.Constants;
import it.zwets.sms.scheduler.util.DateHelper;
import it.zwets.sms.scheduler.util.ScheduleHelper;


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
				execution.getVariable(Constants.VAR_SMS_STATUS, String.class) : null;
				
		int retryCount = execution.hasVariable(Constants.VAR_RETRY_COUNT) ?
				execution.getVariable(Constants.VAR_RETRY_COUNT, Integer.class) : -1;
		
		ScheduleHelper smsSchedule = new ScheduleHelper(
				execution.getVariable(Constants.VAR_SMS_SCHEDULE, String.class));

		if (retryCount == -1) { // first time around
			Date smsDueTime = smsSchedule.nextAvailable();
		}
		else {
			Date prevDueTime = retryCount != -1 ?
					execution.getVariable(Constants.VAR_SMS_DUETIME, Date.class) : null;
		}		
		
            // Set the result ID on the execution
        
		execution.setVariable(Constants.VAR_RETRY_COUNT, retryCount + 1);
//		execution.setVariable(Constants.VAR_SMS_DUETIME, smsDueTime);
		
//        	LOG.info("Schedule SMS triaged: {}, count {}", 
//                VAR_RPT_CONTENT_ID, reportItem.getId(), reportItem.getContentStoreId() );
	}
}
