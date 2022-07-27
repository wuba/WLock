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
package com.wuba.wlock.server.migrate.protocol;

public enum MigrateResult {
    SUCCESS(1, "成功"),
    EXCEPTION(2, "异常"),
    STATE_CHECK_FAIL(3, "状态校验失败"),
    VERSION_CHECK_FAIL(4, "版本校验失败"),
    REGISTER_KEY_CHECK_FAIL(5, "密钥校验失败")
    ;
    private Integer value;

    private String desc;

    MigrateResult(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public Integer getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }
}
