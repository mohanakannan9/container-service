package org.nrg.containers.config;

import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.api.impl.DockerControlApi;
import org.nrg.containers.daos.DockerImageDao;
import org.nrg.containers.model.DockerHubPrefs;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.services.ContainerService;
import org.nrg.containers.services.DockerImageService;
import org.nrg.containers.services.DockerService;
import org.nrg.containers.services.DockerServiceImpl;
import org.nrg.containers.services.HibernateDockerImageService;
import org.nrg.framework.orm.hibernate.AggregatedAnnotationSessionFactoryBean;
import org.nrg.prefs.services.NrgPreferenceService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableWebMvc
@ComponentScan("org.nrg.containers.rest")
@Import(ContainersHibernateEntityTestConfig.class)
public class DockerImageTestConfig {
    @Bean
    public DockerImageService dockerImageService() {
        return new HibernateDockerImageService();
    }

    @Bean
    public DockerImageDao dockerImageDao() {
        return new DockerImageDao();
    }

    @Bean
    public DockerService dockerService() {
        return new DockerServiceImpl();
    }

    @Bean
    public ContainerControlApi mockContainerControlApi() {
        return Mockito.mock(ContainerControlApi.class);
    }

    @Bean
    public DockerHubPrefs mockDockerHubPrefs() {
        return Mockito.mock(DockerHubPrefs.class);
    }

    @Bean
    public NrgPreferenceService mockNrgPreferenceService() {
        return Mockito.mock(NrgPreferenceService.class);
    }

    @Bean
    public ContainerService mockContainerService() {
        return Mockito.mock(ContainerService.class);
    }

    @Bean
    public AggregatedAnnotationSessionFactoryBean sessionFactory(final DataSource dataSource, @Qualifier("hibernateProperties") final Properties properties) {
        final AggregatedAnnotationSessionFactoryBean bean = new AggregatedAnnotationSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setHibernateProperties(properties);
        bean.setAnnotatedClasses(
                DockerImage.class);
        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }
}
