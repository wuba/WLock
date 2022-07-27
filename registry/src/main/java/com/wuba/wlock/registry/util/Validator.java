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
package com.wuba.wlock.registry.util;

import com.wuba.wlock.common.exception.ValidateException;

public class Validator {

	private static final String DEFAULT_IS_NULL_EX_MESSAGE = "The validated object is null";

	public static void keyValidate(String key) throws ValidateException {
		if (!validateString(key)) {
			throw new ValidateException("key is empty");
		}
	}

	private static boolean validateString(String str) {
		if (null != str && !str.isEmpty()) {
			return true;
		}
		return false;
	}
	
	public static <T> T notNull(T object) {
		return notNull(object, DEFAULT_IS_NULL_EX_MESSAGE);
	}
	
	public static <T> boolean notNullAndEmpty(T object) {
		if(object == null){
			return false;
		}
		if("".equals(object)){
			return false;
		}
		return true;
	}
	
	public static <T> T notNull(T object, String message, Object... values) {
		if (object == null) {
			throw new NullPointerException(String.format(message, values));
		}
		return object;
	}

	public static <T extends CharSequence> T notEmpty(T chars, String message, Object... values) {
		if (chars == null) {
			throw new NullPointerException(String.format(message, values));
		}
		if (chars.length() == 0) {
			throw new IllegalArgumentException(String.format(message, values));
		}
		return chars;
	}
}
