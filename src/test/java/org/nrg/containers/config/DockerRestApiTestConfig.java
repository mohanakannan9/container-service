package org.nrg.containers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.api.DockerControlApi;
import org.nrg.containers.daos.CommandDao;
import org.nrg.containers.daos.ContainerExecutionRepository;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.DockerCommand;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.containers.model.XnatCommandWrapper;
import org.nrg.containers.rest.DockerRestApi;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerExecutionService;
import org.nrg.containers.services.DockerHubService;
import org.nrg.containers.services.DockerService;
import org.nrg.containers.services.impl.DockerServiceImpl;
import org.nrg.containers.services.impl.HibernateCommandService;
import org.nrg.containers.services.impl.HibernateContainerExecutionService;
import org.nrg.framework.services.ContextService;
import org.nrg.framework.services.NrgEventService;
import org.nrg.prefs.services.NrgPreferenceService;
import org.nrg.transporter.TransportService;
import org.nrg.transporter.TransportServiceImpl;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.services.AliasTokenService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableWebMvc
@EnableWebSecurity
@Import({ExecutionHibernateEntityTestConfig.class, RestApiTestConfig.class})
public class DockerRestApiTestConfig extends WebSecurityConfigurerAdapter {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public DockerRestApi dockerRestApi(final DockerService dockerService,
                                       final ObjectMapper objectMapper,
                                       final UserManagementServiceI userManagementService,
                                       final RoleHolder roleHolder) {
        return new DockerRestApi(dockerService, objectMapper, userManagementService, roleHolder);
    }

    @Bean
    public DockerService dockerService(final ContainerControlApi controlApi,
                                       final DockerHubService dockerHubService,
                                       final CommandService commandService) {
        return new DockerServiceImpl(controlApi, dockerHubService, commandService);
    }

    @Bean
    public DockerHubService mockDockerHubService() {
        return Mockito.mock(DockerHubService.class);
    }

    @Bean
    public DockerServerPrefsBean mockDockerServerPrefsBean() {
        return Mockito.mock(DockerServerPrefsBean.class);
    }

    @Bean
    public ContainerControlApi mockContainerControlApi(final DockerServerPrefsBean containerServerPref,
                                                       final ObjectMapper objectMapper,
                                                       final NrgEventService eventService) {
        final ContainerControlApi controlApi = new DockerControlApi(containerServerPref, objectMapper, eventService);
        return Mockito.spy(controlApi);
    }

    @Bean
    public ContainerExecutionService mockContainerExecutionService() {
        return Mockito.mock(HibernateContainerExecutionService.class);
    }

    @Bean
    public ContainerExecutionRepository mockContainerExecutionRepository() {
        return Mockito.mock(ContainerExecutionRepository.class);
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
    public NrgPreferenceService nrgPreferenceService() {
        return Mockito.mock(NrgPreferenceService.class);
    }

    @Bean
    public CommandService commandService(final ContainerControlApi controlApi,
                                         final AliasTokenService aliasTokenService,
                                         final SiteConfigPreferences siteConfigPreferences,
                                         final TransportService transporter,
                                         final ContainerExecutionService containerExecutionService,
                                         final ConfigService configService) {
        return new HibernateCommandService(controlApi, aliasTokenService, siteConfigPreferences,
                transporter, containerExecutionService, configService);
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
    public ConfigService configService() {
        return Mockito.mock(ConfigService.class);
    }

    @Bean
    public NrgEventService mockNrgEventService() {
        return Mockito.mock(NrgEventService.class);
    }

    @Bean
    public ContextService contextService(final ApplicationContext applicationContext) {
        final ContextService contextService = new ContextService();
        contextService.setApplicationContext(applicationContext);
        return contextService;
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory(final DataSource dataSource, @Qualifier("hibernateProperties") final Properties properties) {
        final LocalSessionFactoryBean bean = new LocalSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setHibernateProperties(properties);
        bean.setAnnotatedClasses(
                Command.class,
                DockerCommand.class,
                XnatCommandWrapper.class);
        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(new TestingAuthenticationProvider());
    }
}
