package com.wuba.wlock.starter;

import com.wuba.wlock.client.WLockClient;
import com.wuba.wlock.starter.config.WLockProperties;
import com.wuba.wlock.starter.processor.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author huguocai
 */
@Slf4j
@EnableConfigurationProperties(WLockProperties.class)
@ConditionalOnProperty(name = "wlock.enabled", havingValue = "true")
@ConditionalOnClass(WLockClient.class)
@Configuration
public class WLockAutoConfiguration {
    public WLockAutoConfiguration() {
    }

    @Bean
    @ConditionalOnMissingBean
    public WLockClientBeanProcessor wLockClientBeanProcessor(WLockProperties properties) {
        return new WLockClientBeanProcessor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public WLockBeanProcessor wLockBeanProcessor(WLockProperties properties) {
        return new WLockBeanProcessor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public WReadWriteLockBeanProcessor wReadWriteLockBeanProcessor(WLockProperties properties) {
        return new WReadWriteLockBeanProcessor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public WReadLockBeanProcessor wReadLockBeanProcessor(WLockProperties properties) {
        return new WReadLockBeanProcessor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public WWriteLockBeanProcessor wWriteLockBeanProcessor(WLockProperties properties) {
        return new WWriteLockBeanProcessor(properties);
    }
}