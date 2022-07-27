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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.PARAMETER})
public @interface ValidationCheck {

    /**
     * group
     * @return
     */
    String[] group() default {};

    /**
     * 是否允许为空
     * @return
     */
    boolean allowEmpty() default  true;

    /**
     * 最大值
     * @return
     */
    String maxValue() default "";

    /**
     * 最小值
     * @return
     */
    String minValue() default "";

    /**
     * 最小长度
     * @return
     */
    int minLength() default 0;

    /**
     * 最大长度
     * @return
     */
    int maxLength() default 0;

    /**
     * 指定取值范围 和 enumClass 二选一
     * @return
     */
    String valueIn() default "";

    /**
     * 不在指定范围
     * @return
     */
    String valueNotIn() default "";

    /**
     * 表达式验证
     * @return
     */
    String regexExpression() default  "";

    /**
     * 字段描述
     * @return
     */
    String filedDescription() default "";

    /**
     * 是否允许中文
     * @return
     */
    boolean allowChineseLanguage() default true;

}
