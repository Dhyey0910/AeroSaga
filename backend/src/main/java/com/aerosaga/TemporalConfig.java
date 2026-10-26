package com.aerosaga;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {
    public static final String QUEUE = "AEROSAGA_QUEUE";

    @Bean WorkflowServiceStubs serviceStubs() { return WorkflowServiceStubs.newLocalServiceStubs(); } // localhost:7233
    @Bean WorkflowClient workflowClient(WorkflowServiceStubs s) { return WorkflowClient.newInstance(s); }

    @Bean
    WorkerFactory workerFactory(WorkflowClient client, DroneActivitiesImpl activities) {
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(QUEUE);
        worker.registerWorkflowImplementationTypes(MissionWorkflowImpl.class, FleetWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        factory.start();
        return factory;
    }
}
