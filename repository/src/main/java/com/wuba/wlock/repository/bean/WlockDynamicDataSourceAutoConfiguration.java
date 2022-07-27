/*
 * Copyright (C) 2005-present, 58.com.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wuba.wlock.repository.bean;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.processor.DsProcessor;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceCreatorAutoConfiguration;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourcePropertiesCustomizer;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDatasourceAopProperties;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.druid.DruidDynamicDataSourceConfiguration;
import com.wuba.wlock.repository.interceptor.WlockDynamicDataSourceAnnotationInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Role;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Configuration
@EnableConfigurationProperties(DynamicDataSourceProperties.class)
@AutoConfigureBefore(value = DataSourceAutoConfiguration.class, name = "com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure")
@Import(value = {DruidDynamicDataSourceConfiguration.class, DynamicDataSourceCreatorAutoConfiguration.class})
@ConditionalOnProperty(prefix = DynamicDataSourceProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class WlockDynamicDataSourceAutoConfiguration implements InitializingBean {

	private final DynamicDataSourceProperties properties;

	private final List<DynamicDataSourcePropertiesCustomizer> dataSourcePropertiesCustomizers;

	public WlockDynamicDataSourceAutoConfiguration(
			DynamicDataSourceProperties properties,
			ObjectProvider<List<DynamicDataSourcePropertiesCustomizer>> dataSourcePropertiesCustomizers) {
		this.properties = properties;
		this.dataSourcePropertiesCustomizers = dataSourcePropertiesCustomizers.getIfAvailable();
	}

	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	@Bean
	@ConditionalOnProperty(prefix = DynamicDataSourceProperties.PREFIX + ".aop", name = "enabled", havingValue = "true", matchIfMissing = true)
	public Advisor wlockDynamicDatasourceAnnotationAdvisor(DsProcessor dsProcessor) {
		DynamicDatasourceAopProperties aopProperties = properties.getAop();
		WlockDynamicDataSourceAnnotationInterceptor interceptor = new WlockDynamicDataSourceAnnotationInterceptor(aopProperties.getAllowedPublicOnly(), dsProcessor);
		DynamicDataSourceAnnotationAdvisor advisor = new DynamicDataSourceAnnotationAdvisor(interceptor, DS.class);
		// 注意这里 order 要和原生的不一样,保证修改数据源成功
		advisor.setOrder(aopProperties.getOrder() + 1);
		return advisor;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (!CollectionUtils.isEmpty(dataSourcePropertiesCustomizers)) {
			for (DynamicDataSourcePropertiesCustomizer customizer : dataSourcePropertiesCustomizers) {
				customizer.customize(properties);
			}
		}
	}
}
