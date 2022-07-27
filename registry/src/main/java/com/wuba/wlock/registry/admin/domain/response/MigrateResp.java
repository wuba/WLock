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
package com.wuba.wlock.registry.admin.domain.response;


public class MigrateResp {

	private long id;
	private String server;
	private int groupId;
	private int migrate;
	private int executeResult;
	private int isEnd;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public int getMigrate() {
		return migrate;
	}

	public void setMigrate(int migrate) {
		this.migrate = migrate;
	}

	public int getExecuteResult() {
		return executeResult;
	}

	public void setExecuteResult(int executeResult) {
		this.executeResult = executeResult;
	}

	public int getIsEnd() {
		return isEnd;
	}

	public void setIsEnd(int isEnd) {
		this.isEnd = isEnd;
	}

	@Override
	public String toString() {
		return "MigrateView{" +
				"id=" + id +
				", server='" + server + '\'' +
				", groupId=" + groupId +
				", migrate=" + migrate +
				", executeResult=" + executeResult +
				", isEnd=" + isEnd +
				'}';
	}
}
