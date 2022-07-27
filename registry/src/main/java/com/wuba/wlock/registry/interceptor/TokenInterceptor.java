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
package com.wuba.wlock.registry.interceptor;

import com.alibaba.fastjson.JSON;
import com.wuba.wlock.registry.admin.domain.ActionResult;
import com.wuba.wlock.registry.admin.domain.CommonResponse;
import com.wuba.wlock.registry.admin.utils.CommonResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

	@Value("${registry.token}")
	String callToken;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		try {
			String token = request.getHeader("token");
			if (StringUtils.isEmpty(token)) {
				returnJson(response, "入参错误：没有token");
				return false;
			}

			if (!callToken.equals(token)) {
				returnJson(response, "入参错误：token错误");
				return false;
			}

			return true;
		} catch (Exception e) {
			log.error("TokenInterceptor error", e);
			return false;
		}
	}

	private void returnJson(HttpServletResponse response, String result) throws Exception {
		PrintWriter writer = null;
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html; charset=utf-8");
		try {
			ActionResult actionResult = CommonResultUtil.buildResult(CommonResponse.buildFail(result));
			writer = response.getWriter();
			writer.print(JSON.toJSONString(actionResult));
		} catch (IOException e) {
			log.error("response error", e);
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

}
