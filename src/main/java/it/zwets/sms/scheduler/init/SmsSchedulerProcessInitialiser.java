/**
 * 
 */
package it.zwets.sms.scheduler.init;

import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import it.zwets.sms.scheduler.Constants;
import it.zwets.sms.scheduler.Schedule;

/**
 * Initialises the SmsSchedulerProcess process variables.
 * 
 * @author zwets
 */
@Component
public class SmsSchedulerProcessInitialiser extends AbstractProcessInitialiser {

	private static final Logger LOG = LoggerFactory.getLogger(SmsSchedulerProcessInitialiser.class);
	private static final long serialVersionUID = 1L;

//	private Random rng = new Random();

	@Autowired
	protected Environment environment;

	@Override
	public void initialise(DelegateExecution execution) {

			// Extract the relevant variables from the execution

/*
		AuthenticationContext a;
		a.getAuthenticatedUserId();
		IdentityService svc;
		SecurityScope ss;
		FlowableHttp
*/		
		
		LOG.debug("USER ID: {}", Authentication.getAuthenticatedUserId());
		
			// Coming from the invoker, need to check
		
		String clientId = execution.getVariable(Constants.VAR_CLIENT_ID, String.class);
		String targetId = execution.getVariable(Constants.VAR_TARGET_ID, String.class);
		Schedule schedule = execution.getVariable(Constants.VAR_SMS_SCHEDULE, Schedule.class);
		String payload = execution.getVariable(Constants.VAR_SMS_PAYLOAD, String.class);

		LOG.info("Process variables before initialisation: I:C:T:B:S:P {}:{}:{}:{}:{}:P", 
				execution.getProcessInstanceId(), clientId, targetId, 
				execution.getProcessInstanceBusinessKey(), schedule.toString().replaceAll(" ", "-"), payload);

			// Add the following
		
		initVariable(execution, Constants.VAR_SMS_RETRIES, -1);
		initVariable(execution, Constants.VAR_SMS_STATUS, Constants.SMS_STATUS_NEW);
		// NO: initVariable(execution, Constants.VAR_SMS_DUETIME, Constants.SMS_STATUS_UNBORN);

//		boolean testing = environment.getProperty(PROP_TEST_REMINDERS, Boolean.class, false);
//		int hour = environment.getRequiredProperty(PROP_REM_HOUR, Integer.class);
//
//			// Set up the map of variables we will set on the execution
//
//		HashMap<String,Object> vars = new HashMap<String,Object>();
//
//		LocalDateTime now = dateHelper.toLocalDateTime(new Date());
//
//		if (LOG.isDebugEnabled() && armId == 1) {
//			LOG.debug("Setting all reminders to skip, arm is 1");
//		}
//
//		for (int rem = 1; rem <= 3; rem++) {
//
//			String propTest = TEST_PROP_REM_PFX_SEC + rem;
//			String propBase = PROP_REM_PFX_DAY + rem;
//			String varPfx = VARPFX_REM + rem;
//
//			// Check for optional reminder.day.*.disabled property
//			boolean isDisabled = environment.getProperty(
//					propBase + PROP_REM_SFX_DISABLED, Boolean.class, false);
//
//			// Initialise the timer due variable as not due (null)
//			vars.put(varPfx + VARSFX_REM_DUE, null);
//
//			// Retrieve the reminder.day.* for this reminder, required property unless disabled
//			int daysOffset = isDisabled ? 0 : environment.getRequiredProperty(propBase, Integer.class);
//
//			if (armId == 1 || (VAX_ID_0.equals(vaxId) && 
//					((preBirth && daysOffset >= 0) || (!preBirth && daysOffset < 0)))) {
//				// No reminders ever for arm 1; in vax0 neg offsets pre EDD, 0 or higher post birth
//				vars.put(varPfx + VARSFX_REM_STATUS, "NOT APPLICABLE");
//			}
//			else if (isDisabled) {
//				// Disabled was introduced all out for reminder 3
//				vars.put(varPfx + VARSFX_REM_STATUS, "DISABLED");
//			}
//			else {
//					// Compute the real reminder times, then shift if testing
//
//				LocalDateTime timerDue = dateHelper.toLocalDateTime(dueDate)
//						.plusDays(daysOffset)
//						.withHourOfDay(hour)
//						.withMinuteOfHour(rng.nextInt(30))
//						.withSecondOfMinute(rng.nextInt(60));
//
//				if (timerDue.isAfter(now)) {
//
//					LOG.debug("Reminder {} due at {}", rem, timerDue);
//					vars.put(varPfx + VARSFX_REM_DUE, timerDue.toString());
//					vars.put(varPfx + VARSFX_REM_STATUS, "SCHEDULED");
//
//					if (testing) {
//						int secOfs = environment.getProperty(propTest, Integer.class, rem * 30);
//						timerDue = now.plusSeconds(secOfs);
//						LOG.debug("[TEST] shifting reminder {} to {} secs from now: {}", rem, secOfs, timerDue);
//						vars.put(varPfx + VARSFX_REM_DUE, timerDue.toString());
//					}
//				}
//				else {
//					LOG.debug("Reminder {} due in the past at {}, skipped", rem, timerDue);
//					vars.put(varPfx + VARSFX_REM_STATUS, "EXPIRED");
//				}
//			}
//		}
//
//		execution.setVariables(vars);
	}
}
