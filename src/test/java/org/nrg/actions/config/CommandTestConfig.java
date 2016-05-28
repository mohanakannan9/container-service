package org.nrg.actions.config;

import org.hibernate.SessionFactory;
import org.nrg.actions.daos.CommandDao;
import org.nrg.actions.daos.ScriptEnvironmentDao;
import org.nrg.actions.model.Command;
import org.nrg.actions.model.CommandVariable;
import org.nrg.actions.model.ScriptEnvironment;
import org.nrg.actions.services.CommandService;
import org.nrg.actions.services.HibernateCommandService;
import org.nrg.actions.services.HibernateScriptEnvironmentService;
import org.nrg.actions.services.ScriptEnvironmentService;
import org.nrg.automation.entities.Script;
import org.nrg.actions.model.ScriptCommand;
import org.nrg.automation.repositories.ScriptRepository;
import org.nrg.automation.services.ScriptService;
import org.nrg.automation.services.impl.hibernate.HibernateScriptService;
import org.nrg.containers.config.ContainersHibernateEntityTestConfig;
import org.nrg.containers.daos.DockerImageDao;
import org.nrg.containers.model.DockerImageCommand;
import org.nrg.containers.model.DockerImage;
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
//@EnableTransactionManagement
@Import(ContainersHibernateEntityTestConfig.class)
public class CommandTestConfig {
    @Bean
    public DockerImageService dockerImageService() {
        return new HibernateDockerImageService();
    }

    @Bean
    public DockerImageDao dockerImageDao() {
        return new DockerImageDao();
    }

    @Bean
    public ScriptService scriptService() {
        return new HibernateScriptService();
    }

    @Bean
    public ScriptRepository scriptRepository() {
        return new ScriptRepository();
    }

    @Bean
    public ScriptEnvironmentService scriptEnvironmentService() {
        return new HibernateScriptEnvironmentService();
    }

    @Bean
    public ScriptEnvironmentDao scriptEnvironmentDao() {
        return new ScriptEnvironmentDao();
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
    public AggregatedAnnotationSessionFactoryBean sessionFactory(final DataSource dataSource, @Qualifier("hibernateProperties") final Properties properties) {
        final AggregatedAnnotationSessionFactoryBean bean = new AggregatedAnnotationSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setHibernateProperties(properties);
        bean.setAnnotatedClasses(
                Command.class,
                DockerImageCommand.class,
                DockerImage.class,
                ScriptCommand.class,
                Script.class,
                ScriptEnvironment.class,
                CommandVariable.class);
        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }
}
