package it.zwets.sms.scheduler;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.zwets.sms.scheduler.SmsSchedulerConfiguration.Constants;
import it.zwets.sms.scheduler.util.DateHelper;

@Service
public class SmsSchedulerService {

	private static final Logger LOG = LoggerFactory.getLogger(SmsSchedulerService.class);
	
	private final RuntimeService runtimeService;
	private final HistoryService historyService;
	private final DateHelper dateHelper;
	
	/** The DTO for reporting status */
    public final record SmsStatus(
            String id, String client, String batch, String key, String target, String status, String due, String deadline, String started, String ended, int retries, String user) { }

    public SmsSchedulerService(ProcessEngine processEngine, DateHelper dateHelper) {
        this.runtimeService = processEngine.getRuntimeService();
        this.historyService = processEngine.getHistoryService();
        this.dateHelper = dateHelper;
    }
    
    // Scheduling ---------------------------------------------------------------------------------

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
    		String clientId, String batchId, String clientKey, String targetId, String schedule, String payload) {

		LOG.debug("SmsSchedulerService::scheduleSms({},{},{},{},{},{})", clientId, batchId, clientKey, targetId, schedule, payload);
		
		Map<String,Object> vars = new HashMap<String,Object>();
		
        String userId = getAuthenticatedUser();
        vars.put(Constants.VAR_USER_ID, userId);
		
		vars.put(Constants.VAR_CLIENT_ID, clientId);
        vars.put(Constants.VAR_BATCH_ID, batchId);
        vars.put(Constants.VAR_CLIENT_KEY, clientKey);
		vars.put(Constants.VAR_TARGET_ID, targetId);
		vars.put(Constants.VAR_SCHEDULE, schedule);
		vars.put(Constants.VAR_PAYLOAD, payload);
        
		ProcessInstance pi = runtimeService.startProcessInstanceByKey(Constants.SMS_SCHEDULER_PROCESS_NAME, vars);

		// We are in a transaction so process variables not yet committed but we should be able to read,
		// or else we put in the defaults from incoming and status "NEW".g
        vars = pi.getProcessVariables();
        return new SmsStatus(
                pi.getId(),
                (String) vars.getOrDefault(Constants.VAR_CLIENT_ID, clientId),
                (String) vars.getOrDefault(Constants.VAR_BATCH_ID, batchId),
                (String) vars.getOrDefault(Constants.VAR_CLIENT_KEY, clientKey),
                (String) vars.getOrDefault(Constants.VAR_TARGET_ID, targetId),
                (String) vars.getOrDefault(Constants.VAR_SMS_STATUS, Constants.SMS_STATUS_NEW),
                dateHelper.format((Instant) vars.getOrDefault(Constants.VAR_SMS_DUETIME, null)),
                (String) vars.getOrDefault(Constants.VAR_SMS_DEADLINE, null),
                dateHelper.format(pi.getStartTime()),
                null, // pi has no end time
                (int) vars.getOrDefault(Constants.VAR_SMS_RETRIES, -1),
                (String) vars.getOrDefault(Constants.VAR_USER_ID, null));
    }
	
	// Query --------------------------------------------------------------------------------------

    @Transactional
    public SmsStatus getSmsStatus(String id) {
        LOG.trace("SmsSchedulerService::getSmsStatus(id={})", id);
        
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(id)
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
            .processDefinitionKey(Constants.SMS_SCHEDULER_PROCESS_NAME)
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
			.processDefinitionKey(Constants.SMS_SCHEDULER_PROCESS_NAME)
			.variableValueEquals(Constants.VAR_CLIENT_ID, clientId)
			.orderByProcessInstanceStartTime().asc()
			.includeProcessVariables()
    		.list().stream()
            .map(this::hpiToSmsStatus)
    		.toList();
    }

    @Transactional
    public List<SmsStatus> getStatusListByBatch(String clientId, String batchId) {
        LOG.trace("SmsSchedulerService::getStatusList(clientId={}, batchId={})", clientId, batchId);
        
        return historyService
                .createHistoricProcessInstanceQuery()
                .processDefinitionKey(Constants.SMS_SCHEDULER_PROCESS_NAME)
                .variableValueEquals(Constants.VAR_CLIENT_ID, clientId)
                .variableValueEquals(Constants.VAR_BATCH_ID, batchId)
                .includeProcessVariables()
                .orderByProcessInstanceStartTime().asc()
                .list().stream()
                .map(this::hpiToSmsStatus)
                .toList();
    }

    @Transactional
    public List<SmsStatus> getStatusListByClientKey(String clientId, String clientKey) {
        LOG.trace("SmsSchedulerService::getStatusList(clientId={}, clientKey={})", clientId, clientKey);
        
        return historyService
                .createHistoricProcessInstanceQuery()
                .processDefinitionKey(Constants.SMS_SCHEDULER_PROCESS_NAME)
                .variableValueEquals(Constants.VAR_CLIENT_ID, clientId)
                .variableValueEquals(Constants.VAR_CLIENT_KEY, clientKey)
                .includeProcessVariables()
                .orderByProcessInstanceStartTime().asc()
                .list().stream()
                .map(this::hpiToSmsStatus)
                .toList();
    }

    @Transactional
    public List<SmsStatus> getStatusListByTarget(String clientId, String targetId) {
		LOG.trace("SmsSchedulerService::getStatusList(clientId={}, targetId={})", clientId, targetId);
    	
		return historyService
        		.createHistoricProcessInstanceQuery()
                .processDefinitionKey(Constants.SMS_SCHEDULER_PROCESS_NAME)
         		.variableValueEquals(Constants.VAR_CLIENT_ID, clientId)
        		.variableValueEquals(Constants.VAR_TARGET_ID, targetId)
        		.includeProcessVariables()
    			.orderByProcessInstanceStartTime().asc()
        		.list().stream()
        		.map(this::hpiToSmsStatus)
        		.toList();
    }
    
    // Cancel ----------------------------------------------------------------------------------------

    @Transactional
    public void cancelSms(String instanceId) {
        LOG.trace("cancelSms({})", instanceId);

        Execution ex = runtimeService.createExecutionQuery()
                .processInstanceId(instanceId)
                .activityId(Constants.ACTIVITY_RECV_CANCEL)
                .singleResult();
        
        if (ex != null) {
            LOG.debug("Canceling SMS {}", instanceId);
            runtimeService.trigger(ex.getId());
        }
    }

    @Transactional
    public void cancelBatch(String clientId, String batchId) {
        LOG.trace("cancelBatch({},{})", clientId, batchId);
        
        for (Execution ex : runtimeService.createExecutionQuery()
                .processDefinitionKey(Constants.SMS_SCHEDULER_PROCESS_NAME)
                .activityId(Constants.ACTIVITY_RECV_CANCEL)
                .processVariableValueEquals(Constants.VAR_CLIENT_ID, clientId)
                .processVariableValueEquals(Constants.VAR_BATCH_ID, batchId)
                .list())
        {
            LOG.debug("Canceling SMS by batch {}:{}: {}", clientId, batchId, ex.getProcessInstanceId());
            runtimeService.trigger(ex.getId());
        }
    }
    
    @Transactional
    public void cancelByClientKey(String clientId, String clientKey) {
        LOG.trace("cancelByClientKey({},{})", clientId, clientKey);
        
        for (Execution ex : runtimeService.createExecutionQuery()
                .processDefinitionKey(Constants.SMS_SCHEDULER_PROCESS_NAME)
                .activityId(Constants.ACTIVITY_RECV_CANCEL)
                .processVariableValueEquals(Constants.VAR_CLIENT_ID, clientId)
                .processVariableValueEquals(Constants.VAR_CLIENT_KEY, clientKey)
                .list())
        {
            LOG.debug("Canceling SMS by key {}:{}: {}", clientId, clientKey, ex.getProcessInstanceId());
            runtimeService.trigger(ex.getId());
        }
    }
    
    @Transactional
    public void cancelAllForTarget(String clientId, String targetId) {
        LOG.debug("cancelAllForTarget({},{})", clientId, targetId);
        
        for (Execution ex : runtimeService.createExecutionQuery()
                .processDefinitionKey(Constants.SMS_SCHEDULER_PROCESS_NAME)
                .activityId(Constants.ACTIVITY_RECV_CANCEL)
                .processVariableValueEquals(Constants.VAR_CLIENT_ID, clientId)
                .processVariableValueEquals(Constants.VAR_TARGET_ID, targetId)
                .list())
        {
            LOG.debug("Canceling SMS for target {}:{}: {}", clientId, targetId, ex.getProcessInstanceId());
            runtimeService.trigger(ex.getId());
        }
    }
    
    @Transactional
    public void cancelAllForClient(String clientId) {
        LOG.trace("cancelAllForClient({})", clientId);
        
        for (Execution ex : runtimeService.createExecutionQuery()
                .processDefinitionKey(Constants.SMS_SCHEDULER_PROCESS_NAME)
                .activityId(Constants.ACTIVITY_RECV_CANCEL)
                .processVariableValueEquals(Constants.VAR_CLIENT_ID, clientId)
                .list())
        {
            LOG.debug("Canceling SMS for client {}: {}", clientId, ex.getProcessInstanceId());
            runtimeService.trigger(ex.getId());
        }
    }
        
    // Deleting (internal only) -------------------------------------------------------------------
    
    @Transactional
    public void deleteInstance(String instanceId) {
        LOG.debug("deleteInstance({})", instanceId);
        if (runtimeService.createProcessInstanceQuery().processInstanceId(instanceId).count() != 0) {
            runtimeService.deleteProcessInstance(instanceId, null);
        }
        historyService.deleteHistoricProcessInstance(instanceId);
    }

    @Transactional
    public void deleteAllForClient(String clientId) {
        for (HistoricProcessInstance hpi : historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(Constants.SMS_SCHEDULER_PROCESS_NAME)
                .variableValueEquals(Constants.VAR_CLIENT_ID, clientId)
                .list())
        {
            deleteInstance(hpi.getId());
        }
    }

    // Helpers ------------------------------------------------------------------------------------

    private SmsStatus hpiToSmsStatus(HistoricProcessInstance hpi) {
        var pvs = hpi.getProcessVariables();
        return new SmsStatus(
                hpi.getId(),
                (String) pvs.getOrDefault(Constants.VAR_CLIENT_ID, null),
                (String) pvs.getOrDefault(Constants.VAR_BATCH_ID, null),
                (String) pvs.getOrDefault(Constants.VAR_CLIENT_KEY, null),
                (String) pvs.getOrDefault(Constants.VAR_TARGET_ID, null),
                (String) pvs.getOrDefault(Constants.VAR_SMS_STATUS, null),
                dateHelper.format((Instant) pvs.getOrDefault(Constants.VAR_SMS_DUETIME, null)),
                (String) pvs.getOrDefault(Constants.VAR_SMS_DEADLINE, null),
                dateHelper.format(hpi.getStartTime()),
                dateHelper.format(hpi.getEndTime()),
                (int) pvs.getOrDefault(Constants.VAR_SMS_RETRIES, -1),
                (String) pvs.getOrDefault(Constants.VAR_USER_ID, null));
    }
    
    private String getAuthenticatedUser() {
        SecurityContext ctx = SecurityContextHolder.getContext();
        if (ctx != null) {
            Authentication auth = ctx.getAuthentication();
            if (auth != null) {
                return auth.getName();
            }
        }
        return null;
    }
}