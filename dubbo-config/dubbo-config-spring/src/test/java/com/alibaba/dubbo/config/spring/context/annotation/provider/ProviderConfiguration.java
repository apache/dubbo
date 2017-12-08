package com.alibaba.dubbo.config.spring.context.annotation.provider;

import com.alibaba.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author ken.lj
 * @date 2017/11/3
 */
@DubboComponentScan(basePackages = "com.alibaba.dubbo.config.spring.context.annotation")
@ImportResource("META-INF/spring/dubbo-annotation-provider.xml")
@PropertySource("META-INF/default.properties")
@EnableTransactionManagement
public class ProviderConfiguration {

    @Primary
    @Bean
    public PlatformTransactionManager platformTransactionManager() {
        return new PlatformTransactionManager(){

            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                return null;
            }

            @Override
            public void commit(TransactionStatus status) throws TransactionException {

            }

            @Override
            public void rollback(TransactionStatus status) throws TransactionException {

            }
        };
    }

}

