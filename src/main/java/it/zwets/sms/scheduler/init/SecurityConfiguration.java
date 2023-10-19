package it.zwets.sms.scheduler.init;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.User;
import org.flowable.spring.security.FlowableUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
//@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(SecurityConfiguration.class);
	
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//    	http
//    		.csrf(csrf -> csrf
//    				.disable())
//    		.authorizeHttpRequests(req -> req
//    				.anyRequest().authenticated())
//    		.httpBasic(Customizer.withDefaults())
//    		.sessionManagement(ses -> ses
//    				.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
//    	
//    	return http.build();
//    }

	@Autowired
	IdmIdentityService identityService;
	
    @Bean
    public UserDetailsService userDetailsService() {
    	LOG.info("Flowable IDM is the Spring UserDetailsService");

	LOG.warn("TODO: remove temporary admin user");
    	User user = identityService.newUser("admin");
    	user.setPassword("test");
    	user.setDisplayName("Test Admin");
    	user.setEmail("io@zwets.it");
    	identityService.saveUser(user);

    	return new FlowableUserDetailsService(identityService);
    }
}
