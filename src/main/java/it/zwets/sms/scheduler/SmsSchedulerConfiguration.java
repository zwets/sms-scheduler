package it.zwets.sms.scheduler;

import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.zwets.sms.scheduler.util.DateHelper;

/**
 * Application configuration.
 */
@Configuration
public class SmsSchedulerConfiguration {
    
    private static final Logger LOG = LoggerFactory.getLogger(SmsSchedulerConfiguration.class);

    @Value("${sms-scheduler.app.time-zone:Z}")
    private String zoneOffset;
    
    @Bean
    public DateHelper dateHelper() {
        LOG.debug("Creating DateHelper for time zone offset {}", zoneOffset);
        return new DateHelper(ZoneOffset.of(zoneOffset));
    }
}
