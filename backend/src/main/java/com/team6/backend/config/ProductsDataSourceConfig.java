package com.team6.backend.config;


import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;


@EnableJpaRepositories(
        basePackages = {"com.team6.backend.products"},
        entityManagerFactoryRef = "productsEntityManagerFactory",
        transactionManagerRef = "productsTransactionManager"
)
@Configuration
public class ProductsDataSourceConfig {
    // @Primary
    @Bean
    @ConfigurationProperties(prefix = "products.datasource")
    DataSource productsDataSource() {
        return DataSourceBuilder.create().build();
    }

    // @Primary
    @Bean
    LocalContainerEntityManagerFactoryBean productsEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("productsDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.team6.backend.products")
                .persistenceUnit("products")
                .build();
    }

    // @Primary
    @Bean
    PlatformTransactionManager productsTransactionManager(
            @Qualifier("productsEntityManagerFactory") EntityManagerFactory localEntityManagerFactory) {
        return new JpaTransactionManager(localEntityManagerFactory);
    }
}
