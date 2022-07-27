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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.util.Collection;

public class LogConfig {
	private static LogConfig instance = new LogConfig();
	private volatile boolean debugEnabled = false;
			
	private LogConfig() {}
	
	public static LogConfig getInstance() {
		return instance;
	}
	
	public void enableDebug() {
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Collection<Logger> loggers = ctx.getLoggers();
		if (loggers != null) {
			for (Logger logger : loggers) {
				logger.setLevel(Level.DEBUG);
			}
		}
		debugEnabled = true;
	}
	
	public void disableDebug() {
		if (!debugEnabled) {
			return;
		}
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Collection<Logger> loggers = ctx.getLoggers();
		if (loggers != null) {
			for (Logger logger : loggers) {
				if (logger.isDebugEnabled()) {
					logger.setLevel(Level.INFO);
				}
			}
		}
		debugEnabled = false;
	}
}
