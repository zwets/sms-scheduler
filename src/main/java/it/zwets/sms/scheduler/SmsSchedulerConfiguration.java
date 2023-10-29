package it.zwets.sms.scheduler;

import java.time.Duration;
import java.time.ZoneOffset;

import org.flowable.engine.RuntimeService;
import org.flowable.idm.api.IdmIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.zwets.sms.scheduler.delegate.TriageDelegate;
import it.zwets.sms.scheduler.diag.ProcessLogger;
import it.zwets.sms.scheduler.diag.TaskLogger;
import it.zwets.sms.scheduler.diag.VariableLogger;
import it.zwets.sms.scheduler.iam.IamService;
import it.zwets.sms.scheduler.init.SmsSchedulerProcessInitialiser;
import it.zwets.sms.scheduler.util.DateHelper;

/**
 * Application configuration.
 * 
 * Produces the beans needed by the process engine.  Defines constants for
 * the string names and values used in the process definition.
 */
@Configuration(proxyBeanMethods = false)
public class SmsSchedulerConfiguration {
    
    private static final Logger LOG = LoggerFactory.getLogger(SmsSchedulerConfiguration.class);    

    @Value("${sms.scheduler.app.time-zone:Z}")
    private String appTimeZone;
    
    @Value("${sms.scheduler.config.max-add-jitter}")
    private Duration maxAddJitter;

    @Value("${sms.scheduler.config.ack-send-timeout}")
    private Duration ackSendTimeout;

    @Value("${sms.scheduler.config.ack-recv-timeout}")
    private Duration ackRecvTimeout;

    @Value("${sms.scheduler.config.wait-after-fail}")
    private Duration waitAfterFail;

    @Value("${sms.scheduler.diag.processes.enabled:false}")
    private boolean diagProcessesEnabled;

    @Value("${sms.scheduler.diag.processes.detailed:false}")
    private boolean diagProcessesDetailed;
    
    @Value("${sms.scheduler.diag.tasks.enabled:false}")
    private boolean diagTasksEnabled;

    @Value("${sms.scheduler.diag.tasks.detailed:false}")
    private boolean diagTasksDetailed;

    private final RuntimeService runtimeService;
    private final IdmIdentityService idmIdentityService;
    
    /**
     * Public constructor
     * @param runtimeService
     * @param idmIdentityService
     */
    public SmsSchedulerConfiguration(RuntimeService runtimeService, IdmIdentityService idmIdentityService) {
        this.runtimeService = runtimeService;
        this.idmIdentityService = idmIdentityService;
    }
    
    @Bean 
    public IamService iamService() {
        LOG.debug("Creating IamService");
        return new IamService(idmIdentityService);
    }
    
    @Bean
    public SmsSchedulerProcessInitialiser smsSchedulerProcessInitialiser() {
        return new SmsSchedulerProcessInitialiser();
    }
    
    @Bean
    public TriageDelegate triageDelegate() {
        return new TriageDelegate(waitAfterFail, maxAddJitter);
    }
    
    /**
     * Timeout expression for the initial send acknowledgement border condition
     * @return a period defined in the application properties sms.scheduler.config.ack-send-timeout
     */
    public Duration getAckSendTimeout() {
        return ackSendTimeout;
    }

    /**
     * Timeout expression for the wait for the delivery acknowledgement timer
     * @return a period defined by the application property sms.scheduler.config.ack-recv-timeout
     */
    public Duration getAckRecvTimeout() {
        return ackRecvTimeout;
    }
    
    @Bean
    public DateHelper dateHelper() {
        LOG.debug("Creating DateHelper bean for time zone {}", appTimeZone);
        return new DateHelper(ZoneOffset.of(appTimeZone));
    }
    
    @Bean
    public ProcessLogger processLogger() {
        return new ProcessLogger(new VariableLogger(runtimeService), diagProcessesEnabled, diagProcessesDetailed);
    }

    @Bean
    public TaskLogger taskLogger() {
        return new TaskLogger(new VariableLogger(runtimeService), diagTasksEnabled, diagTasksDetailed);
    }

    /**
     * Defines as constants the string names and values used in the model.
     */
    public static final class Constants {

        // Process names and globals
        
        public static final String APP_PROCESS_NAME = "smsSchedulerProcess";
        
        // Variable names (set on the process instance)

        public static final String VAR_CLIENT_ID = "clientId";
        public static final String VAR_TARGET_ID = "targetId";
        //public static final String VAR_CLIENT_KEY = not a variable, is businessKey
        public static final String VAR_SCHEDULE = "schedule";
        public static final String VAR_PAYLOAD = "payload";
        
        public static final String VAR_SMS_DUETIME = "smsDueTime";
        public static final String VAR_SMS_DEADLINE = "smsDeadline";
        public static final String VAR_SMS_STATUS = "smsStatus";
        public static final String VAR_SMS_RETRIES = "smsRetries";
 
        // Configuration expressions (getters below)
        
        public static final String CFG_ACK_SEND_TIMEOUT = "ackSendTimeout";
        public static final String CFG_ACK_RECV_TIMEOUT = "ackRecvTimeout";
        
        // Values for the VAR_SMS_STATUS variable
        
        public static final String SMS_STATUS_NEW = "NEW"; // TODO: remove or replace
        public static final String SMS_STATUS_SCHEDULED = "SCHEDULED";
        public static final String SMS_STATUS_ENROUTE = "ENROUTE";
        public static final String SMS_STATUS_EXPIRED = "EXPIRED";
        public static final String SMS_STATUS_SENT = "SENT";
        public static final String SMS_STATUS_DELIVERED = "DELIVERED";
        
        // Channel and event names (tied to the *.event and *.channel definitions)
        
        public static final String CHANNEL_SCHEDULE_SMS = "scheduleSmsChannel";
        public static final String EVENT_KEY_SCHEDULE_SMS = "scheduleSmsEvent";
        public static final String CHANNEL_SEND_SMS = "sendSmsChannel";
        public static final String EVENT_KEY_SEND_SMS = "sendSmsEvent";
        public static final String CHANNEL_SMS_STATUS = "smsStatusChannel";
        public static final String EVENT_KEY_SMS_STATUS = "smsStatusEvent";
    }
}
