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
package com.wuba.wlock.repository.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class MybatisPlusConfig {

	/**
	 * 分表字段
	 */
	public static final ThreadLocal<String> splitColumn = new ThreadLocal<>();

	private static Map<String, Integer> tableToTableCount = new HashMap<>();

	@Bean
	public MybatisPlusInterceptor mybatisPlusInterceptor() {
		MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
		DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor = new DynamicTableNameInnerInterceptor();
		dynamicTableNameInnerInterceptor.setTableNameHandler((sql, tableName) -> getTableName(tableName));
		interceptor.addInnerInterceptor(dynamicTableNameInnerInterceptor);
		return interceptor;
	}

	static {
		tableToTableCount.put("t_cluster_group_qps_", 16);
		tableToTableCount.put("t_key_qps_", 128);
		tableToTableCount.put("t_server_qps_", 8);
	}

	private String getTableName(String tableName) {
		if (Strings.isNotEmpty(splitColumn.get()) && tableToTableCount.containsKey(tableName)) {
			String newTableName = tableName + Math.abs(splitColumn.get().hashCode() % tableToTableCount.get(tableName));
			splitColumn.remove();
			return newTableName;
		}
		return tableName;
	}
}
