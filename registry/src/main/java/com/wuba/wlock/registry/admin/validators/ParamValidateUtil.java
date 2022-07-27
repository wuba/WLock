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



import com.wuba.wlock.registry.constant.CommonConstant;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.regex.Pattern;


public final class ParamValidateUtil {

	private ParamValidateUtil() {
	}

	static Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");

	/**
	 * 验证对象合法性
	 *
	 * @param args
	 * @return
	 */
	public static ValidateResult valid(Object... args) {

		Class c = args[0].getClass();
		Field[] fields = c.getDeclaredFields();

		for (Field field : fields) {
			//对于private私有化的成员变量，通过setAccessible来修改器访问权限
			field.setAccessible(true);
			try {
				ValidateResult result = validate(field, args[0], args.length > 1 ? (String) args[1] : null);
				if (result == ValidateResult.NOT_PASS) {
					return result;
				}
			} catch(Exception e) {
				return ValidateResult.NOT_PASS.setErrMsg("exception");
			}
			//重新设置会私有权限
			field.setAccessible(false);
		}
		return ValidateResult.PASS;
	}

	/**
	 * 验证属性合法性
	 *
	 * @param field
	 * @param object
	 * @return
	 */
	private static ValidateResult validate(Field field, Object object, String group) throws Exception {
		//获取属性值
		Object value = field.get(object);
		Class cl = field.getType();

		//获取注解
		ValidationCheck validationCheck = field.getAnnotation(ValidationCheck.class);
		if (validationCheck == null) {
			//不需要验证
			return ValidateResult.PASS;
		}

		//支持分组
		String[] groupIn = validationCheck.group();
		if (groupIn != null && groupIn.length > 0) {
			//指定的分组才验证
			if (group == null || !Arrays.asList(groupIn).contains(group)) {
				return ValidateResult.PASS;
			}
		}

		//字段描述信息
		String description = "".equals(validationCheck.filedDescription()) ? field.getName() : validationCheck.filedDescription();


		//是否为空
		boolean valueIsNull = value == null || "".equals(value);
		if (!validationCheck.allowEmpty()) {
			if (valueIsNull) {
				return ValidateResult.NOT_PASS.setErrMsg(description + "不能为空");
			}
		}
		//判断空
		if (valueIsNull) {
			//不允许为空
			if (!validationCheck.allowEmpty()) {
				return ValidateResult.NOT_PASS.setErrMsg(description + "不能为空");
			} else {
				return ValidateResult.PASS;//为空不用走其它验证，直接返回
			}
		}

		//是否基本类型
		boolean isPrimitive = cl.isPrimitive() || isBoxType(cl);

		//非基本类型和包装类，不支持验证，直接返回成功
		if (!isPrimitive) {
			return ValidateResult.PASS;
		}

		//是否是数字
		boolean isNumber = isNumber(value);


		//判断最大长度
		if (validationCheck.maxLength() > 0) {
			if (value.toString().length() > validationCheck.maxLength()) {
				return ValidateResult.NOT_PASS.setErrMsg(description + "长度大于" + validationCheck.maxLength());
			}
		}

		//判断最小长度
		if (validationCheck.minLength() > 0) {
			if (value.toString().length() < validationCheck.minLength()) {
				return ValidateResult.NOT_PASS.setErrMsg(description + "长度小于" + validationCheck.minLength());
			}
		}

		//判断指定范围
		String valueIn = validationCheck.valueIn();
		if (valueIn != null && valueIn.length() > 0) {
			boolean hasValue = false;
			for (String v : valueIn.split(CommonConstant.COMMA)) {
				if ("".equals(v)) {
					continue;
				}
				if (v.equals(value.toString())) {
					hasValue = true;
				}
			}
			if (!hasValue) {
				return ValidateResult.NOT_PASS.setErrMsg(description + "不在指定范围之内" + valueIn);
			}
		}

		//判断不在指定范围
		String valueNotIn = validationCheck.valueNotIn();
		if (valueNotIn != null && valueNotIn.length() > 0) {
			boolean hasValue = false;
			for (String v : valueNotIn.split(CommonConstant.COMMA)) {
				if (("").equals(v)) {
					continue;
				}
				if (v.equals(value.toString())) {
					hasValue = true;
				}
			}
			if (hasValue) {
				return ValidateResult.NOT_PASS.setErrMsg(description + "不允许为" + valueNotIn);
			}
		}

		//判断最小值
		String minValue = validationCheck.minValue();
		if (minValue != null && minValue.trim().length() > 0 && isNumber(minValue)) {
			if (isNumber) {
				if (Long.valueOf(value + "") < Long.valueOf(minValue)) {
					return ValidateResult.NOT_PASS.setErrMsg(description + "不能小于等于" + minValue);
				}
			}
		}


		//判断最大值
		String maxValue = validationCheck.maxValue();
		if (maxValue != null && maxValue.trim().length() > 0 && isNumber(maxValue)) {
			if (isNumber) {
				if (Long.valueOf(value + "") > Long.valueOf(maxValue)) {
					return ValidateResult.NOT_PASS.setErrMsg(description + "不能大于等于" + maxValue);
				}
			}
		}

		//正则匹配
		String regexExpression = validationCheck.regexExpression();
		if (regexExpression != null && regexExpression.length() > 0) {
			if (!value.toString().matches(regexExpression)) {
				return ValidateResult.NOT_PASS.setErrMsg(description + "不匹配表达式" + regexExpression);
			}
		}

		boolean allowChineseLanguage = validationCheck.allowChineseLanguage();
		if (!allowChineseLanguage) {
			if (value.toString().length() != value.toString().getBytes().length) {
				return ValidateResult.NOT_PASS.setErrMsg(description + "不允许使用中文" + allowChineseLanguage);
			}
		}

		return ValidateResult.PASS;
	}

	/**
	 * 是否是包装类
	 *
	 * @param cl
	 * @return
	 */
	private static boolean isBoxType(Class cl) {
		if (String.class.isAssignableFrom(cl) ||
				Byte.class.isAssignableFrom(cl) ||
				Short.class.isAssignableFrom(cl) ||
				Integer.class.isAssignableFrom(cl) ||
				Long.class.isAssignableFrom(cl) ||
				Float.class.isAssignableFrom(cl) ||
				Double.class.isAssignableFrom(cl) ||
				Character.class.isAssignableFrom(cl) ||
				Boolean.class.isAssignableFrom(cl)
				) {
			return true;

		}
		return false;
	}


	private static boolean isNumber(Object value) {
		return pattern.matcher(value + "").matches();
	}


}
