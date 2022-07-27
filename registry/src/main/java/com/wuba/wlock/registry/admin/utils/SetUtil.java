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
package com.wuba.wlock.registry.admin.utils;

import java.util.HashSet;
import java.util.Set;


public class SetUtil {

	public static <T> Set<T> diffSet(Set<T> set1, Set<T> set2) {
		Set<T> result = new HashSet<>(set1);
		Set<T> m1 = new HashSet<>(set1);
		Set<T> m2 = new HashSet<>(set2);
		result.removeAll(set2);
		m2.removeAll(m1);
		result.addAll(m2);
		return result;
	}
}
