/**
 * 
 */
package it.zwets.sms.scheduler.init;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * Concrete subclasses of this {@link ExecutionListener} are attached to
 * process definitions in order to initialises process variables that
 * have not been set by the invoking process starter.
 * 
 * @author zwets
 */
abstract public class AbstractProcessInitialiser implements ExecutionListener {

	private static final long serialVersionUID = 1L;

	private final Logger LOG = LoggerFactory.getLogger(AbstractProcessInitialiser.class);

	@Autowired
	private Environment environment;
	
	/* (non-Javadoc)
	 * @see org.flowable.engine.delegate.ExecutionListener#notify(org.flowable.engine.delegate.DelegateExecution)
	 */
	@Override
	public void notify(DelegateExecution execution) {
		
		String processKey = StringUtils.substringBefore(execution.getProcessDefinitionId(), ":");
		
		LOG.info("Initialise D:P:X {}:{}:{}", processKey, execution.getProcessInstanceId(), execution.getId());
		
		initialise(execution);
	}

	/**
	 * Invoked on subclass when it is time to do the initialisations.
	 * @param execution the execution being initialised
	 */
	abstract public void initialise(DelegateExecution execution);
	
	/**
	 * Initialise variable name with value value if it is not set.
	 * 
	 * @param execution to set the variable on
	 * @param name the variable to set
	 * @param value the value to set
	 */
	protected void initVariable(DelegateExecution execution, String name, Object value) {

		if (!execution.hasVariable(name)) {
			LOG.debug("{} := {}", name, value);
			execution.setVariable(name, value);
		}
	}

	/**
	 * Initialise variable from property <i>processKey</i><code>.var.<i>name</i>.
	 * The value of the variable is unchanged if it is already set.
	 * An exception is thrown if the variable is not set and the property is not found.
	 * 
	 * @param execution to set the variable on
	 * @param name the variable to set
	 * @param type the class of the variable
	 * @throws IllegalStateException if the property is not found
	 */
	protected <T> void initVariableFromProperty(DelegateExecution execution, String name, Class<T> type) {

		if (!execution.hasVariable(name)) {

			String processKey = StringUtils.substringBefore(execution.getProcessDefinitionId(), ":");
			String propName = processKey + ".var." + name;

			T value = environment.getRequiredProperty(propName, type);

			initVariable(execution, name, value);
		}
	}

	/**
	 * Initialise variable from optional property <i>processKey</i><code>.var.</code><i>name</i>.
	 * The value of the variable is unchanged if it is already set. If the property is not found,
	 * initialise variable with <i>defVal</i>.
	 * 
	 * @param execution to set the variable on]
	 * @param name the variable to set
	 * @param type the class of the variable
	 * @param defVal the default to initialise the variable to
	 */
	protected <T> void initVariableFromOptionalProperty(DelegateExecution execution, String name, Class<T> type, T defVal) {

		if (!execution.hasVariable(name)) {

			String processKey = StringUtils.substringBefore(execution.getProcessDefinitionId(), ":");
			String propName = processKey + ".var." + name;

			T value = environment.getProperty(propName, type);

			if (value == null) {
				value = defVal;
			}
			
			initVariable(execution, name, value);
		}
	}

	/**
	 * Initialise variable from optional property <i>processKey</i><code>.var.<i>name</i>.
	 * The value of the variable is unchanged if it is already set. The variable is not
	 * created if the property is not found.
	 * 
	 * @param execution to set the variable on
	 * @param name the variable to set
	 * @param type the class of the variable
	 */
	protected <T> void initVariableFromOptionalProperty(DelegateExecution execution, String name, Class<T> type) {

		if (!execution.hasVariable(name)) {

			String processKey = StringUtils.substringBefore(execution.getProcessDefinitionId(), ":");
			String propName = processKey + ".var." + name;

			T value = environment.getProperty(propName, type);

			if (value != null) {
				initVariable(execution, name, value);
			}
		}
	}
}
