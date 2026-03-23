package com.cine.cinelog.features.batch.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.cloud.task.configuration.TaskLifecycleConfiguration;
import org.springframework.cloud.task.configuration.TaskObservationCloudKeyValues;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskNameResolver;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * Configura Spring Cloud Task manualmente (sem @EnableTask e sem SimpleTaskAutoConfiguration)
 * para evitar referencia circular com entityManagerFactory.
 *
 * Registra:
 * - TaskRepository, TaskExplorer, TaskNameResolver com prefix BOOT3_TASK_
 * - springCloudTaskTransactionManager (DataSource-based, para SCT)
 * - transactionManager (JPA-based, @Primary, para BatchJobsConfig e repositories)
 * - TaskLifecycleConfiguration (via @Import) para lifecycle tracking e close-context
 * - TaskProperties e TaskObservationCloudKeyValues (dependencias do lifecycle)
 */
@Configuration
@Profile("task")
@EnableConfigurationProperties(TaskProperties.class)
@Import(TaskLifecycleConfiguration.class)
public class TaskConfig implements TaskConfigurer {

    private final DataSource dataSource;
    private final TaskExecutionDaoFactoryBean taskExecutionDaoFactory;
    private final DataSourceTransactionManager sctTransactionManager;

    public TaskConfig(DataSource dataSource) {
        this.dataSource = dataSource;
        this.taskExecutionDaoFactory = new TaskExecutionDaoFactoryBean(dataSource, "BOOT3_TASK_");
        this.sctTransactionManager = new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @Override
    public TaskRepository getTaskRepository() {
        return new SimpleTaskRepository(taskExecutionDaoFactory);
    }

    @Bean
    @Override
    public TaskExplorer getTaskExplorer() {
        return new SimpleTaskExplorer(taskExecutionDaoFactory);
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return sctTransactionManager;
    }

    @Bean
    @Override
    public TaskNameResolver getTaskNameResolver() {
        return new SimpleTaskNameResolver();
    }

    @Override
    public DataSource getTaskDataSource() {
        return dataSource;
    }

    @Bean(name = "springCloudTaskTransactionManager")
    public PlatformTransactionManager springCloudTaskTransactionManager() {
        return sctTransactionManager;
    }

    /**
     * JpaTransactionManager registrado com nome 'transactionManager' e @Primary.
     * Necessario porque o Spring Boot JPA autoconfig pode nao criar este bean
     * quando o SimpleTaskAutoConfiguration esta excluido.
     * O BatchJobsConfig e @Transactional nos repositories JPA dependem deste bean.
     */
    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    /**
     * Dependencia do TaskLifecycleConfiguration — normalmente criado pelo
     * SimpleTaskAutoConfiguration apenas com @Profile("cloud").
     * Aqui fornecemos um bean vazio para satisfazer a injecao.
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskObservationCloudKeyValues taskObservationCloudKeyValues() {
        return new TaskObservationCloudKeyValues();
    }

    /**
     * Inicializador das tabelas BOOT3_TASK_* no banco.
     * Normalmente criado pelo SimpleTaskAutoConfiguration.
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskRepositoryInitializer taskRepositoryInitializer(TaskProperties taskProperties) {
        var initializer = new TaskRepositoryInitializer(taskProperties);
        initializer.setDataSource(dataSource);
        return initializer;
    }
}
