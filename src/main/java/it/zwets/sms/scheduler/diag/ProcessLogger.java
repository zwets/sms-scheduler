package it.zwets.sms.scheduler.diag;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The singleton {@link ExecutionListener} that we attach at specific points
 * in the process definitions, so as to have insight in significant events.
 * 
 * Logs at <code>INFO</code> level.  Created and configured in the Configuration.
 * 
 * When its property <code>detailed</code> is set, it also logs all variables
 * that are set on the current execution using {@link VariableLogger}.
 * 
 * @author zwets
 */
public class ProcessLogger implements ExecutionListener {

    private final Logger LOG = LoggerFactory.getLogger(ProcessLogger.class);
	
    private static final long serialVersionUID = 1L;

	private final VariableLogger variableLogger;
	private final boolean enabled;
	private final boolean detailed;

	public ProcessLogger(VariableLogger variableLogger, boolean enabled, boolean detailed) {
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
	public void notify(DelegateExecution execution) {
		
		if (enabled && LOG.isInfoEnabled()) {
			
			LOG.info("Execution is at D:P(B):X/M:A:E {}:{}({}):{}/{}:{}:{}",
				StringUtils.substringBefore(execution.getProcessDefinitionId(),":"),
				execution.getProcessInstanceId(), 
				execution.getProcessInstanceBusinessKey(),
				execution.getId(),
				StringUtils.substringAfterLast(StringUtils.substringBefore(String.valueOf(execution.getCurrentFlowElement()), "@"),"."),
				execution.getCurrentActivityId(), 
				execution.getEventName());

			if (detailed) {
				variableLogger.dumpDelegateExecutionVariables(execution);
			}
		}
	}
}
