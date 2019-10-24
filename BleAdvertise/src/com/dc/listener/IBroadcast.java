package com.dc.listener;

import android.bluetooth.BluetoothDevice;

public interface IBroadcast {
	/**
	 * 超时回调
	 */
	public void onTimeout();

	/**
	 * 实时获取其他设备广播的数据，包含设备信息、信号强度信息、数据信息
	 * 
	 * @param deviceList
	 *            所有的设备信息
	 * @param rssi
	 *            信号强度信息
	 * @param scanRecord
	 *            数据记录信息
	 */
	public void onBroadcastData(BluetoothDevice device, int rssi,
			byte[] scanRecord);

	/**
	 * 获取所有的数据信息
	 * 
	 * @param data
	 *            广播发送的数据
	 */
	public void onAllData(String data);
	
	/**
	 * 等待回调，主要是等待用户刷卡或者输密码等操作
	 */
	public void onWaiting();
	
	/**
	 * 错误信息
	 * @param errCode 错误码，参考ErrorCode.java类
	 */
	public void onError(int errCode);
	
	
}
