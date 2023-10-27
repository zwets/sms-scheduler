package it.zwets.sms.scheduler;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.stereotype.Component;

/**
 * Defines the application properties
 */
@ConfigurationProperties("sms-scheduler")
public class SmsSchedulerConfiguration {
    
    private static final Logger LOG = LoggerFactory.getLogger(SmsSchedulerConfiguration.class);

    @DurationUnit(ChronoUnit.SECONDS)
    private final Duration sessionTimeout;
    
    private final Duration readTimeout;
    
    private final Period fooBar;
    
    private final LocalDate birthDay;
    
    public SmsSchedulerConfiguration(
            @DurationUnit(ChronoUnit.SECONDS) @DefaultValue("30s") Duration sessionTimeout,
            @DefaultValue("1000ms") Duration readTimeout,
            @DefaultValue("1w") Period fooBar,
            @DefaultValue("1968-05-18") LocalDate birthDay) {
        
        this.sessionTimeout = sessionTimeout;
        this.readTimeout = readTimeout;
        this.fooBar = fooBar;
        this.birthDay = birthDay;
    }

    public Duration getSessionTimeout() {
        return sessionTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public Period getFooBar() {
        return fooBar;
    }

    public LocalDate getBirthDay() {
        return birthDay;
    }
}
