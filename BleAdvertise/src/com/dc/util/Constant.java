package com.dc.util;

public class Constant {
	public static int broadTime = 800;// 广播时长,加上代码执行时间
	public static final int broadInterval = broadTime;// 广播间隔
	public static final int broadDataLen = 24;// 广播有效数据长度
	public static int broadMaxTime = 3;// 广播最大重试次数
	public static final int permenantStep = 4;// 最大并发个数，即同时广播数
	public static final int scanTimeOut = 10 * 1000;// 刷卡和输密超时时间
	public static final int broadTimeOut = 0;// 广播设置中的超时时间
	public static int scanNormalTime = 3000;// 其他操作超时时间

	public static String currentBoradData = null;// 当前发送的指令数据
	public static boolean isDataPkg = true;// 是否为数据包
	public static boolean isNeedAck = false;// 是否需要应答包（确认包）

	/**
	 * SP存储KEY
	 * @description:描述。。。
	 * @author: liudagang
	 * @date:2018-7-20
	 * @version:1.0
	 */
	public static class SpKey {
		public static final String TIME = "sp_time";
		public static final String NUMBER = "sp_number";
		public static final String DATA_LENGTH = "sp_data";
		public static final String RETRY_INTERVAL = "sp_retry_interval";	
		public static final String START_BYTES = "sp_start_bytes";	
		public static final String ADD_BYTES = "sp_add_bytes";	
		public static final String RETRY_TIMES = "sp_retry_times";	
	}
	
	/**
	 * 包类型
	 * @description:描述。。。
	 * @author: liudagang
	 * @date:2018-7-20
	 * @version:1.0
	 */
	public static class PackageType{
		/**不需要应答包+数据包 00*/
		public static final String NONEED_DATA = "00";
		/**不需要应答包+应答包 01*/
		public static final String NONEED_ACK = "01";
		/**需要应答包+数据包 10*/
		public static final String NEED_DATA = "10";
		/**需要应答包+应答包 11*/
		public static final String NEED_ACK = "11";
	}
}
