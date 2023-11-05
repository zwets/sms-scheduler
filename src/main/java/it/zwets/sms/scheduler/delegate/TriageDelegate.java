package it.zwets.sms.scheduler.delegate;

import java.time.Duration;
import java.time.Instant;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.zwets.sms.scheduler.SmsSchedulerConfiguration.Constants;
import it.zwets.sms.scheduler.util.Scheduler;

/**
 * Triages incoming SMS schedule request and retries.
 * 
 * @author zwets
 */
public class TriageDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(TriageDelegate.class);

    private final Duration waitAfterFail;
    private final Duration maxAddJitter;
    
    /**
     * Constructor with its injectable configuration parameters.
     * @param maxAddJitter maximum random duration to add to initial send
     * @param waitAfterFail duration to wait before rescheduling after fail
     */
    public TriageDelegate(Duration waitAfterFail, Duration maxAddJitter) {
        LOG.debug("TriageDelegate created with "
                + "waitAfterFail = {} and maxAddJitter = {}", waitAfterFail, maxAddJitter);
        this.waitAfterFail = waitAfterFail;
        this.maxAddJitter = maxAddJitter;
    }
    
    @Override
    public void execute(DelegateExecution execution) {

        LOG.debug("TriageDelegate starting triage on execution {}", StringUtils.substringBefore(execution.getId(), '-'));

        String clientId = execution.getVariable(Constants.VAR_CLIENT_ID, String.class);
        String targetId = execution.getVariable(Constants.VAR_TARGET_ID, String.class);
        String clientKey = execution.getVariable(Constants.VAR_CLIENT_KEY, String.class);
        String schedule = execution.getVariable(Constants.VAR_SCHEDULE, String.class);
        Scheduler scheduler = new Scheduler(schedule);

        String smsStatus = execution.hasVariable(Constants.VAR_SMS_STATUS)
                ? execution.getVariable(Constants.VAR_SMS_STATUS, String.class)
                : Constants.SMS_STATUS_NEW;
        
        int smsRetries = execution.hasVariable(Constants.VAR_SMS_RETRIES)
                ? execution.getVariable(Constants.VAR_SMS_RETRIES, Integer.class)
                : -1;

        Instant smsDueTime = null;
        Instant deadlineInstant = null;
        
        if (smsRetries == -1) { // first time around

            smsDueTime = scheduler.getFirstAvailableInstant();
            
            if (smsDueTime != null) {
                
                // Unless SMS is due in the next minute, add (configurable) random jitter
                if (smsDueTime.isAfter(Instant.now().plusMillis(60 * 1000))) {

                    long jitter = Math.round(Math.random() * maxAddJitter.getSeconds());
                    LOG.debug("Adding {}s jitter", jitter);
                    
                    deadlineInstant = scheduler.getDeadlineInstant(smsDueTime).plusSeconds(jitter);
                    smsDueTime = smsDueTime.plusSeconds(jitter);
                }

                LOG.info("Scheduling new request: {}:{}:{}:{}:{}",
                        StringUtils.substringBefore(execution.getProcessInstanceId(),'-'),
                        clientId, targetId, clientKey, smsDueTime);
            }
            else {
                LOG.warn("Request schedule expired on arrival, not scheduling a send");
            }
        }
        else {
            Instant prevDueTime = execution.getVariable(Constants.VAR_SMS_DUETIME, Instant.class);
            
            if (prevDueTime == null) {
                LOG.error("Should not happen: retry execution with no previous smsDueTime");
                prevDueTime = Instant.now();
            }

            smsDueTime = scheduler.getFirstAvailableInstant(prevDueTime.plus(waitAfterFail));
            deadlineInstant = scheduler.getDeadlineInstant(smsDueTime);
            
            if (smsDueTime == null) {
                LOG.info("Not retrying [{}] request, schedule exhausted: {}:{}:{}:{}", 
                        smsRetries+1,
                        StringUtils.substringBefore(execution.getProcessInstanceId(),'-'),
                        clientId, targetId, clientKey);
            }
            else {
                LOG.info("Will retry [{}] at {}: {}:{}:{}:{}", 
                        smsRetries+1, smsDueTime,
                        StringUtils.substringBefore(execution.getProcessInstanceId(),'-'),
                        clientId, targetId, clientKey);
            }
        }
        
        smsStatus = smsDueTime == null ? Constants.SMS_STATUS_EXPIRED : Constants.SMS_STATUS_SCHEDULED;
        String smsDeadline = deadlineInstant == null ? null : deadlineInstant.toString();
        
        execution.setVariable(Constants.VAR_SMS_DUETIME, smsDueTime);
        execution.setVariable(Constants.VAR_SMS_DEADLINE, smsDeadline);
        execution.setVariable(Constants.VAR_SMS_STATUS, smsStatus);
        execution.setVariable(Constants.VAR_SMS_RETRIES, smsRetries + 1);
    }
}