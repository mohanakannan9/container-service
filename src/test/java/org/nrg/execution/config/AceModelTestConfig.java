package org.nrg.execution.config;


import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.nrg.execution.daos.AceDao;
import org.nrg.execution.daos.ActionDao;
import org.nrg.execution.daos.CommandDao;
import org.nrg.execution.model.Action;
import org.nrg.execution.model.ActionContextExecution;
import org.nrg.execution.model.Command;
import org.nrg.execution.services.AceService;
import org.nrg.execution.services.ActionService;
import org.nrg.execution.services.CommandService;
import org.nrg.execution.services.HibernateAceService;
import org.nrg.execution.services.HibernateActionService;
import org.nrg.execution.services.HibernateCommandService;
import org.nrg.automation.entities.Script;
import org.nrg.execution.api.ContainerControlApi;
import org.nrg.framework.orm.hibernate.AggregatedAnnotationSessionFactoryBean;
import org.nrg.transporter.TransportService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.transaction.support.ResourceTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@Import(ExecutionHibernateEntityTestConfig.class)
public class AceModelTestConfig {
    @Bean
    public AceService aceService() {
        return new HibernateAceService();
    }

    @Bean
    public AceDao aceDao() {
        return new AceDao();
    }

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
    public ContainerControlApi controlApi() {
        return Mockito.mock(ContainerControlApi.class);
    }

    @Bean
    public TransportService transportService() {
        return Mockito.mock(TransportService.class);
    }

    @Bean
    public AggregatedAnnotationSessionFactoryBean sessionFactory(final DataSource dataSource, @Qualifier("hibernateProperties") final Properties properties) {
        final AggregatedAnnotationSessionFactoryBean bean = new AggregatedAnnotationSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setHibernateProperties(properties);
        bean.setAnnotatedClasses(
                Action.class,
                Command.class,
                Script.class,
                ActionContextExecution.class);
        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }
}
