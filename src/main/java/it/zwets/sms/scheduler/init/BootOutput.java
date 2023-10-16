package it.zwets.sms.scheduler.init;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Special kind of Spring Bean, gets executed when a Spring Boot application boots.
 */
@Component
public class BootOutput implements CommandLineRunner {
    
    private RepositoryService repositoryService;
    private RuntimeService runtimeService;
    private TaskService taskService;

    public BootOutput(final RepositoryService repositoryService,
                             final RuntimeService runtimeService,
                             final TaskService taskService) {

        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    @Override
    public void run(String... strings) throws Exception {
        System.out.println("Number of process definitions : "
           + repositoryService.createProcessDefinitionQuery().count());
        
        // TODO: send SMS at start of server
        
        // System.out.println("Number of tasks : " + taskService.createTaskQuery().count());
        // runtimeService.startProcessInstanceByKey("smsSchedulerProcess");
        // System.out.println("Number of tasks after process start: "
        //            + taskService.createTaskQuery().count());
    }
}
