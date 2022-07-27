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
package com.wuba.wlock.client.helper;

import com.wuba.wlock.client.registryclient.entity.ClientKeyEntity;

import javax.xml.transform.TransformerException;
import java.io.File;

public class FileManager {

	private static class FileManagerHolder {
		private static final FileManager instance = new FileManager();
	}

	public static FileManager getInstance() {
		return FileManagerHolder.instance;
	}

	public synchronized boolean saveConfigs2Files(ClientKeyEntity clientKey) throws TransformerException {
		boolean flag = false;
		String keyPath = PathUtil.getFilePath(clientKey.getKey());
		File subPath = new File(keyPath);
		if (!subPath.exists() || !subPath.isDirectory()) {
			subPath.mkdirs();
		} else {
			FileUtil.clearUpDir(subPath.listFiles());
		}

		String clusterContent = new XmlParser().createClusterXml(clientKey);
		saveClusters2File(keyPath + "/clusters.xml", clusterContent);
		
		return flag;
	}

	private boolean saveClusters2File(String path, String content) {
		File file = new File(path);
		if (!file.exists() || !file.isDirectory()) {
			return FileUtil.createFile(path, content);
		} else {
			return FileUtil.updateFile(path, content);
		}
	}

	public String[] getClustersFile(String dirPath) {
		File dir = new File(dirPath);
		if (dir.exists() && dir.isDirectory()) {
			return dir.list();
		}
		return null;
	}

	public synchronized void deleteFiles(String lockKey) {
		String keyPath = PathUtil.getFilePath(lockKey);

		File subPath = new File(keyPath);
		if (!subPath.exists() || !subPath.isDirectory()) {
			return;
		} else {
			FileUtil.clearUpDir(subPath.listFiles());
		}
	}

}
