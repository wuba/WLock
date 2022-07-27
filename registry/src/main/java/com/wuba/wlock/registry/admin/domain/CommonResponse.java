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
package com.wuba.wlock.registry.admin.domain;


public class CommonResponse extends ActionResult{
	private static final String SUCCESS = "0000";
	private static final String FAIL = "9999";
	private Object body;

	public CommonResponse() {
	}

	public CommonResponse(String message) {
		super(message);
	}

	public CommonResponse(String msg, Object body) {
		this(msg);
		this.body = body;
	}

	public Object getBody() {
		return body;
	}

	public void setBody(Object body) {
		this.body = body;
	}

	public static CommonResponse buildSuccess(Object t) {
		return new CommonResponse("success", t);
	}

	public static CommonResponse buildSuccess(String msg, Object t) {
		return new CommonResponse(msg, t);
	}

	public static CommonResponse buildSuccess(String msg) {
		return new CommonResponse(msg);
	}

	public static CommonResponse buildSuccess() {
		return new CommonResponse("操作成功");
	}

	public static CommonResponse buildFail(String msg) {
		CommonResponse commonResponse = new CommonResponse(msg);
		commonResponse.setStatus(FAIL);
		return commonResponse;
	}

}
