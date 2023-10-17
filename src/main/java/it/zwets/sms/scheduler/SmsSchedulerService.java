package it.zwets.sms.scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmsSchedulerService {

	@Autowired
	private RuntimeService runtimeService;

	@Transactional
    public void scheduleSms(
    		String clientId, String targetId, String uniqueId, 
    		Schedule schedule, String payload) {
		
		Map<String,Object> vars = new HashMap<String,Object>();
		
		vars.put("clientId", clientId);
		vars.put("targetId", targetId);
		vars.put("uniqueId", uniqueId);
		vars.put("smsSchedule", schedule);
		vars.put("smsPayload", payload);
        
		runtimeService.startProcessInstanceByKey(Constants.APP_PROCESS_NAME, vars);
    }

	@Transactional
	public void cancelSms(String clientId, String uniqueId) {
	}

	@Transactional
	public void cancelTarget(String clientId, String targetId) {
		
	}
	
	@Transactional
	public void cancelAll() {
		
	}
	
    @Transactional
    public List<ProcessInstance> getProcessInstances(String clientId) {
        return runtimeService.createProcessInstanceQuery().variableValueEquals(Constants.VAR_CLIENT_ID, clientId).list();
    }

    @Transactional
    public List<ProcessInstance> getProcessInstances(String clientId, String targetId) {
        return runtimeService.createProcessInstanceQuery().variableValueEquals(Constants.VAR_CLIENT_ID, clientId).variableValueEquals(Constants.VAR_TARGET_ID, targetId).list();
    }
}
