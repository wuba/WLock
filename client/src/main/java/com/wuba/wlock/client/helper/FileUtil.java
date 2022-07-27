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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtil {

	private static FileWriter fWriter = null;

	private static final Log logger = LogFactory.getLog(FileUtil.class);

	public static String getRootPath() {
		File file = new File(System.getProperty("user.dir"));
		String path = file.getAbsolutePath().replace('\\', '/');
		path = path.substring(0, path.indexOf('/'));
		return path;
	}

	public static synchronized boolean createFile(String fileName, String content) {
		try {
			fWriter = new FileWriter(fileName);
			fWriter.write(content);
			return true;
		} catch (IOException e) {
			logger.warn("create file " + fileName + "failed, because:", e);
			return false;
		} finally {
			if (fWriter != null) {
				try {
					fWriter.close();
				} catch (IOException e) {
					logger.warn("close FileWriter failed, because:", e);
				}
			}
		}
	}

	public static boolean deleteFile(String fileName) {
		File file = new File(fileName);
		if (file.exists() && file.isFile()) {
			return file.delete();
		}
		return true;
	}

	public static boolean updateFile(String fileName, String content) {
		File file = new File(fileName);
		if (file.exists() && file.isFile()) {
			String newFileName = fileName + ".tmp";
			File newFile = new File(newFileName);
			if (newFile.exists()) {
				if (!newFile.delete()) {
					return false;
				}
			}
			if (!file.renameTo(newFile)) {
				return false;
			}
			boolean res = createFile(fileName, content);
			if (res) {
				newFile.delete();
			} else {
				newFile.renameTo(file);
			}
			return res;
		} else {
			return createFile(fileName, content);
		}
	}

	public static void clearUpDir(File[] files) {
		for (File file : files) {
			if (file.exists()) {
				if (file.isDirectory()) {
					clearUpDir(file.listFiles());
					file.delete();
				} else if (file.isFile()) {
					file.delete();
				}
			}
		}
	}

}
