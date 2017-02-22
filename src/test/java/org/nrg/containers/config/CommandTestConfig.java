package org.nrg.containers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.daos.CommandDao;
import org.nrg.containers.model.CommandEntity;
import org.nrg.containers.model.DockerCommandEntity;
import org.nrg.containers.model.CommandWrapperEntity;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerExecutionService;
import org.nrg.containers.services.impl.HibernateCommandService;
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
import org.springframework.transaction.support.ResourceTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
//@EnableTransactionManagement
@Import(ExecutionHibernateEntityTestConfig.class)
public class CommandTestConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
//    @Bean
//    public DockerImageService dockerImageService() {
//        return new HibernateDockerImageService();
//    }
//
//    @Bean
//    public DockerImageDao dockerImageDao() {
//        return new DockerImageDao();
//    }

//    @Bean
//    public ScriptService scriptService() {
//        return new HibernateScriptService();
//    }
//
//    @Bean
//    public ScriptRepository scriptRepository() {
//        return new ScriptRepository();
//    }

//    @Bean
//    public ScriptEnvironmentService scriptEnvironmentService() {
//        return new HibernateScriptEnvironmentService();
//    }
//
//    @Bean
//    public ScriptEnvironmentDao scriptEnvironmentDao() {
//        return new ScriptEnvironmentDao();
//    }

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
    public ContainerControlApi controlApi() {
        return Mockito.mock(ContainerControlApi.class);
    }

    @Bean
    public TransportService transportService() {
        return new TransportServiceImpl();
    }

    @Bean
    public ConfigService configService() {
        return Mockito.mock(ConfigService.class);
    }

    @Bean
    public ContainerExecutionService mockContainerExecutionService() {
        return Mockito.mock(ContainerExecutionService.class);
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory(final DataSource dataSource, @Qualifier("hibernateProperties") final Properties properties) {
        final LocalSessionFactoryBean bean = new LocalSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setHibernateProperties(properties);
        bean.setAnnotatedClasses(
                CommandEntity.class,
                DockerCommandEntity.class,
                CommandWrapperEntity.class);
        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }
}
