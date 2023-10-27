package it.zwets.sms.scheduler.init;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import it.zwets.sms.scheduler.Constants;

/**
 * Special kind of Spring Bean, gets executed when a Spring Boot application boots.
 */
@Component
public class BootOutput implements CommandLineRunner {
    
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;

    public BootOutput(
            final RepositoryService repositoryService,
            final RuntimeService runtimeService) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
    }

    @Override
    public void run(String... strings) throws Exception {
        System.out.println("Number of process definitions: "
           + repositoryService.createProcessDefinitionQuery().count());
        System.out.println("Number of SMS scheduled processes: "
                + runtimeService.createProcessInstanceQuery().processDefinitionKey(Constants.APP_PROCESS_NAME).count());
        
        // TODO: add historical processes
        // TODO: send SMS at start of server
    }
}
