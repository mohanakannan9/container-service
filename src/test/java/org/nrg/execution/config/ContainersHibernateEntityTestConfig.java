package org.nrg.execution.config;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

@Configuration
public class ContainersHibernateEntityTestConfig {
    @Bean
    public Properties hibernateProperties() throws IOException {
        Properties properties = new Properties();
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.hbm2ddl.auto", "create");
        properties.put("hibernate.cache.use_second_level_cache", false);
        properties.put("hibernate.cache.use_query_cache", false);

        PropertiesFactoryBean hibernate = new PropertiesFactoryBean();
        hibernate.setProperties(properties);
        hibernate.afterPropertiesSet();
        return hibernate.getObject();
    }

//    @Bean
//    public HibernateEntityPackageList hibernateEntityPackageList() {
//        return (new HibernateEntityPackageList("org.nrg.containers"));
//    }

    @Bean
    public DataSource dataSource() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.h2.Driver.class);
        dataSource.setUrl("jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUsername("sa");
        return dataSource;
    }

    //    @Bean
//    public RegionFactory cacheRegionFactory() throws IOException {
//        return new SingletonEhCacheRegionFactory(hibernateProperties());
//    }


//    @Bean
//    public SessionFactory sessionFactory() throws Exception {
//        AggregatedAnnotationSessionFactoryBean sessionFactoryBean = new AggregatedAnnotationSessionFactoryBean();
//        sessionFactoryBean.setDataSource(dataSource());
//        sessionFactoryBean.setHibernateProperties(hibernateProperties());
////        sessionFactoryBean.setCacheRegionFactory(cacheRegionFactory());
//        sessionFactoryBean.afterPropertiesSet();
//
//        return sessionFactoryBean.getObject();
//    }
//
//    @Bean
//    public PlatformTransactionManager txManager() {
//        return new DataSourceTransactionManager(dataSource());
//    }
}
