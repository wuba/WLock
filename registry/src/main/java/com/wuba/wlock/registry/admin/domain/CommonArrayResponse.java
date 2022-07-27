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

import java.util.Collection;


public class CommonArrayResponse extends ActionResult{

	private Collection body;

	public CommonArrayResponse() {
	}

	public CommonArrayResponse(String message, Collection t) {
		super(message);
		this.body = t;
	}

	public CommonArrayResponse(String status, String msg) {
		super(status, msg);
	}

	public Collection getBody() {
		return body;
	}

	public void setBody(Collection body) {
		this.body = body;
	}

	public static CommonArrayResponse buildSuccess(Collection t) {
		return new CommonArrayResponse("success", t);
	}

	public static CommonArrayResponse buildSuccess(String msg, Collection t) {
		return new CommonArrayResponse(msg, t);
	}

	public static CommonArrayResponse buildFail(String msg) {
		return new CommonArrayResponse(ActionResult.FAIL, msg);
	}

}
