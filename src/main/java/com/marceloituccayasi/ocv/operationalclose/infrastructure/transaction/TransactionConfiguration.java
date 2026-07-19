package com.marceloituccayasi.ocv.operationalclose.infrastructure.transaction;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Configures explicit transaction execution for application use cases.
 */
@Configuration(proxyBeanMethods = false)
public class TransactionConfiguration {

    @Bean
    TransactionTemplate transactionTemplate(
            PlatformTransactionManager transactionManager) {

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.setIsolationLevel(
                TransactionDefinition.ISOLATION_READ_COMMITTED);

        return transactionTemplate;
    }

}
