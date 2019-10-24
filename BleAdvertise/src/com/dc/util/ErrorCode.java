package com.dc.util;

/**
 * 错误码
 * @description:描述。。。
 * @author: liudagang
 * @date:2018-7-19
 * @version:1.0
 */
public class ErrorCode{
	/**
	 * 获取到的数据包类型错误，应答包和数据包
	 */
	public static final int RECEIVE_DATA_TYPE_ERROR = 0x00;
	
	/**
	 * 获取到的应答数据包中原数据长度错误
	 */
	public static final int RECEIVE_DATA_LENGTH_ERROR = 0x01;
	
}
