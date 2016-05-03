package org.nrg.containers.config;

import org.hibernate.SessionFactory;
import org.nrg.actions.model.Command;
import org.nrg.actions.model.CommandInput;
import org.nrg.actions.model.Output;
import org.nrg.containers.daos.DockerImageCommandDao;
import org.nrg.containers.daos.DockerImageDao;
import org.nrg.containers.model.DockerImageCommand;
import org.nrg.containers.model.Image;
import org.nrg.containers.services.DockerImageCommandService;
import org.nrg.containers.services.DockerImageService;
import org.nrg.containers.services.HibernateDockerImageCommandService;
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
public class DockerImageCommandTestConfig {
    @Bean
    public DockerImageService dockerImageService() {
        return new HibernateDockerImageService();
    }

    @Bean
    public DockerImageDao dockerImageDao() {
        return new DockerImageDao();
    }

    @Bean
    public DockerImageCommandService dockerImageCommandService() {
        return new HibernateDockerImageCommandService();
    }

    @Bean
    public DockerImageCommandDao dockerImageCommandDao() {
        return new DockerImageCommandDao();
    }

    @Bean
    public AggregatedAnnotationSessionFactoryBean sessionFactory(final DataSource dataSource, @Qualifier("hibernateProperties") final Properties properties) {
        final AggregatedAnnotationSessionFactoryBean bean = new AggregatedAnnotationSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setHibernateProperties(properties);
        bean.setAnnotatedClasses(
                Command.class,
                DockerImageCommand.class,
                Image.class,
                CommandInput.class,
                Output.class);
        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }
}
