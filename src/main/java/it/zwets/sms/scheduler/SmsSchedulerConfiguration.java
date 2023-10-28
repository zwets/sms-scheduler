package it.zwets.sms.scheduler;

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
    
    public static final class Constants {

        // Process names and globals
        
        public static final String APP_PROCESS_NAME = "smsSchedulerProcess";
        
        // Variable names

        public static final String VAR_CLIENT_ID = "clientId";
        public static final String VAR_TARGET_ID = "targetId";
        public static final String VAR_SMS_SCHEDULE = "smsSchedule";
        public static final String VAR_SMS_PAYLOAD = "smsPayload";
        
        public static final String VAR_SMS_DUETIME = "smsDueTime";
        public static final String VAR_SMS_STATUS = "smsStatus";
        public static final String VAR_SMS_RETRIES = "smsRetries";
        
        // Values for VAR_SMS_STATUS
        
        public static final String SMS_STATUS_NEW = "NEW"; // TODO: remove or replace
        public static final String SMS_STATUS_SCHEDULED = "SCHEDULED";
        public static final String SMS_STATUS_ENROUTE = "ENROUTE";
        public static final String SMS_STATUS_EXPIRED = "EXPIRED";
        public static final String SMS_STATUS_SENT = "SENT";
        public static final String SMS_STATUS_DELIVERED = "DELIVERED";
    }


    @Value("${sms.scheduler.app.time-zone:Z}")
    private String appTimeZone;

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
        return new TriageDelegate();
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
}
