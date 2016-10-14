package org.nrg.containers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.api.DockerControlApi;
import org.nrg.containers.daos.CommandDao;
import org.nrg.containers.daos.ContainerExecutionRepository;
import org.nrg.containers.events.DockerContainerEventListener;
import org.nrg.containers.events.DockerEventPuller;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.ContainerExecution;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerExecutionService;
import org.nrg.containers.services.HibernateCommandService;
import org.nrg.containers.services.HibernateContainerExecutionService;
import org.nrg.framework.services.NrgEventService;
import org.nrg.prefs.services.NrgPreferenceService;
import org.nrg.transporter.TransportService;
import org.nrg.transporter.TransportServiceImpl;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.services.AliasTokenService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.ResourceTransactionManager;
import reactor.Environment;
import reactor.bus.EventBus;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
@EnableTransactionManagement
//@ComponentScan(value = "org.nrg.execution",
//        excludeFilters = {
//                @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Config.*", value = {}),
//                @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*PrefsBean.*", value = {})})
@Import({ExecutionHibernateEntityTestConfig.class})
public class DockerIntegrationTestConfig implements SchedulingConfigurer {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public DockerControlApi dockerControlApi() {
        return new DockerControlApi();
    }

    @Bean
    public DockerServerPrefsBean mockDockerServerPrefsBean() {
        return Mockito.mock(DockerServerPrefsBean.class);
    }

    @Bean
    public CommandService commandService() {
        return new HibernateCommandService();
    }

    @Bean
    public CommandDao commandDao() {
        return new CommandDao();
    }

    @Bean
    public TransportService transportService() {
        return Mockito.mock(TransportServiceImpl.class);
    }

    @Bean
    public Environment env() {
        return Environment.initializeIfEmpty().assignErrorJournal();
    }

    @Bean
    public EventBus eventBus(final Environment env) {
        return EventBus.create(env, Environment.THREAD_POOL);
    }

    @Bean
    public NrgEventService nrgEventService(final EventBus eventBus) {
        return new NrgEventService(eventBus);
    }

    @Bean
    public NrgPreferenceService mockNrgPreferenceService() {
        return Mockito.mock(NrgPreferenceService.class);
    }

    @Bean
    public AliasTokenService aliasTokenService() {
        return Mockito.mock(AliasTokenService.class);
    }

    @Bean
    public SiteConfigPreferences siteConfigPreferences() {
        return Mockito.mock(SiteConfigPreferences.class);
    }

    @Bean
    public DockerContainerEventListener containerEventListener(final EventBus eventBus) {
        return new DockerContainerEventListener(eventBus);
    }

    @Bean
    public ContainerExecutionService containerExecutionService(final ContainerControlApi containerControlApi,
                                                               final SiteConfigPreferences siteConfigPreferences,
                                                               final AliasTokenService aliasTokenService,
                                                               final TransportService transportService) {
        return new HibernateContainerExecutionService(containerControlApi, siteConfigPreferences, aliasTokenService, transportService);
    }

    @Bean
    public ContainerExecutionRepository containerExecutionRepository() {
        return new ContainerExecutionRepository();
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory(final DataSource dataSource, @Qualifier("hibernateProperties") final Properties properties) {
        final LocalSessionFactoryBean bean = new LocalSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setHibernateProperties(properties);
        bean.setAnnotatedClasses(
                Command.class,
                ContainerExecution.class);
//                Preference.class, Tool.class);

        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }

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
