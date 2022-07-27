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
package com.wuba.wlock.repository.helper;

import java.util.List;

public class Page<T> {

	private List<T> pageList;
	private PageInfo pageInfo;

	public Page() {
	}

	public Page(int currentPage, int num, int totalNum, int pageSize, List<T> pageList) {
		this.pageList = pageList;
		this.pageInfo = this.getPageInfoByParams(currentPage, num, totalNum, pageSize);
	}

	public Page(int currentPage, int pageSize, int totalNum, List<T> pageList) {
		this.pageList = pageList;
		if (pageList == null || pageList.isEmpty()) {
			this.pageInfo = this.getPageInfoByParams(currentPage, 0, totalNum, pageSize);
			return;
		}
		this.pageInfo = this.getPageInfoByParams(currentPage, pageList.size(), totalNum, pageSize);
	}

	public Page(PageInfo pageInfo, List<T> pageList) {
		this.pageInfo = pageInfo;
		this.pageList = pageList;
	}

	public List<T> getPageList() {
		return pageList;
	}

	public void setPageList(List<T> pageList) {
		this.pageList = pageList;
	}

	public PageInfo getPageInfo() {
		return pageInfo;
	}

	public void setPageInfo(int page, int num, int totalnum, int totalPage) {
		this.pageInfo = new PageInfo(page, totalPage, num, totalnum);
	}

	public PageInfo getPageInfoByParams(int currentPage, int num, int totalNum, int pageSize) {
		int totalPage = 0;
		if (pageSize == 0) {
			pageSize = 10;
		}
		if (totalNum % pageSize == 0) {
			totalPage = totalNum / pageSize;
		} else {
			totalPage = totalNum / pageSize + 1;
		}
		return new PageInfo(currentPage, totalPage, num, totalNum);
	}

}
