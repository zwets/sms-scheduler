/**
 * 
 */
package it.zwets.sms.scheduler.init;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.zwets.sms.scheduler.SmsSchedulerConfiguration.Constants;

/**
 * Initialises the SmsSchedulerProcess process variables.
 * 
 * @author zwets
 */
public class SmsSchedulerProcessInitialiser implements ExecutionListener {

	private static final Logger LOG = LoggerFactory.getLogger(SmsSchedulerProcessInitialiser.class);
	private static final long serialVersionUID = 1L;

	@Override
	public void notify(DelegateExecution execution) {

		// Check that required variables are set
		
		String clientId = execution.getVariable(Constants.VAR_CLIENT_ID, String.class);

		if (clientId == null) {
		    LOG.error("Process must set variable {}", Constants.VAR_CLIENT_ID);
		    throw new RuntimeException("Process cannot be started without client ID");
		}
		
        String schedule = execution.getVariable(Constants.VAR_SCHEDULE, String.class);

        if (schedule == null) {
            LOG.error("Process must set variable {}", Constants.VAR_SCHEDULE);
            throw new RuntimeException("Process cannot be started without schedule");            
        }

        String payload = execution.getVariable(Constants.VAR_PAYLOAD, String.class);

        if (payload == null) {
            LOG.error("Process must set variable {}", Constants.VAR_PAYLOAD);
            throw new RuntimeException("Process cannot be started without payload");            
        }

		// Initialise the smsStatus and retries variable
		
		execution.setVariable(Constants.VAR_SMS_STATUS, Constants.SMS_STATUS_NEW);
        execution.setVariable(Constants.VAR_SMS_RETRIES, -1);
		
        if (LOG.isDebugEnabled()) {
            LOG.debug("Process initialised: I:C:B:K:T:S:P:U {}:{}:{}:{}:{}:{}:{}:{}", 
                StringUtils.substringBefore(execution.getProcessInstanceId(), '-'),
                clientId, 
                execution.getVariable(Constants.VAR_BATCH_ID),
		        execution.getVariable(Constants.VAR_CLIENT_KEY),
                execution.getVariable(Constants.VAR_TARGET_ID),
		        schedule,
		        payload,
		        execution.getVariable(Constants.VAR_USER_ID));
        }
	}
}
