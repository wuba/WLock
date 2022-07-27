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
package com.wuba.wlock.common.entity;

public class VersionMessage {
	/**
	 * IP + port
	 */
	private String addr;
	/**
	 * 版本
	 */
	private String ver;
	/**
	 * 语言
	 */
	private String lang;
	/**
	 * 更新时间
	 */
	private long date;
	/**
	 * hashcode
	 */
	private String key;
	/**
	 * 秘钥名
	 */
	private String keyName;
	
	public VersionMessage() {
	}
	
	public VersionMessage(String key, String ver, String lang, String addr, String keyName) {
		this.addr = addr;
		this.ver = ver;
		this.lang = lang;
		this.key = key;
		this.date = System.currentTimeMillis();
		this.keyName = keyName;
	}
	
	public String getAddr() {
		return addr;
	}
	
	public void setAddr(String addr) {
		this.addr = addr;
	}
	
	public String getVer() {
		return ver;
	}
	
	public void setVer(String ver) {
		this.ver = ver;
	}
	
	public String getLang() {
		return lang;
	}
	
	public void setLang(String lang) {
		this.lang = lang;
	}
	
	public long getDate() {
		return date;
	}
	
	public void setDate(long date) {
		this.date = date;
	}
	
	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public boolean versionExist(String version) {
		if (version != null && !version.isEmpty()) {
			if (this.getVer().contains(version)) {
				return true;
			}
			return false;
		}
		return true;
	}
	
	public boolean ipExist(String ip) {
		if (ip != null && !ip.isEmpty()) {
			if (this.getAddr().contains(ip)) {
				return true;
			}
			return false;
		}
		return true;
	}
	
	public boolean languageExist(String lang) {
		if (lang != null && !lang.isEmpty()) {
			if (this.getLang().contains(lang)) {
				return true;
			}
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return "VersionMessage [addr=" + addr + ", ver=" + ver + ", lang=" + lang + ", date=" + date + ", key=" + key + "]";
	}

	public String getKeyName() {
		return keyName;
	}

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

}
