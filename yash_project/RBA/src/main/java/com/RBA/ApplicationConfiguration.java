package com.RBA;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class ApplicationConfiguration {

    private final RBADatabaseConfiguration rbaDatabaseConfiguration;

    @Lazy
    public ApplicationConfiguration(RBADatabaseConfiguration rbaDatabaseConfiguration) {
        this.rbaDatabaseConfiguration = rbaDatabaseConfiguration;
    }

    @Bean
    public DataSource dataSourceRBA(){
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName(rbaDatabaseConfiguration.getDriverClass());
        dataSourceBuilder.url(rbaDatabaseConfiguration.getJdbcUrl());
        dataSourceBuilder.username(rbaDatabaseConfiguration.getUserName());
        dataSourceBuilder.password(rbaDatabaseConfiguration.getPassword());
        return dataSourceBuilder.build();
    }

    @Bean
    public NamedParameterJdbcTemplate jdbcTemplate(){
        return new NamedParameterJdbcTemplate(dataSourceRBA());
    }
}
