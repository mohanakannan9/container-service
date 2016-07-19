package org.nrg.execution.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.nrg.execution.daos.CommandDao;
import org.nrg.execution.model.Command;
import org.nrg.execution.services.AceService;
import org.nrg.execution.services.CommandService;
import org.nrg.execution.services.HibernateCommandService;
import org.nrg.automation.entities.Script;
import org.nrg.automation.repositories.ScriptRepository;
import org.nrg.automation.services.ScriptService;
import org.nrg.automation.services.impl.hibernate.HibernateScriptService;
import org.nrg.execution.api.ContainerControlApi;
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

    @Bean
    public ScriptService scriptService() {
        return new HibernateScriptService();
    }

    @Bean
    public ScriptRepository scriptRepository() {
        return new ScriptRepository();
    }

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
    public AggregatedAnnotationSessionFactoryBean sessionFactory(final DataSource dataSource, @Qualifier("hibernateProperties") final Properties properties) {
        final AggregatedAnnotationSessionFactoryBean bean = new AggregatedAnnotationSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setHibernateProperties(properties);
        bean.setAnnotatedClasses(
                Command.class,
                Script.class);
        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }

    @Bean
    public AceService aceService() {
        return Mockito.mock(AceService.class);
    }
}
