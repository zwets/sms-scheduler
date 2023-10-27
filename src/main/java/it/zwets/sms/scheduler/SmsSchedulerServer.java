package it.zwets.sms.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(proxyBeanMethods = false)
public class SmsSchedulerServer {

	public static void main(String[] args) {
		SpringApplication.run(SmsSchedulerServer.class, args);
	}
}
