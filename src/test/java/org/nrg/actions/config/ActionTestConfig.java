package org.nrg.actions.config;


import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.nrg.actions.daos.ActionDao;
import org.nrg.actions.daos.CommandDao;
import org.nrg.actions.model.*;
import org.nrg.actions.services.ActionService;
import org.nrg.actions.services.CommandService;
import org.nrg.actions.services.HibernateActionService;
import org.nrg.actions.services.HibernateCommandService;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.daos.DockerImageDao;
import org.nrg.containers.services.DockerImageService;
import org.nrg.containers.services.HibernateDockerImageService;
import org.nrg.framework.orm.hibernate.AggregatedAnnotationSessionFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.transaction.support.ResourceTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@Import(ActionsHibernateEntityTestConfig.class)
public class ActionTestConfig {
    @Bean
    public ActionService actionService() {
        return new HibernateActionService();
    }

    @Bean
    public ActionDao actionDao() {
        return new ActionDao();
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
    public DockerImageService dockerImageService() {
        return new HibernateDockerImageService();
    }

    @Bean
    public DockerImageDao dockerImageDao() {
        return new DockerImageDao();
    }

    @Bean
    public ContainerControlApi controlApi() {
        return Mockito.mock(ContainerControlApi.class);
    }

    @Bean
    public AggregatedAnnotationSessionFactoryBean sessionFactory(final DataSource dataSource, @Qualifier("hibernateProperties") final Properties properties) {
        final AggregatedAnnotationSessionFactoryBean bean = new AggregatedAnnotationSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setHibernateProperties(properties);
        bean.setAnnotatedClasses(
                Action.class,
                Command.class);
        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }
}
