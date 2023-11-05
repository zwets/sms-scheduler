package it.zwets.sms.scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.zwets.sms.scheduler.SmsSchedulerConfiguration.Constants;

/**
 * Manages blocked targets.
 * 
 * Blocked targets are recipients that we never schedule messages for.
 */
@Service
public class TargetBlockerService {

    private static final Logger LOG = LoggerFactory.getLogger(TargetBlockerService.class);
    
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    
    public TargetBlockerService(ProcessEngine processEngine) {
        this.runtimeService = processEngine.getRuntimeService();
        this.historyService = processEngine.getHistoryService();
    }
    
    @Transactional
    public void blockTarget(String clientId, String targetId) {

        if (!isTargetBlocked(clientId, targetId)) {
            LOG.debug("Blocking target: {}:{}", clientId, targetId);
            
            Map<String,Object> vars = new HashMap<String,Object>();
            
            vars.put(Constants.VAR_CLIENT_ID, clientId);
            vars.put(Constants.VAR_TARGET_ID, targetId);
            
            runtimeService.startProcessInstanceByKey(Constants.BLOCK_TARGET_PROCESS_NAME, vars);
        }
    }
    
    @Transactional
    public void unblockTarget(String clientId, String targetId) {

        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(Constants.BLOCK_TARGET_PROCESS_NAME)
                .variableValueEquals(Constants.VAR_CLIENT_ID, clientId)
                .variableValueEquals(Constants.VAR_TARGET_ID, targetId)
                .singleResult();
        
        if (hpi != null) {
            LOG.debug("Unblocking target: {}:{}", clientId, targetId);
            historyService.deleteHistoricProcessInstance(hpi.getId());
        }
    }
    
    @Transactional
    public boolean isTargetBlocked(String clientId, String targetId) {
        LOG.trace("isTargetBlocked({},{})", clientId, targetId);
        
        return historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(Constants.BLOCK_TARGET_PROCESS_NAME)
                .variableValueEquals(Constants.VAR_CLIENT_ID, clientId)
                .variableValueEquals(Constants.VAR_TARGET_ID, targetId)
                .count() != 0;
    }

    @Transactional
    public String getBlockedTargets(String clientId) {
        LOG.trace("getBlockedTargets({})", clientId);
        
        return historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(Constants.BLOCK_TARGET_PROCESS_NAME)
                .variableValueEquals(Constants.VAR_CLIENT_ID, clientId)
                .includeProcessVariables()
                .list().stream().map(i -> (String)i.getProcessVariables().get(Constants.VAR_TARGET_ID))
                    .collect(Collectors.joining("\n")).concat("\n");
    }
}
