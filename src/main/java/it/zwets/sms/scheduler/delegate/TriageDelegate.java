package it.zwets.sms.scheduler.delegate;

import java.time.Instant;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.zwets.sms.scheduler.Constants;
import it.zwets.sms.scheduler.Schedule;

/**
 * Triages incoming SMS schedule request and retries.
 * 
 * @author zwets
 */
@Component
public class TriageDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(TriageDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {

        LOG.debug("start Triage");

        String clientId = execution.getVariable(Constants.VAR_CLIENT_ID, String.class);
        String targetId = execution.getVariable(Constants.VAR_TARGET_ID, String.class);

        Schedule smsSchedule = execution.getVariable(Constants.VAR_SMS_SCHEDULE, Schedule.class);
        String smsStatus = execution.hasVariable(Constants.VAR_SMS_STATUS)
                ? execution.getVariable(Constants.VAR_SMS_STATUS, String.class)
                : Constants.SMS_STATUS_NEW;
        int smsRetries = execution.hasVariable(Constants.VAR_SMS_RETRIES)
                ? execution.getVariable(Constants.VAR_SMS_RETRIES, Integer.class)
                : -1;

        Instant smsDueTime;
        if (smsRetries == -1) { // first time around
            smsDueTime = smsSchedule.getFirstAvailableInstant();
        } else {
            Instant prevDueTime = execution.getVariable(Constants.VAR_SMS_DUETIME, Instant.class);
            smsDueTime = smsSchedule.getFirstAvailableInstant(prevDueTime.plusSeconds(10 * 60)); // TODO: make property
        }

        smsStatus = smsDueTime == null ? Constants.SMS_STATUS_EXPIRED : Constants.SMS_STATUS_SCHEDULED;

        execution.setVariable(Constants.VAR_SMS_DUETIME, smsDueTime);
        execution.setVariable(Constants.VAR_SMS_STATUS, smsStatus);
        execution.setVariable(Constants.VAR_SMS_RETRIES, smsRetries + 1);

        LOG.info("Schedule SMS triaged: I:C:T:B:R:S:D {}:{}:{}:{}:{}:{}", 
                execution.getProcessInstanceId(), clientId, targetId, execution.getProcessInstanceBusinessKey(),
                smsRetries, smsStatus, smsDueTime);
    }
}
