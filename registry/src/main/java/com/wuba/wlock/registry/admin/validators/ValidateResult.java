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
package com.wuba.wlock.registry.admin.validators;


public enum ValidateResult {
    /**
     * 验证通过
     */
    PASS(""),
    /**
     * 验证失败
     */
    NOT_PASS("");

    private String errMsg;

    public String getErrMsg() {
        return errMsg;
    }

    public ValidateResult setErrMsg(String errMsg) {
        this.errMsg = errMsg;
        return this;
    }

    ValidateResult(String errMsg){
        this.errMsg = errMsg;
    }

    public boolean isPass(){
        return this==PASS;
    }

}
