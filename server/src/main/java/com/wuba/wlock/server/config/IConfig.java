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
package com.wuba.wlock.server.config;

import com.google.common.base.Strings;
import com.wuba.wlock.server.exception.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;


public abstract class IConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(IConfig.class);
	protected Properties properties;
	private static String ERROR_MESSAGE_KEY_NOT_EXIST = "property: %s not found in file %s";
	private static String ERROR_MESSAGE_KEY_TYPE_ERROR = "property: %s is not %s in file %s";
	protected String path;

	void initConfig(String path, boolean mustExist) throws ConfigException {
		properties = new Properties();
		this.path = path;
		try (FileInputStream fileInputStream = new FileInputStream(path)) {
			properties.load(fileInputStream);
			loadSpecial();
		} catch(FileNotFoundException e) {
			if (!mustExist) {
				LOGGER.warn("config file path {} not exist.use default", path, e);
				return;
			}
			throw new ConfigException(e);
		} catch(IOException e) {
			if (!mustExist) {
				LOGGER.warn("config file path {} read throw exception.use default", path, e);
				return;
			}
			throw new ConfigException(e);
		}
	}

	public abstract void init(String path, boolean mustExist) throws ConfigException;

	/**
	 * 加载需要特殊处理的数据
	 */
	protected abstract void loadSpecial();

	protected int getInt(String key) throws ConfigException {
		String property = properties.getProperty(key);
		if (Strings.isNullOrEmpty(property)) {
			throw new ConfigException(String.format(ERROR_MESSAGE_KEY_NOT_EXIST, key, path));
		}
		try {
			return Integer.parseInt(property);
		} catch(Exception e) {
			throw new ConfigException(String.format(ERROR_MESSAGE_KEY_TYPE_ERROR, key, "Integer", path));
		}
	}

	protected int getInt(String key, int defaultValue) {
		String property = properties.getProperty(key);
		if (Strings.isNullOrEmpty(property)) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(property);
		} catch(Exception e) {
			return defaultValue;
		}
	}

	protected String getString(String key) throws ConfigException {
		String property = properties.getProperty(key);
		if (Strings.isNullOrEmpty(property)) {
			throw new ConfigException(String.format(ERROR_MESSAGE_KEY_NOT_EXIST, key, path));
		}
		return property;
	}

	protected String getString(String key, String defaultValue) {
		String property = properties.getProperty(key);
		if (Strings.isNullOrEmpty(property)) {
			return defaultValue;
		}
		try {
			return property;
		} catch(Exception e) {
			return defaultValue;
		}
	}

	protected double getDouble(String key) throws ConfigException {
		String property = properties.getProperty(key);
		if (Strings.isNullOrEmpty(property)) {
			throw new ConfigException(String.format(ERROR_MESSAGE_KEY_NOT_EXIST, key, path));
		}
		try {
			return Double.parseDouble(property);
		} catch(Exception e) {
			throw new ConfigException(String.format(ERROR_MESSAGE_KEY_TYPE_ERROR, key, "Double", path));
		}
	}

	protected double getDouble(String key, double defaultValue) {
		String property = properties.getProperty(key);
		if (Strings.isNullOrEmpty(property)) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(property);
		} catch(Exception e) {
			return defaultValue;
		}
	}

	protected long getLong(String key) throws ConfigException {
		String property = properties.getProperty(key);
		if (Strings.isNullOrEmpty(property)) {
			throw new ConfigException(String.format(ERROR_MESSAGE_KEY_NOT_EXIST, key, path));
		}
		try {
			return Long.parseLong(property);
		} catch(Exception e) {
			throw new ConfigException(String.format(ERROR_MESSAGE_KEY_TYPE_ERROR, key, "Long", path));
		}
	}

	protected long getLong(String key, long defaultValue) {
		String property = properties.getProperty(key);
		if (Strings.isNullOrEmpty(property)) {
			return defaultValue;
		}
		try {
			return Long.parseLong(property);
		} catch(Exception e) {
			return defaultValue;
		}
	}

	protected boolean getBoolean(String key) throws ConfigException {
		String property = properties.getProperty(key);
		if (Strings.isNullOrEmpty(property)) {
			throw new ConfigException(String.format(ERROR_MESSAGE_KEY_NOT_EXIST, key, path));
		}
		try {
			return Boolean.parseBoolean(property);
		} catch(Exception e) {
			throw new ConfigException(String.format(ERROR_MESSAGE_KEY_TYPE_ERROR, key, "Boolean", path));
		}
	}

	protected boolean getBoolean(String key, boolean defaultValue) {
		String property = properties.getProperty(key);
		if (Strings.isNullOrEmpty(property)) {
			return defaultValue;
		}
		try {
			return Boolean.parseBoolean(property);
		} catch(Exception e) {
			return defaultValue;
		}
	}
}
