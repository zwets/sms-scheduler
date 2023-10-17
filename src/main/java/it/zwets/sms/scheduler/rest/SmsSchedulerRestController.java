package it.zwets.sms.scheduler.rest;

import java.util.ArrayList;
import java.util.List;

import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.zwets.sms.scheduler.Schedule;
import it.zwets.sms.scheduler.SmsSchedulerService;

@RestController
public class SmsSchedulerRestController {

	@Autowired
    private SmsSchedulerService theService;

    @PostMapping(value="/schedule", consumes=MediaType.APPLICATION_JSON_VALUE)
    public void scheduleSms(@RequestParam ScheduleArgs arg) {
        theService.scheduleSms(arg.clientId, arg.targetId, arg.uniqueId, arg.schedule, arg.payload);
    }

    @RequestMapping(value="/scheduled", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public List<PIRep> getProcessInstances(@RequestParam String clientId) {
        List<ProcessInstance> pis = theService.getProcessInstances(clientId);
        List<PIRep> dtos = new ArrayList<PIRep>();
        for (ProcessInstance pi : pis) {
            dtos.add(new PIRep(pi.getId(), pi.getName()));
        }
        return dtos;
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
    
    static class PIRep {

        private String id;
        private String name;

        public PIRep(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }
}
