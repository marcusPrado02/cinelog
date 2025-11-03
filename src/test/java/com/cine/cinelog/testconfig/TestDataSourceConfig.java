package com.cine.cinelog.testconfig;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@TestConfiguration
public class TestDataSourceConfig {
    @Bean
    @Primary
    public DataSource testDataSource(@Qualifier("rawDataSource") DataSource raw) {
        return raw;
    }
}
