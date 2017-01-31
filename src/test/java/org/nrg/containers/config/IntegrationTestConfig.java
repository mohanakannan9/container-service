package org.nrg.containers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.api.DockerControlApi;
import org.nrg.containers.daos.CommandDao;
import org.nrg.containers.daos.ContainerExecutionRepository;
import org.nrg.containers.daos.XnatCommandWrapperRepository;
import org.nrg.containers.events.DockerContainerEventListener;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.ContainerExecution;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerExecutionService;
import org.nrg.containers.services.XnatCommandWrapperService;
import org.nrg.containers.services.impl.HibernateCommandService;
import org.nrg.containers.services.impl.HibernateContainerExecutionService;
import org.nrg.containers.services.impl.HibernateXnatCommandWrapperService;
import org.nrg.framework.services.ContextService;
import org.nrg.framework.services.NrgEventService;
import org.nrg.prefs.services.NrgPreferenceService;
import org.nrg.transporter.TransportService;
import org.nrg.transporter.TransportServiceImpl;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.PermissionsServiceI;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xnat.services.archive.CatalogService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
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
@Import({ExecutionHibernateEntityTestConfig.class, RestApiTestConfig.class})
public class IntegrationTestConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

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
    public CommandService commandService(final ContainerControlApi controlApi,
                                         final AliasTokenService aliasTokenService,
                                         final SiteConfigPreferences siteConfigPreferences,
                                         final TransportService transporter,
                                         final ContainerExecutionService containerExecutionService,
                                         final ConfigService configService,
                                         final XnatCommandWrapperService xnatCommandWrapperService) {
        return new HibernateCommandService(controlApi, aliasTokenService, siteConfigPreferences,
                transporter, containerExecutionService, configService, xnatCommandWrapperService);
    }

    @Bean
    public CommandDao commandDao() {
        return new CommandDao();
    }

    @Bean
    public XnatCommandWrapperService xnatCommandWrapperService() {
        return new HibernateXnatCommandWrapperService();
    }

    @Bean
    public XnatCommandWrapperRepository xnatCommandWrapperRepository() {
        return new XnatCommandWrapperRepository();
    }

    @Bean
    public TransportService transportService() {
        return new TransportServiceImpl();
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
    public ConfigService configService() {
        return Mockito.mock(ConfigService.class);
    }

    @Bean
    public ContextService contextService(final ApplicationContext applicationContext) {
        final ContextService contextService = new ContextService();
        contextService.setApplicationContext(applicationContext);
        return contextService;
    }

    @Bean
    public DockerContainerEventListener containerEventListener(final EventBus eventBus) {
        return new DockerContainerEventListener(eventBus);
    }

    @Bean
    public PermissionsServiceI permissionsService() {
        return Mockito.mock(PermissionsServiceI.class);
    }

    @Bean
    public CatalogService catalogService() {
        return Mockito.mock(CatalogService.class);
    }

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
}
