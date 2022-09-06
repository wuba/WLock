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
package com.wuba.wlock.registry.admin.constant;

public class ExceptionConstant {

	public static final String KEY_EXIST = "密钥名称已经存在";

	public static final String KEY_NOT_EXIST = "密钥不存在";

	public static final String CLUSTER_NOT_EXIST = "集群不存在";

	public static final String APPLY_KEY_ERROR = "申请密钥失败";

	public static final String APPLY_ONLINE_KEY_ERROR = "申请上线密钥失败";
	public static final String APPLY_QPS_ERROR = "申请发送量调整失败";
	public static final String APPLY_OFFLINE_KEY_ERROR = "申请下线密钥失败";

	public static final String APPROVE_ONLINE_KEY_ERROR = "审批上线密钥失败";
	public static final String APPROVE_QPS_ERROR = "审批发送量调整失败";
	public static final String ORDER_QUERY_ERROR= "查询工单失败";

	public static final String ORDER_NOTEXIST = "工单不存在";
	public static final String USER_HAS_NO_AUTHROITY = "用户没有操作权限";

	public static final String SERVER_EXCEPTION = "服务端异常";

	public static final String PARAMS_EXCEPTION = "参数异常";

	public static final String CLUSTER_EXIST_RESOURCES = "请先删除群下的server节点";

	public static final String CLUSTER_NAME_EXISTED = "集群名已存在";

	public static final String SERVER_EXISTED = "server节点已存在";

	public static final String SERVER_UN_EXISTED = "server节点不存在";

	public static final String SERVER_SEQID_EXIST = "当前序列ID已存在";
	
	public static final String DELETE_SUCCESS = "删除成功";

	public static final String DELETE_FAILED = "删除失败";

	public static final String UPDATE_SUCCESS = "修改成功";

	public static final String UPDATE_FAILED = "修改失败";

	public static final String ADD_SUCCESS = "添加成功";

	public static final String QUICK_INIT_SUCCESS = "快速初始化成功";
	
	public static final String ADD_FAILED = "添加失败";

	public static final String ONLINE_DELETE_ERROR = "上线状态无法删除,请先下线";
	
	public static final String ONLINE_SUCCESS = "上线成功";
	
	public static final String OFFLINE_SUCCESS = "下线成功";

	public static final String ROLLBACK_SUCCESS = "回滚成功";

	public static final String OPERATE_SUCCESS = "操作成功";

	public static final String ONLINE_SERVER_ERROR = "上线后server数量小于3个";
	
	public static final String OFFLINE_SERVER_ERROR = "下线后server数量小于3个";
	
	public static final String NO_PERMISSION = "您没有操作权限";

	public static final String STARTTIME_BIGGER_ENDTIME = "查询起始时间不得大于截止时间";

	public static final String QUERY_KEY_ERROR = "查询密钥失败";
	
	public static final String NO_PERMISSION_OF_KEY = "您无该秘钥权限";
	
	public static final String ONLINE_KEY_NO_DELETE = "线上秘钥无法删除,请提交下线秘钥工单";

	public static final String ONLINE_KEY_EXIST = "线上秘钥存在,无法删除";

	public static final String QUERY_TIME_LIMIT_ERROR = "锁操作记录查询不能超过30分钟";

	public static final String DATE_TO_TIMESTAMP_ERROR = "日期格式转换为时间戳失败";

	public static final String LOCK_KEY_CLUSTER_ERROR = "秘钥不属于该集群,请确认秘钥归属";

	public static final String KEY_GROUP_ERROR = "秘钥不属于该分组,不能进行迁移";

	public static final String MIGRATE_STATE_LIMIT_ONLINE_OFFLINE = "迁移过程中不允许节点上下线";

	public static final String KEY_MIGRATE_EMPTY = "没有要处理的迁移数据,请检查参数是否准确";

	public static final String KEY_MIGRATE_WAIT = "安全迁移状态需要保证处于 10min , 才能继续进行操作,当前时间未到操作时间,请等待.";

	public static final String KEY_MIGRATE_PROCESS_LIMIT = "非回滚完成状态不能重新开启迁移";

	public static final String ROLLBACK_MIGRATE_PROCESS_LIMIT = "非秘钥迁移状态不允许回滚操作哦";

	public static final String VERSION_MIGRATE_PROCESS_LIMIT = "查询迁移记录,版本错误";

	public static final String MIGRATE_GROUP_NODE_CHANGE_COUNT_CHECK = "单次变更节点数量超过 1 个或者没有变更";

	public static final String MIGRATE_GROUP_NODE_CHANGE_TYPE_CHECK = "不允许操作老节点";

	public static final String MIGRATE_GROUP_NODE_ADD_CHECK = "秘钥迁移没有执行完成,不允许进行节点添加操作";

	public static final String MIGRATE_CLUSTER_SPLIT_NODE_LIST_EMPTY = "新集群节点列表不能为空";

	public static final String MIGRATE_CLUSTER_SPLIT_MUST_NEW_CLUSTER = "拆分集群只能拆分到新集群";


}

