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


import com.wuba.wlock.registry.admin.domain.ActionResult;
import com.wuba.wlock.registry.admin.domain.CommonArrayResponse;
import com.wuba.wlock.registry.admin.domain.CommonResponse;
import com.wuba.wlock.registry.admin.exceptions.ServiceException;

public final class CommonResultUtil {

	private CommonResultUtil() {
	}

	public static ActionResult buildResult(Exception e) {
		e.printStackTrace();
		CommonResponse commonResponse;
		if (e instanceof ServiceException) {
			String msg = e.getMessage();
			commonResponse = CommonResponse.buildFail(msg);
		} else {
			commonResponse = CommonResponse.buildFail("未知错误，请联系管理员");
		}
		return buildResult(commonResponse);
	}

	public static ActionResult buildResult(final CommonResponse commonResponse) {
		return commonResponse;
	}

	public static ActionResult buildResult(final CommonArrayResponse commonResponse) {
		return commonResponse;
	}


}
