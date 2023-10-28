package it.zwets.sms.scheduler.diag;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The singleton {@link ExecutionListener} that we attach at specific points
 * in the process definitions, so as to have insight in significant events.
 * 
 * Logs at <code>INFO</code> level.  Can be switched on and off using the
 * <code>sms.scheduler.diag.processes.*</code> properties.
 * 
 * When its property <code>detailed</code> is set, it also logs all variables
 * that are set on the current execution using {@link VariableLogger}.
 * 
 * @author zwets
 */
@Component
public class ProcessLogger implements ExecutionListener {

	private static final long serialVersionUID = 1L;
	private final Logger LOG = LoggerFactory.getLogger(ProcessLogger.class);

	@Autowired
	private VariableLogger variableDumper;

	@Value("${sms-scheduler.diag.processes}")
	private boolean enabled;

	@Value("${sms-scheduler.diag.processes.detailed}")
	private boolean detailed;

	/**
	 * When not enabled, no output whatsoever will be logged.
	 * @return
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * When not enabled, no output whatsoever will be logged.
	 * @param silent
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * When detailed, output also the variables set at the execution pointer.
	 * @return whether to log detailed output
	 */
	public boolean isDetailed() {
		return detailed;
	}

	/**
	 * When detailed, output also the variables set at the execution pointer.
	 * @param detailed
	 */
	public void setDetailed(boolean detailed) {
		this.detailed = detailed;
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
				variableDumper.dumpDelegateExecutionVariables(execution);
			}
		}
	}
}
