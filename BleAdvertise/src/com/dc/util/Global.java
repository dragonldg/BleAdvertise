package com.dc.util;

import android.annotation.SuppressLint;
import android.os.Environment;

import java.io.*;

/**
 * 公共方法类
 */

@SuppressLint("DefaultLocale")
public class Global {
	private static Global m_Global;

	private Global() {
	}

	public static Global getInstance() {
		if (m_Global == null)
			m_Global = new Global();
		return m_Global;
	}

	/**
	 * 判断字符串是否为空
	 */
	public static boolean isEmpty(String str) {
		if (str == null || "".equals(str)) {
			return true;
		}
		return false;
	}

	public static String strLenToHexString(String content) {
		return strLenToHexString(content, 2);
	}

	/**
	 * 将int装换成16进制
	 * 
	 * @param content
	 *            装换的内容
	 * @param len
	 *            转换后的长度
	 */
	public static String strLenToHexString(String content, int len) {
		int contentLen = content.length() / 2;
		String hex = Integer.toHexString(contentLen).toUpperCase();
		if (len > hex.length()) {
			int hexLen = hex.length();
			for (int i = 0; i < len - hexLen; i++) {
				hex = "0" + hex;
			}
		}
		return hex;
	}

	/**
	 * 保存log文件到本地路径
	 */
	public static void saveLogToFile(final String log) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					File dir = new File(getSDPath() + File.separator + "Sand");
					// Log.i(TAG, dir.getPath());
					isExist(dir.getPath());
					File file = new File(dir, "log");
					// Log.i(TAG, file.getPath());
					if (file.length() >= 1024 * 1024 * 5) {// 如果log大于5兆那么就删除文件重新写入
						file.delete();
					}
					if (!file.exists()) {
						file.createNewFile();
					}
					OutputStreamWriter write = new OutputStreamWriter(
							new FileOutputStream(file, true), "gbk");
					write.write(log);
					write.flush();
					write.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();

	}

	public static String getSDPath() {
		String sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory().getPath();// 获取跟目录
		}

		return sdDir;
	}

	/**
	 * @param path
	 *            文件夹路径
	 */
	public static void isExist(String path) {
		File file = new File(path);
		// 判断文件夹是否存在,如果不存在则创建文件夹
		if (!file.exists()) {
			file.mkdir();
		}
	}

	/**
	 * 将int装换成len的bcd进制
	 * 
	 * @param content
	 *            装换的内容
	 * @param len
	 *            转换后的长度
	 */
	public static String strLenToBCDString(String content, int len) {
		String contentLen = content.length() + "";
		if (len > contentLen.length()) {
			int hexLen = contentLen.length();
			for (int i = 0; i < len - hexLen; i++) {
				contentLen = "0" + contentLen;
			}
		}
		return contentLen;
	}
}
