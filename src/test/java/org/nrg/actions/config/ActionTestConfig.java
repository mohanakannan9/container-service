package org.nrg.actions.config;


import org.hibernate.SessionFactory;
import org.nrg.actions.daos.ActionDao;
import org.nrg.actions.model.Action;
import org.nrg.actions.model.CommandInput;
import org.nrg.actions.model.Output;
import org.nrg.actions.model.matcher.Matcher;
import org.nrg.actions.model.ActionInput;
import org.nrg.actions.model.tree.MatchTreeNode;
import org.nrg.actions.services.ActionService;
import org.nrg.actions.services.HibernateActionService;
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
    public DockerImageCommandService dockerImageCommandService() {
        return new HibernateDockerImageCommandService();
    }

    @Bean
    public DockerImageCommandDao dockerImageCommandDao() {
        return new DockerImageCommandDao();
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
    public AggregatedAnnotationSessionFactoryBean sessionFactory(final DataSource dataSource, @Qualifier("hibernateProperties") final Properties properties) {
        final AggregatedAnnotationSessionFactoryBean bean = new AggregatedAnnotationSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setHibernateProperties(properties);
        bean.setAnnotatedClasses(
                Action.class,
                ActionInput.class,
                MatchTreeNode.class,
                Matcher.class,
                CommandInput.class,
                Image.class,
                Output.class,
                DockerImageCommand.class);
        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }
}
