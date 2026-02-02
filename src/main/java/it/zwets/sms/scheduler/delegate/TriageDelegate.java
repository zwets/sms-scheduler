package it.zwets.sms.scheduler.delegate;

import java.time.Duration;
import java.time.Instant;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.zwets.sms.scheduler.SmsSchedulerConfiguration.Constants;
import it.zwets.sms.scheduler.TargetBlockerService;
import it.zwets.sms.scheduler.util.Scheduler;

/**
 * Triages incoming SMS schedule request and retries.
 * 
 * @author zwets
 */
public class TriageDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(TriageDelegate.class);

    private final TargetBlockerService blockerService;
    private final Duration waitAfterFail;
    private final Duration maxAddJitter;
    
    /**
     * Constructor with its injectable configuration parameters.
     * @param maxAddJitter maximum random duration to add to initial send
     * @param waitAfterFail duration to wait before rescheduling after fail
     */
    public TriageDelegate(TargetBlockerService blockerService, Duration waitAfterFail, Duration maxAddJitter) {
        LOG.debug("TriageDelegate created with "
                + "waitAfterFail = {} and maxAddJitter = {}", waitAfterFail, maxAddJitter);
        this.blockerService = blockerService;
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
        
        if (blockerService.isTargetBlocked(clientId, targetId)) {
            LOG.debug("Target is blocked: {}:{}", clientId, targetId);
            execution.setVariable(Constants.VAR_SMS_STATUS, Constants.SMS_STATUS_BLOCKED);
            
            return;
        }
        
        if (smsRetries == -1) { // first time around

            smsDueTime = scheduler.getFirstAvailableInstant();
            
            if (smsDueTime != null) {
                
                deadlineInstant = scheduler.getDeadlineInstant(smsDueTime);
                
                // Unless SMS is due in the next minute, add (configurable) random jitter
                if (smsDueTime.isAfter(Instant.now().plusMillis(60 * 1000))) {

                    long jitter = Math.round(Math.random() * maxAddJitter.getSeconds());
                    LOG.debug("Adding {}s jitter", jitter);
                    
                    smsDueTime = smsDueTime.plusSeconds(jitter);
                    deadlineInstant = deadlineInstant.plusSeconds(jitter);
                }

                LOG.info("Scheduling new request at {}: {}:{}:{}:{}", smsDueTime,
                        StringUtils.substringBefore(execution.getProcessInstanceId(),'-'),
                        clientId, targetId, clientKey);
            }
            else {
                LOG.warn("Request schedule expired on arrival, not scheduling a send");
            }
        }
        else { // this is a retry after a message has gone through the process

            // The incoming message may have status EXPIRED, FAILED or INVALID, and we
            // retry in all cases, for the following reasons:
            //  - FAILED means gateway or SMSC failed to send an otherwise good message,
            //    so obviously we retry (if there is a next slot)
            //  - EXPIRED means either Gateway received the request past its deadline
            //    or the SMSC reported it expired, so we retry if there's next slot
            //  - INVALID would mean that Gateway or SMSC rejected the message for some
            //    reason (so we shouldn't retry).  However, if we get INVALID then this
            //    must be from the RECV wait state, because if the SEND wait state ended
            //    in INVALID the process would have ended in INVALID (see BPMN diagram).
            //    This means that the INVALID cannot come from Gateway itself (and mean:
            //    invalid forever), nor from SMSC initial look at the message.  It must
            //    be a translation of a later SMSC notification, almost certainly REJECT
            //    (prior to 1.3.0 Gateway also translated UNDELIV to INVALID; we changed
            //    that to FAILED).  The INVALID then almost certainly is a REJECT from
            //    SMSC, but we retry (note: it did not reject the first time around).
            //    All this might be easier if SMSCs would stick to standard SMPP error
            //    codes: https://smpp.org/smpp-error-codes.html

            Instant prevDueTime = execution.getVariable(Constants.VAR_SMS_DUETIME, Instant.class);
            
            if (prevDueTime == null) {
                LOG.error("Should not happen: retry execution with no previous smsDueTime");
                prevDueTime = Instant.now();
            }

            smsDueTime = prevDueTime.plus(waitAfterFail);

            Instant now = Instant.now();
            if (smsDueTime.isBefore(now)) {
                smsDueTime = now;
            }

            smsDueTime = scheduler.getFirstAvailableInstant(smsDueTime);
            deadlineInstant = scheduler.getDeadlineInstant(smsDueTime);
            
            if (smsDueTime == null) {
                LOG.info("No retry [{}] for {} request, schedule exhausted: {}:{}:{}:{}",
                        smsRetries+1, smsStatus,
                        StringUtils.substringBefore(execution.getProcessInstanceId(),'-'),
                        clientId, targetId, clientKey);
            }
            else {
                LOG.info("Scheduling retry [{}] for {} request at {}: {}:{}:{}:{}",
                        smsRetries+1, smsStatus, smsDueTime,
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