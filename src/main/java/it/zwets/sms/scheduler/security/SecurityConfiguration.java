package it.zwets.sms.scheduler.security;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.spring.security.FlowableUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

import jakarta.servlet.DispatcherType;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(ses -> ses.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())  // not needed for REST
            .authorizeHttpRequests(req -> req
                    .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).authenticated()
//                    .requestMatchers(HttpMethod.GET, "/iam/accounts/{id}")
//                          .access(new WebExpressionAuthorizationManager(
//                              "hasRole('ADMIN') || authentication.name == #id"))
//                  .requestMatchers("/iam/**").hasRole("ADMIN")
//                  .requestMatchers("/actuator/**").hasRole("ADMIN")
//                  .requestMatchers("/schedule/**").hasRole("USER")
//                . .requestMatchers(HttpMethod.G0ET, "/schedule/**", "/admin/**").hasAuthority("ADMIN")
                    .anyRequest().denyAll())
//                  .anyRequest().authenticated())
            .httpBasic(bas -> bas.realmName("SMS Scheduler"));

        return http.build();
    }
    
    @Bean
    public UserDetailsService userDetailsService(IdmIdentityService identityService) {
        LOG.info("Flowable IDM is the Spring UserDetailsService");
        return new FlowableUserDetailsService(identityService);
    }
}
