package it.zwets.sms.scheduler.init;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.spring.security.FlowableUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http  
            .sessionManagement(ses -> ses.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(CsrfConfigurer::disable)      // gets in the way of and is unneeded for REST
            .authorizeHttpRequests(req -> req
                .anyRequest().authenticated())  // see https://stackoverflow.com/a/77339695/2109137
            .httpBasic(bas -> bas.realmName("SMS Scheduler"));

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(IdmIdentityService identityService) {
        LOG.info("Flowable IDM is the Spring UserDetailsService");
        return new FlowableUserDetailsService(identityService);
    }
}
