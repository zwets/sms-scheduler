package it.zwets.sms.scheduler;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmsSchedulerService {

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;
	
	@Transactional
    public void startProcess() {
		Map<String,Object> vars = new HashMap<String,Object>();
		vars.put("clientId", "Client ID");
		vars.put("targetId", "Target ID");
		vars.put("uniqueId", "Unique ID");
		vars.put("smsSchedule", "%d %d".formatted(new Date().getTime() / 1000, new Date().getTime() / 1000 + 10));
        runtimeService.startProcessInstanceByKey(Constants.APP_PROCESS_NAME, vars);
    }

    @Transactional
    public List<Task> getTasks(String assignee) {
        return taskService.createTaskQuery().taskAssignee(assignee).list();
    }
}
