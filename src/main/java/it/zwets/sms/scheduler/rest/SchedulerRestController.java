package it.zwets.sms.scheduler.rest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import it.zwets.sms.scheduler.Schedule;
import it.zwets.sms.scheduler.SmsSchedulerService;
import it.zwets.sms.scheduler.SmsSchedulerService.SmsStatus;

@RestController
public class SchedulerRestController {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerRestController.class);

    @Autowired
    private SmsSchedulerService theService;

    @GetMapping(path = "/schedule", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SmsStatus> getSmsStatuses() {
        LOG.info("REST GET /schedule");
        return theService.getStatusList();
    }

    @GetMapping(path = "/schedule/{clientId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SmsStatus> getSmsStatuses(@PathVariable String clientId) {
        LOG.info("REST GET /schedule/client={}", clientId);
        return theService.getStatusList(clientId);
    }

    @GetMapping(path = "/schedule/{clientId}/{targetId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SmsStatus> getSmsStatuses(@PathVariable String clientId, @PathVariable String targetId) {
        LOG.info("REST GET /schedule/client={}/targetId={}", clientId, targetId);
        return theService.getStatusList(clientId, targetId);
    }

    @GetMapping(path = "/schedule/{clientId}/{targetId}/{uniqueId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public SmsStatus getSmsStatuses(@PathVariable String clientId, @PathVariable String targetId,
            @PathVariable String uniqueId) {
        LOG.info("REST GET /schedule/client={}/targetId={}/uniqueId={}", clientId, targetId, uniqueId);
        SmsStatus smsStatus = theService.getSmsStatus(clientId, targetId, clientId);
        if (smsStatus != null) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
        }
        return smsStatus;
    }

    @DeleteMapping(path = "/schedule/{clientId}/{targetId}/{uniqueId}")
    public void cancelSms(@PathVariable String clientId, @PathVariable String targetId, @PathVariable String uniqueId) {
        LOG.info("REST GET /schedule/client={}/targetId={}/uniqueId={}", clientId, targetId, uniqueId);
        theService.cancelSms(clientId, targetId, uniqueId);
    }

    @ResponseBody
    @PostMapping(path = "/schedule", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SmsStatus scheduleSms(@Validated @RequestBody ScheduleArgs arg) {
        LOG.info("REST POST to /schedule API");
        return theService.scheduleSms(arg.clientId, arg.targetId, arg.uniqueId, arg.schedule, arg.payload);
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
