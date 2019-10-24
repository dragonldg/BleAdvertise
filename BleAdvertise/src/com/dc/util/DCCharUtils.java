package com.dc.util;


import android.util.Log;

public class DCCharUtils {

	private static boolean isDebug = true;

	public static void showLog(String tag, String msg) {
		if (isDebug) {
			Log.i(tag, msg);
			Global.saveLogToFile(tag + ">>>>>>>" + msg + "\n\n");
		}
	}

	public static void showLogD(String tag, String msg) {
		if (isDebug) {
			Log.i(tag, msg);
			Global.saveLogToFile(tag + ">>>>>>>" + msg + "\n\n");
		}
	}

	public static void showLogI(String tag, String msg) {
		if (isDebug) {
			Log.i(tag, msg);
			Global.saveLogToFile(tag + ">>>>>>>" + msg + "\n\n");
		}
	}

	public static void showLogW(String tag, String msg) {
		if (isDebug) {
			Log.w(tag, msg);
		}
	}

	public static void showLogE(String tag, String msg) {
		if (isDebug) {
			Log.e(tag, msg);
			Global.saveLogToFile(tag + ">>>>>>>" + msg + "\n\n");
		}
	}

	public static void showLogV(String tag, String msg) {
		if (isDebug) {
			Log.v(tag, msg);
			Global.saveLogToFile(tag + ">>>>>>>" + msg + "\n\n");
		}
	}

	public static char[] StringToASCIIArray(String s) {
		if (s == null) {
			return new char[0];
		}
		return s.toCharArray();
	}

	/**
	 * @param s
	 * @return
	 */
	public static byte[] StringToByteArray(String s) {
		int sl = s.length();
		byte[] charArray = new byte[sl];
		for (int i = 0; i < sl; i++) {
			char charElement = s.charAt(i);
			charArray[i] = (byte) charElement;
		}
		return charArray;
	}

	/**
	 * @param bs
	 * @return
	 */
	public static char[] ByteToCharArray(byte[] bs) {
		int bsl = bs.length;
		char[] charArray = new char[bsl];
		for (int i = 0; i < bsl; i++) {
			charArray[i] = (char) (((char) bs[i]) & 0x00FF);
		}
		return charArray;
	}

	/**
	 * 将byte类型变量转为char类型
	 * 
	 * @param b
	 * @return
	 */
	public static char byte2char(byte b) {
		return (char) (((char) b) & 0x00FF);
	}

	/**
	 * 将16进制字符串转换为byte[]
	 * 
	 * @param bs
	 * @return
	 */
	public static byte[] hexString2ByteArray(String bs) {
		int bsLength = bs.length();
		if (bsLength % 2 != 0) {
			return null;
		}
		byte[] cs = new byte[bsLength / 2];
		String st;
		for (int i = 0; i < bsLength; i = i + 2) {
			st = bs.substring(i, i + 2);
			cs[i / 2] = (byte) Integer.parseInt(st, 16);
		}
		return cs;
	}

	/**
	 * 将byte[]转换为16进制字符串
	 * 
	 * @param b
	 * @return
	 */
	public static String bytes2HexString(byte[] b) {
		String r = "";

		for (int i = 0; i < b.length; i++) {
			String hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			r += hex.toUpperCase();
		}

		return r;
	}

	static final char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * 只针对字节数组，且长度为1或2
	 * 
	 * @param b
	 * @return
	 */
	public static int byteArrayToInt(byte[] b) {
		if (b.length == 2) {
			return b[1] & 0xFF | (b[0] & 0xFF) << 8;
		} else {
			return b[0] & 0xFF;
		}
	}

	/**
	 * 将byte数组以16进制字符显示
	 * 
	 * @param b
	 */
	public static String showResult16Str(byte[] b) {
		if (b == null) {
			return "";
		}
		String rs = "";
		int bl = b.length;
		byte bt;
		String bts = "";
		int btsl;
		for (int i = 0; i < bl; i++) {
			bt = b[i];
			bts = Integer.toHexString(bt);
			btsl = bts.length();
			if (btsl > 2) {
				bts = bts.substring(btsl - 2).toUpperCase();
			} else if (btsl == 1) {
				bts = "0" + bts.toUpperCase();
			} else {
				bts = bts.toUpperCase();
			}
			// System.out.println("i::"+i+">>>bts::"+bts);
			rs += bts;
		}
		return rs;
	}

	/**
	 * 将byte数组以16进制0x样式字符显示
	 * 
	 * @param b
	 */
	public static String showResult0xStr(byte[] b) {
		String rs = "";
		int bl = b.length;
		byte bt;
		String bts = "";
		int btsl;
		for (int i = 0; i < bl; i++) {
			bt = b[i];
			bts = Integer.toHexString(bt);
			btsl = bts.length();
			if (btsl > 2) {
				bts = "0x" + bts.substring(btsl - 2);
			} else if (btsl == 1) {
				bts = "0x0" + bts;
			} else {
				bts = "0x" + bts;
			}
			rs += (bts + " ");
		}
		return rs;
	}

	/**
	 * 整数转为2字节的16进制
	 * @param dataLength 数据长度（10进制）
	 * @return
	 */
	public static String intTo2BHexStr(int dataLength) {
		String data = Integer.toHexString(dataLength);
		switch (data.length()) {
		case 1:
			data = "000"+data;
			break;

		case 2:
			data = "00"+data;
			break;
		case 3:
			data = "0"+data;
			break;
		}
		return data;
	}
}
