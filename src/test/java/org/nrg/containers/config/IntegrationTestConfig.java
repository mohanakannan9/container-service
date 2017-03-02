package org.nrg.containers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.api.DockerControlApi;
import org.nrg.containers.daos.ContainerExecutionRepository;
import org.nrg.containers.events.DockerContainerEventListener;
import org.nrg.containers.model.CommandEntity;
import org.nrg.containers.model.CommandInputEntity;
import org.nrg.containers.model.CommandMountEntity;
import org.nrg.containers.model.CommandWrapperInputEntity;
import org.nrg.containers.model.CommandWrapperOutputEntity;
import org.nrg.containers.model.ContainerExecution;
import org.nrg.containers.model.ContainerExecutionMount;
import org.nrg.containers.model.DockerCommandEntity;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.containers.model.CommandWrapperEntity;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerExecutionService;
import org.nrg.containers.services.ContainerLaunchService;
import org.nrg.containers.services.impl.ContainerLaunchServiceImpl;
import org.nrg.containers.services.impl.HibernateContainerExecutionService;
import org.nrg.framework.services.NrgEventService;
import org.nrg.transporter.TransportService;
import org.nrg.transporter.TransportServiceImpl;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.PermissionsServiceI;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xnat.services.archive.CatalogService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.ResourceTransactionManager;
import reactor.Environment;
import reactor.bus.EventBus;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@Import({CommandConfig.class, HibernateConfig.class, RestApiTestConfig.class})
public class IntegrationTestConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /*
    Control API and dependencies + Events
     */
    @Bean
    public DockerControlApi dockerControlApi(final DockerServerPrefsBean containerServerPref,
                                             final ObjectMapper objectMapper,
                                             final NrgEventService eventService) {
        return new DockerControlApi(containerServerPref, objectMapper, eventService);
    }

    @Bean
    public DockerServerPrefsBean mockDockerServerPrefsBean() {
        return Mockito.mock(DockerServerPrefsBean.class);
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
    public DockerContainerEventListener containerEventListener(final EventBus eventBus) {
        return new DockerContainerEventListener(eventBus);
    }

    /*
    Container launch Service and dependencies
     */
    @Bean
    public ContainerLaunchService containerLaunchService(final CommandService commandService,
                                                         final ContainerControlApi controlApi,
                                                         final AliasTokenService aliasTokenService,
                                                         final SiteConfigPreferences siteConfigPreferences,
                                                         final TransportService transporter,
                                                         final ContainerExecutionService containerExecutionService,
                                                         final ConfigService configService) {
        return new ContainerLaunchServiceImpl(commandService, controlApi, aliasTokenService,
                siteConfigPreferences, transporter, containerExecutionService, configService);
    }

    @Bean
    public TransportService transportService() {
        return new TransportServiceImpl();
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
    public ConfigService configService() {
        return Mockito.mock(ConfigService.class);
    }

    /*
    Container execution service and dependencies
     */
    @Bean
    public ContainerExecutionService containerExecutionService(final ContainerControlApi containerControlApi,
                                                               final SiteConfigPreferences siteConfigPreferences,
                                                               final TransportService transportService,
                                                               final PermissionsServiceI permissionsService,
                                                               final CatalogService catalogService,
                                                               final ObjectMapper mapper) {
        return new HibernateContainerExecutionService(containerControlApi, siteConfigPreferences, transportService, permissionsService, catalogService, mapper);
    }

    @Bean
    public ContainerExecutionRepository containerExecutionRepository() {
        return new ContainerExecutionRepository();
    }

    @Bean
    public PermissionsServiceI permissionsService() {
        return Mockito.mock(PermissionsServiceI.class);
    }

    @Bean
    public CatalogService catalogService() {
        return Mockito.mock(CatalogService.class);
    }

    /*
    Session factory
     */
    @Bean
    public LocalSessionFactoryBean sessionFactory(final DataSource dataSource, @Qualifier("hibernateProperties") final Properties properties) {
        final LocalSessionFactoryBean bean = new LocalSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setHibernateProperties(properties);
        bean.setAnnotatedClasses(
                CommandEntity.class,
                DockerCommandEntity.class,
                CommandInputEntity.class,
                CommandMountEntity.class,
                CommandWrapperEntity.class,
                CommandWrapperInputEntity.class,
                CommandWrapperOutputEntity.class,
                ContainerExecution.class,
                ContainerExecutionMount.class);

        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }
}
