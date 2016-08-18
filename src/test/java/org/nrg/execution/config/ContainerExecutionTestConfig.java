package org.nrg.execution.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.nrg.execution.api.ContainerControlApi;
import org.nrg.execution.daos.CommandDao;
import org.nrg.execution.daos.ContainerExecutionRepository;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.ContainerExecution;
import org.nrg.execution.services.CommandService;
import org.nrg.execution.services.ContainerExecutionService;
import org.nrg.execution.services.HibernateCommandService;
import org.nrg.execution.services.HibernateContainerExecutionService;
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
import reactor.bus.EventBus;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
//@EnableTransactionManagement
@Import(ExecutionHibernateEntityTestConfig.class)
public class ContainerExecutionTestConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public EventBus eventBus() {
        return Mockito.mock(EventBus.class);
    }

    @Bean
    public ContainerExecutionService containerExecutionService(final EventBus eventBus) {
        return new HibernateContainerExecutionService(eventBus);
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
                ContainerExecution.class);
        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }
}
