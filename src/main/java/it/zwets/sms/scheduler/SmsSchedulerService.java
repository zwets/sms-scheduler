package it.zwets.sms.scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmsSchedulerService {

	private static final Logger LOG = LoggerFactory.getLogger(SmsSchedulerService.class);
	
	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private HistoryService historyService;

	@Transactional
    public SmsStatus scheduleSms(
    		String clientId, String targetId, String uniqueId, 
    		Schedule schedule, String payload) {

		LOG.info("SmsSchedulerService::scheduleSms({},{},{},{},{})", clientId, targetId, uniqueId, schedule, payload);
		
		Map<String,Object> vars = new HashMap<String,Object>();
		
		vars.put("clientId", clientId);
		vars.put("targetId", targetId);
		vars.put("uniqueId", uniqueId);
		vars.put("smsSchedule", schedule);
		vars.put("smsPayload", payload);
        
		runtimeService.startProcessInstanceByKey(Constants.APP_PROCESS_NAME, vars);
		
		return new SmsStatus(clientId, targetId, uniqueId, Constants.SMS_STATUS_NEW, 0);
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
    public SmsStatus getSmsStatus(String clientId, String targetId, String uniqueId) {
        LOG.info("SmsSchedulerService::getSmsStatus( clientId={}, targetId={}, uniqueId={} )");
        
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(Constants.APP_PROCESS_NAME)
                .includeProcessVariables()
                .singleResult();
        
        SmsStatus result = null;
               
        if (hpi != null) {
            Map<String, Object> pvs = hpi.getProcessVariables();
            result = new SmsStatus(
                    (String) pvs.getOrDefault(Constants.VAR_CLIENT_ID, null),
                    (String) pvs.getOrDefault(Constants.VAR_TARGET_ID, null),
                    (String) pvs.getOrDefault(Constants.VAR_UNIQUE_ID, null),
                    (String) pvs.getOrDefault(Constants.VAR_SMS_STATUS, null),
                    (int) pvs.getOrDefault(Constants.VAR_SMS_RETRIES, -1));
        }
        
        return result;
    }
    
    @Transactional
    public List<SmsStatus> getStatusList() {
        LOG.info("SmsSchedulerService::getStatusList()");

        return historyService
            .createHistoricProcessInstanceQuery()
            .processDefinitionKey(Constants.APP_PROCESS_NAME)
            .orderByProcessInstanceStartTime().asc()
            .includeProcessVariables()
            .list().stream()
            .map(hpi -> hpi.getProcessVariables())
            .map(pvs -> new SmsStatus(
                    (String) pvs.getOrDefault(Constants.VAR_CLIENT_ID, null),
                    (String) pvs.getOrDefault(Constants.VAR_TARGET_ID, null),
                    (String) pvs.getOrDefault(Constants.VAR_UNIQUE_ID, null),
                    (String) pvs.getOrDefault(Constants.VAR_SMS_STATUS, null),
                    (int) pvs.getOrDefault(Constants.VAR_SMS_RETRIES, -1)))
            .toList();
    }

    @Transactional
    public List<SmsStatus> getStatusList(String clientId) {
		LOG.info("SmsSchedulerService::getStatusList(clientId={})", clientId);

		return historyService
			.createHistoricProcessInstanceQuery()
			.processDefinitionKey(Constants.APP_PROCESS_NAME)
			.variableValueEquals(Constants.VAR_CLIENT_ID, clientId)
			.orderByProcessInstanceStartTime().asc()
			.includeProcessVariables()
    		.list().stream()
    		.map(hpi -> hpi.getProcessVariables())
    		.map(pvs -> new SmsStatus(
    				(String) pvs.getOrDefault(Constants.VAR_CLIENT_ID, null),
    				(String) pvs.getOrDefault(Constants.VAR_TARGET_ID, null),
    				(String) pvs.getOrDefault(Constants.VAR_UNIQUE_ID, null),
    				(String) pvs.getOrDefault(Constants.VAR_SMS_STATUS, null),
    				(int) pvs.getOrDefault(Constants.VAR_SMS_RETRIES, -1)))
    		.toList();
    }

    @Transactional
    public List<SmsStatus> getStatusList(String clientId, String targetId) {
		LOG.info("SmsSchedulerService::getStatusList(clientId={}, targetId={})", clientId, targetId);
    	
		return historyService
        		.createHistoricProcessInstanceQuery()
         		.variableValueEquals(Constants.VAR_CLIENT_ID, clientId)
        		.variableValueEquals(Constants.VAR_TARGET_ID, targetId)
        		.includeProcessVariables()
    			.orderByProcessInstanceStartTime().asc()
        		.list().stream()
        		.map(pi -> pi.getProcessVariables())
        		.map(pvs -> new SmsStatus(
        				(String) pvs.getOrDefault(Constants.VAR_CLIENT_ID, null),
        				(String) pvs.getOrDefault(Constants.VAR_TARGET_ID, null),
        				(String) pvs.getOrDefault(Constants.VAR_UNIQUE_ID, null),
        				(String) pvs.getOrDefault(Constants.VAR_SMS_STATUS, null),
        				(int) pvs.getOrDefault(Constants.VAR_SMS_RETRIES, -1)))
        		.toList();
    }
    
    /**
     * DTO to capture relevant information on scheduled (and TODO: historic) SMS
     * @author zwets
     */
    public static class SmsStatus {
    	private String clientId;
    	private String targetId;
    	private String uniqueId;
    	private String status;
    	private int retries;
    	
		public SmsStatus(String clientId, String targetId, String uniqueId, String status, int retries) {
			this.clientId = clientId;
			this.targetId = targetId;
			this.uniqueId = uniqueId;
			this.status = status;
			this.retries = retries;
		}
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
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public int getRetries() {
			return retries;
		}
		public void setRetries(int retries) {
			this.retries = retries;
		}
    }
}
