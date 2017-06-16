package org.nrg.containers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.api.DockerControlApi;
import org.nrg.containers.daos.ContainerEntityRepository;
import org.nrg.containers.events.listeners.DockerContainerEventListener;
import org.nrg.containers.model.command.entity.CommandEntity;
import org.nrg.containers.model.command.entity.CommandInputEntity;
import org.nrg.containers.model.command.entity.CommandMountEntity;
import org.nrg.containers.model.command.entity.CommandOutputEntity;
import org.nrg.containers.model.command.entity.CommandWrapperDerivedInputEntity;
import org.nrg.containers.model.command.entity.CommandWrapperEntity;
import org.nrg.containers.model.command.entity.CommandWrapperExternalInputEntity;
import org.nrg.containers.model.command.entity.CommandWrapperOutputEntity;
import org.nrg.containers.model.command.entity.DockerCommandEntity;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.model.container.entity.ContainerEntityHistory;
import org.nrg.containers.model.container.entity.ContainerEntityInput;
import org.nrg.containers.model.container.entity.ContainerEntityMount;
import org.nrg.containers.model.container.entity.ContainerEntityOutput;
import org.nrg.containers.model.container.entity.ContainerMountFilesEntity;
import org.nrg.containers.model.server.docker.DockerServerPrefsBean;
import org.nrg.containers.services.CommandLabelService;
import org.nrg.containers.services.CommandResolutionService;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerEntityService;
import org.nrg.containers.services.ContainerService;
import org.nrg.containers.services.impl.CommandLabelServiceImpl;
import org.nrg.containers.services.impl.CommandResolutionServiceImpl;
import org.nrg.containers.services.impl.ContainerServiceImpl;
import org.nrg.containers.services.impl.HibernateContainerEntityService;
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
    /*
    Control API and dependencies + Events
     */
    @Bean
    public DockerControlApi dockerControlApi(final DockerServerPrefsBean containerServerPref,
                                             final CommandLabelService commandLabelService,
                                             final NrgEventService eventService) {
        return new DockerControlApi(containerServerPref, commandLabelService, eventService);
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

    @Bean
    public CommandLabelService commandLabelService(final ObjectMapper objectMapper) {
        return new CommandLabelServiceImpl(objectMapper);
    }

    /*
    Container launch Service and dependencies
     */
    @Bean
    public ContainerService containerService(final CommandService commandService,
                                             final ContainerControlApi containerControlApi,
                                             final ContainerEntityService containerEntityService,
                                             final CommandResolutionService commandResolutionService,
                                             final AliasTokenService aliasTokenService,
                                             final SiteConfigPreferences siteConfigPreferences,
                                             final TransportService transportService,
                                             final PermissionsServiceI permissionsService,
                                             final CatalogService catalogService,
                                             final ObjectMapper mapper) {
        final ContainerService containerService =
                new ContainerServiceImpl(commandService, containerControlApi, containerEntityService,
                        commandResolutionService, aliasTokenService, siteConfigPreferences,
                        transportService, catalogService, mapper);
        ((ContainerServiceImpl)containerService).setPermissionsService(permissionsService);
        return containerService;
    }

    @Bean
    public CommandResolutionService commandResolutionService(final CommandService commandService,
                                                             final ConfigService configService,
                                                             final SiteConfigPreferences siteConfigPreferences,
                                                             final ObjectMapper objectMapper) {
        return new CommandResolutionServiceImpl(commandService, configService, siteConfigPreferences, objectMapper);
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

    @Bean
    public PermissionsServiceI permissionsService() {
        return Mockito.mock(PermissionsServiceI.class);
    }

    @Bean
    public CatalogService catalogService() {
        return Mockito.mock(CatalogService.class);
    }

    /*
    Container entity service and dependencies
     */
    @Bean
    public ContainerEntityService containerEntityService() {
        return new HibernateContainerEntityService();
    }

    @Bean
    public ContainerEntityRepository containerEntityRepository() {
        return new ContainerEntityRepository();
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
                CommandOutputEntity.class,
                CommandMountEntity.class,
                CommandWrapperEntity.class,
                CommandWrapperExternalInputEntity.class,
                CommandWrapperDerivedInputEntity.class,
                CommandWrapperOutputEntity.class,
                ContainerEntity.class,
                ContainerEntityHistory.class,
                ContainerEntityInput.class,
                ContainerEntityOutput.class,
                ContainerEntityMount.class,
                ContainerMountFilesEntity.class);

        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }
}
