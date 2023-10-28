package it.zwets.sms.scheduler.diag;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The singleton {@link TaskListener} that we attach at specific tasks
 * in the process definitions, so as to have insight in significant events.
 * 
 * Logs at <code>INFO</code> level.  Can be switched on and off using the
 * <code>sms.scheduler.diag.tasks.*</code> properties.
 * 
 * When its property <code>detailed</code> is set, it logs the assignment
 * and all variables that are set on the current task.
 * 
 * @author zwets
 */
public class TaskLogger implements TaskListener {

	private static final long serialVersionUID = 1L;
	private final Logger LOG = LoggerFactory.getLogger(TaskLogger.class);

	private final VariableLogger variableLogger;
	private final boolean enabled;
	private final boolean detailed;

	public TaskLogger(VariableLogger variableLogger, boolean enabled, boolean detailed) {
	    this.variableLogger = variableLogger;
	    this.enabled = enabled;
	    this.detailed = detailed;
	}
	/**
	 * When not enabled, no output whatsoever will be logged.
	 * @return
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * When detailed, output also the variables set at the execution pointer.
	 * @return whether to log detailed output
	 */
	public boolean isDetailed() {
		return detailed;
	}

	@Override
	public void notify(DelegateTask task) {

		if (enabled && LOG.isInfoEnabled()) {

			LOG.info("Task at D:P:X:T:K:N/E {}:{}:{}:{}:{}:{}/{}", 
				StringUtils.substringBefore(task.getProcessDefinitionId(),":"),
				task.getProcessInstanceId(),
				task.getExecutionId(),
				task.getId(),
				task.getTaskDefinitionKey(),
				task.getName(),
				task.getEventName());

			if (detailed) {

				// Dump the task assignment
				LOG.info("Task assignment: {}/{}",
					task.getAssignee(),
					StringUtils.join(task.getCandidates(), ","));

				// And all task variables
				variableLogger.dumpDelegateTaskVariables(task);
			}
		}
	}
}
