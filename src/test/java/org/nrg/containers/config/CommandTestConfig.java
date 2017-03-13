package org.nrg.containers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.model.CommandEntity;
import org.nrg.containers.model.CommandInputEntity;
import org.nrg.containers.model.CommandMountEntity;
import org.nrg.containers.model.CommandOutputEntity;
import org.nrg.containers.model.CommandWrapperDerivedInputEntity;
import org.nrg.containers.model.CommandWrapperEntity;
import org.nrg.containers.model.CommandWrapperExternalInputEntity;
import org.nrg.containers.model.CommandWrapperOutputEntity;
import org.nrg.containers.model.DockerCommandEntity;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerEntityService;
import org.nrg.containers.services.ContainerService;
import org.nrg.containers.services.impl.ContainerServiceImpl;
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
import org.springframework.transaction.support.ResourceTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@Import({CommandConfig.class, HibernateConfig.class})
public class CommandTestConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ContainerService containerService(final CommandService commandService,
                                             final ContainerControlApi containerControlApi,
                                             final ContainerEntityService containerEntityService,
                                             final AliasTokenService aliasTokenService,
                                             final SiteConfigPreferences siteConfigPreferences,
                                             final TransportService transportService,
                                             final PermissionsServiceI permissionsService,
                                             final CatalogService catalogService,
                                             final ObjectMapper mapper,
                                             final ConfigService configService) {
        return new ContainerServiceImpl(commandService, containerControlApi, containerEntityService, aliasTokenService, siteConfigPreferences,
                transportService, permissionsService, catalogService, mapper, configService);
    }

    @Bean
    public ContainerControlApi controlApi() {
        return Mockito.mock(ContainerControlApi.class);
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
    public TransportService transportService() {
        return new TransportServiceImpl();
    }

    @Bean
    public ContainerEntityService mockContainerEntityService() {
        return Mockito.mock(ContainerEntityService.class);
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
                CommandWrapperOutputEntity.class);
        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }
}
