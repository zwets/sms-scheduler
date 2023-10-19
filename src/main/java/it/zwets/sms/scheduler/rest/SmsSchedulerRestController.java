package it.zwets.sms.scheduler.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import it.zwets.sms.scheduler.Schedule;
import it.zwets.sms.scheduler.SmsSchedulerService;
import it.zwets.sms.scheduler.SmsSchedulerService.SmsStatus;

@RestController
public class SmsSchedulerRestController {

	private static final Logger LOG = LoggerFactory.getLogger(SmsSchedulerRestController.class);

	@Autowired
    private SmsSchedulerService theService;

    @RequestMapping(value="/scheduled", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public List<SmsStatus> getSmsStatuses(@RequestParam Map<String,String> params) {
    	LOG.info("REST GET to /scheduled API");
 
    	for (String key : params.keySet()) {
    		LOG.info("Key {}: Value: {} ", key, params.get(key));
    	}
 
    	if (params.containsKey("client")) {
    		if (params.containsKey("target")) {
    			return theService.getStatusList(params.get("client"), params.get("target"));
    		}
    		else {
    			return theService.getStatusList(params.get("client"));
    		}
    	}
    	else {
    		return new ArrayList<SmsStatus>();
    	}
    }

	@ResponseBody
    @PostMapping(value="/schedule", consumes=MediaType.APPLICATION_JSON_VALUE)
    public SmsStatus scheduleSms(@Validated @RequestBody ScheduleArgs arg) {
    	LOG.info("REST POST to /schedule API");
        
    	return theService.scheduleSms(arg.clientId, arg.targetId, arg.uniqueId, arg.schedule, arg.payload);
    }

    @DeleteMapping(value="/cancel", produces=MediaType.APPLICATION_JSON_VALUE)
    public List<SmsStatus> cancelSms(@RequestParam Map<String,String> params) {
    	LOG.info("REST DELETE to /cancel API");

    	if (params.containsKey("client")) {
    		if (params.containsKey("unique")) {
    			return theService.cancelSms(params.get("client"), params.get("unique"));
    		}
    		else if (params.containsKey("target")) {
    			return theService.cancelTarget(params.get("client"), params.get("target"));
    		}
    		else {
    			return theService.cancelAll(params.get("client"));
    		}
    	}
    	else {
    		return new ArrayList<SmsStatus>();
    	}
    }

    
    static class ScheduleArgs {

    	private String clientId;
    	private String targetId;
    	private String uniqueId;
    	private Schedule schedule;
    	private String payload;

		public String getClientId() {
			return clientId;
		}
		public void setClientId(String clientId) {
			this.clientId = clientId;
		}
		public String getTargetId() {
			return targetId;
		}
		public void setTargetId(String targetId) {
			this.targetId = targetId;
		}
		public String getUniqueId() {
			return uniqueId;
		}
		public void setUniqueId(String uniqueId) {
			this.uniqueId = uniqueId;
		}
		public Schedule getSchedule() {
			return schedule;
		}
		public void setSchedule(Schedule schedule) {
			this.schedule = schedule;
		}
		public String getPayload() {
			return payload;
		}
		public void setPayload(String payload) {
			this.payload = payload;
		}
    }
}
