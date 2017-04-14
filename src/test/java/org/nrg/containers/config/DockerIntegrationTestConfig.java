package org.nrg.containers.config;

import org.nrg.containers.api.DockerControlApi;
import org.nrg.containers.events.DockerEventPuller;
import org.nrg.containers.model.server.docker.DockerServerPrefsBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
@EnableTransactionManagement
@Import({IntegrationTestConfig.class})
public class DockerIntegrationTestConfig implements SchedulingConfigurer {
    @Bean
    public DockerEventPuller dockerEventPuller(final DockerControlApi dockerControlApi, final DockerServerPrefsBean dockerServerPrefsBean) {
        return new DockerEventPuller(dockerControlApi, dockerServerPrefsBean);
    }

    @Bean
    public TriggerTask dockerEventPullerTask(final DockerEventPuller dockerEventPuller) {
        myTask = new TriggerTask(
                dockerEventPuller,
                new PeriodicTrigger(5L, TimeUnit.SECONDS)
        );
        return myTask;
    }

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler taskScheduler() {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }

    @Override
    public void configureTasks(final ScheduledTaskRegistrar scheduledTaskRegistrar) {
        scheduledTaskRegistrar.setScheduler(taskScheduler());

        scheduledTaskRegistrar.addTriggerTask(myTask);
    }

    private TriggerTask myTask;
}