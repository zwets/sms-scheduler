package it.zwets.sms.scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.zwets.sms.scheduler.SmsSchedulerConfiguration.Constants;
import it.zwets.sms.scheduler.dto.Schedule;
import it.zwets.sms.scheduler.dto.Slot;
import it.zwets.sms.scheduler.util.DateHelper;

@Service
public class SmsSchedulerService {

	private static final Logger LOG = LoggerFactory.getLogger(SmsSchedulerService.class);
	
	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private HistoryService historyService;

	@Autowired
	private DateHelper dateHelper;
	
	/** The DTO for reporting status */
    public final record SmsStatus(
            String id, String client, String target, String key, String status, String started, String ended, int retries) { }

    /**
     * Schedule an SMS
     * 
     * @param clientId the client we are operating for
     * @param targetId optional client identification of the target (recipient)
     * @param clientKey optionally unique ID assigned by the client
     * @param slots the time slots within which sending is desired
     * @param payload the encrypted payload to forward on the send queue
     * @return SmsStatus object with the incoming parameter plus assigned unique id and start time
     */
	@Transactional
    public SmsStatus scheduleSms(
    		String clientId, String targetId, String businessKey, 
    		Slot[] slots, String payload) {

		LOG.debug("SmsSchedulerService::scheduleSms({},{},{},{},{})", clientId, targetId, businessKey, slots, payload);
		
		Map<String,Object> vars = new HashMap<String,Object>();
		
		vars.put(Constants.VAR_CLIENT_ID, clientId);
		vars.put(Constants.VAR_TARGET_ID, targetId);
		vars.put(Constants.VAR_SMS_SCHEDULE, new Schedule(slots));
		vars.put(Constants.VAR_SMS_PAYLOAD, payload);

		ProcessInstance pi = runtimeService.startProcessInstanceByKey(Constants.APP_PROCESS_NAME, businessKey, vars);
		
		return new SmsStatus(pi.getId(), clientId, targetId, businessKey, Constants.SMS_STATUS_NEW, dateHelper.format(pi.getStartTime()), null, 0);
    }

	@Transactional
	public void cancelSms(String clientId, String targetId, String uniqueId) {
		LOG.error("NOT IMPLEMENTED: SmsSchedulerService::cancelSms( clientId={},targetId={},uniqueId={} )", clientId, targetId, uniqueId);
		throw new NotImplementedException("SmsScheduleService::cancelSms");
	}

	@Transactional
	public void cancelAllForClient(String clientId) {
		LOG.error("NOT IMPLEMENTED: SmsSchedulerService::cancelAllForClient( clientId={} )", clientId);
		throw new NotImplementedException("SmsScheduleService::cancelAllForClient");
	}

    @Transactional
    public void cancelAllForTarget(String clientId, String targetId) {
        LOG.error("NOT IMPLEMENTED: SmsSchedulerService::cancelAllForTarget( clientId={}, targetId={} )", clientId, targetId);
        throw new NotImplementedException("SmsScheduleService::cancelAllForTarget");
    }
    
    @Transactional
    public SmsStatus getSmsStatus(String id) {
        LOG.trace("SmsSchedulerService::getSmsStatus(id={})", id);
        
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(Constants.APP_PROCESS_NAME)
                .includeProcessVariables()
                .singleResult();
        
        SmsStatus result = null;
               
        if (hpi != null) {
            result = hpiToSmsStatus(hpi);
        }
        
        return result;
    }
    
    @Transactional
    public List<SmsStatus> getStatusList() {
        LOG.trace("SmsSchedulerService::getStatusList()");

        return historyService
            .createHistoricProcessInstanceQuery()
            .processDefinitionKey(Constants.APP_PROCESS_NAME)
            .orderByProcessInstanceStartTime().asc()
            .includeProcessVariables()
            .list().stream()
            .map(this::hpiToSmsStatus)
            .toList();
    }

    @Transactional
    public List<SmsStatus> getStatusList(String clientId) {
		LOG.trace("SmsSchedulerService::getStatusList(clientId={})", clientId);

		return historyService
			.createHistoricProcessInstanceQuery()
			.processDefinitionKey(Constants.APP_PROCESS_NAME)
			.variableValueEquals(Constants.VAR_CLIENT_ID, clientId)
			.orderByProcessInstanceStartTime().asc()
			.includeProcessVariables()
    		.list().stream()
            .map(this::hpiToSmsStatus)
    		.toList();
    }

    @Transactional
    public List<SmsStatus> getStatusListByTarget(String clientId, String targetId) {
		LOG.trace("SmsSchedulerService::getStatusList(clientId={}, targetId={})", clientId, targetId);
    	
		return historyService
        		.createHistoricProcessInstanceQuery()
                .processDefinitionKey(Constants.APP_PROCESS_NAME)
         		.variableValueEquals(Constants.VAR_CLIENT_ID, clientId)
        		.variableValueEquals(Constants.VAR_TARGET_ID, targetId)
        		.includeProcessVariables()
    			.orderByProcessInstanceStartTime().asc()
        		.list().stream()
        		.map(this::hpiToSmsStatus)
        		.toList();
    }

    @Transactional
    public List<SmsStatus> getStatusListByBusinessKey(String clientId, String businessKey) {
        LOG.trace("SmsSchedulerService::getStatusList(clientId={}, businessKey={})", clientId, businessKey);
        
        return historyService
                .createHistoricProcessInstanceQuery()
                .processDefinitionKey(Constants.APP_PROCESS_NAME)
                .processInstanceBusinessKey(businessKey)
                .variableValueEquals(Constants.VAR_CLIENT_ID, clientId)
                .includeProcessVariables()
                .orderByProcessInstanceStartTime().asc()
                .list().stream()
                .map(this::hpiToSmsStatus)
                .toList();
    }

    private SmsStatus hpiToSmsStatus(HistoricProcessInstance hpi) {
        var pvs = hpi.getProcessVariables();
        return new SmsStatus(
                hpi.getId(),
                (String) pvs.getOrDefault(Constants.VAR_CLIENT_ID, null),
                (String) pvs.getOrDefault(Constants.VAR_TARGET_ID, null),
                hpi.getBusinessKey(),
                (String) pvs.getOrDefault(Constants.VAR_SMS_STATUS, null),
                dateHelper.format(hpi.getStartTime()),
                dateHelper.format(hpi.getEndTime()),
                (int) pvs.getOrDefault(Constants.VAR_SMS_RETRIES, -1));
    }
}
