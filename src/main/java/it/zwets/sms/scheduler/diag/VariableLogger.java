package it.zwets.sms.scheduler.diag;

import java.util.List;
import java.util.Map;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Diagnostic utility to log execution and task variables.
 * 
 * Logs variables and their values at <code>INFO</code> level to its
 * eponymous logger.  Used by the {@link ProcessLogger} and {@link TaskLogger}
 * when set to <code>detailed</code>.
 * 
 * @author zwets
 */
@Component
public class VariableLogger {
	
	private final Logger LOG = LoggerFactory.getLogger(VariableLogger.class);
	
	@Autowired
	protected RuntimeService runtimeService;
	
	public void setRuntimeService(RuntimeService runtimeService) {
		this.runtimeService = runtimeService;
	}
	
	private void dumpVariableInstance(VariableInstance var) {
		if (LOG.isInfoEnabled()) {
			LOG.info("{} = {} ({}/{}:{}:{})", 
				var.getName(), var.getValue(), var.getTypeName(), 
				var.getProcessInstanceId(), var.getExecutionId(), var.getTaskId());
		}
	}
	
	public void dumpExecutionVariables(String executionId) {
		if (LOG.isInfoEnabled()) {
			LOG.info("EXECUTION[{}] VARIABLES", executionId);
			Map<String, VariableInstance> vars = runtimeService.getVariableInstances(executionId);
			for (VariableInstance var : vars.values()) {
				dumpVariableInstance(var);
			}
		}
	}
	
	public void dumpDelegateExecutionVariables(DelegateExecution execution) {
		if (LOG.isInfoEnabled()) {
			LOG.info("DELEGATE-EXECUTION[{}] VARIABLES", execution.getId());
			Map<String,VariableInstance> vars = execution.getVariableInstances();
			for (VariableInstance var : vars.values()) {
				dumpVariableInstance(var);
			}
		}
	}

	public void dumpDelegateTaskVariables(DelegateTask task) {
		if (LOG.isInfoEnabled()) {
			LOG.info("DELEGATE-TASK[{}] VARIABLES", task.getId());
			Map<String,VariableInstance> vars = task.getVariableInstances();
			for (VariableInstance var : vars.values()) {
				dumpVariableInstance(var);
			}
		}
	}

	public void dumpAllExecutionVariables() {
		if (LOG.isInfoEnabled()) {
			List<Execution> executions = runtimeService.createExecutionQuery().list();
			for (Execution execution : executions) {
				dumpExecutionVariables(execution.getId());
			}
		}
	}
}
