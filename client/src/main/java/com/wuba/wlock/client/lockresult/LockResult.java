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
package com.wuba.wlock.client.lockresult;

import com.wuba.wlock.client.protocol.ResponseStatus;

public class LockResult {
	boolean ret = false;
	short responseStatus = -1;
	
	public LockResult() {
	}
	
	public LockResult(boolean ret, short responseStatus) {
		this.ret = ret;
		this.responseStatus = responseStatus;
	}

	public boolean isSuccess() {
		return ret;
	}
	
	public void setRet(boolean ret) {
		this.ret = ret;
	}
	
	public short getResponseStatus() {
		return responseStatus;
	}
	
	public void setResponseStatus(short responseStatus) {
		this.responseStatus = responseStatus;
	}

	@Override
	public String toString() {
		return "LockResult [result=" + ret + ", responseStatus=" + ResponseStatus.toStr(responseStatus) + "]";
	}
}
