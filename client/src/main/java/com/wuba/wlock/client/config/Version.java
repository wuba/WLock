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
package com.wuba.wlock.client.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.util.Properties;

public class Version {
	private static final Log logger = LogFactory.getLog(Version.class);
	
	public static String INFO;
	public static final String LANGUAGE = "Java"; 
	private static String Version = "0.0.0";
	
	static {
		Properties props = new Properties();
		try (InputStream resourceAsStream = Version.class.getResourceAsStream("/META-INF/maven/com.wuba.wlock/client/pom.properties")){
			props.load(resourceAsStream);
			Version = props.getProperty("version");
			INFO = "[WLock-Client]V" + Version;
			logger.info("WLock Client version is " + Version);
		} catch (Exception e) {
			logger.error("Get WLock Client version error.", e);
		}
	}
	
	private Version() {
	}
	
}
