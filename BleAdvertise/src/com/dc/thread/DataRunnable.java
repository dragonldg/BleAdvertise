package com.dc.thread;

import com.dc.util.Constant;
import com.dc.util.DCCharUtils;

import android.annotation.TargetApi;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Build;
import android.util.Log;

//考虑将数据提前分包，然后发送，效率更好
//此处是每次发送都从全包中抓取相应的数据打包发送。
public class DataRunnable implements Runnable {
	private static final String TAG = "DataRunnable";
	private BluetoothLeAdvertiser mBluetoothAdvertiser;// 广播器对象
	private int pcbCount;// 当前发送的包序号
	private byte[] dataBytes;// 数据对象
	private AdvertiseCallback callback;// 回调对象
	private boolean isLastData = false;// 是否为最后一个包
	@SuppressWarnings("unused")
	private Context context;// 预留

	/**
	 * 
	 * @param mBluetoothAdvertiser
	 *            广播器
	 * @param pcbCount
	 *            已经发从次数
	 * @param dataBytes
	 *            字节数据
	 * @param callback
	 *            回调
	 * @param isLastData
	 *            是否最后的数据
	 */
	public DataRunnable(Context context,
			BluetoothLeAdvertiser mBluetoothAdvertiser, int pcbCount,
			byte[] dataBytes, AdvertiseCallback callback, boolean isLastData) {
		super();
		this.context = context;
		this.mBluetoothAdvertiser = mBluetoothAdvertiser;
		this.pcbCount = pcbCount;
		this.dataBytes = dataBytes;
		this.callback = callback;
		this.isLastData = isLastData;
	}

	@Override
	public void run() {
		final AdvertiseCallback tempCallback = callback;
		if (isLastData) {
			sendLastData(pcbCount, dataBytes, tempCallback);
		} else {
			sendFullData(pcbCount, dataBytes, tempCallback);
		}
	}

	/**
	 * 发送广播数据
	 * 
	 * @param pcbCount
	 *            发送的包序号
	 * @param dataBytes
	 *            待发送数据
	 * @param callback
	 *            广播回调
	 */
	private void sendFullData(int pcbCount, byte[] dataBytes,
			AdvertiseCallback callback) {
		int broadContinueTag = 13;// 继续发送包 1101
		int bdNoInt = pcbCount | (broadContinueTag << 4);
		byte[] needbroadData = new byte[Constant.broadDataLen + 1];
		String bdNoStr = Integer.toHexString(bdNoInt);
		if (bdNoStr.length() == 1) {
			bdNoStr = "0" + bdNoStr;
		}
		System.arraycopy(DCCharUtils.hexString2ByteArray(bdNoStr), 0,
				needbroadData, 0, 1);
		System.arraycopy(dataBytes, pcbCount * Constant.broadDataLen,
				needbroadData, 1, Constant.broadDataLen);
		mBluetoothAdvertiser.startAdvertising(
				createAdvSettings(true, Constant.broadTimeOut),
				createScanAdvertiseData(needbroadData), callback);
		try {
			Thread.sleep(Constant.broadTime);// 广播时间
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		stopAdvertise(callback);
	}

	/**
	 * 发送广播数据
	 * 
	 * @param pcbCount
	 *            发送的包序号
	 * @param dataBytes
	 *            待发送数据
	 * @param callback
	 *            广播回调
	 */
	private void sendLastData(int pcbCount, byte[] dataBytes,
			AdvertiseCallback callback) {
		int broadEndTag = 12;// 发送完成包 1100
		int bdNoInt = pcbCount | (broadEndTag << 4);
		byte[] needbroadData = new byte[dataBytes.length - pcbCount
				* Constant.broadDataLen + 1];
		String bdNoStr = Integer.toHexString(bdNoInt);
		if (bdNoStr.length() == 1) {
			bdNoStr = "0" + bdNoStr;
		}
		System.arraycopy(DCCharUtils.hexString2ByteArray(bdNoStr), 0,
				needbroadData, 0, 1);
		System.arraycopy(dataBytes, pcbCount * Constant.broadDataLen,
				needbroadData, 1, dataBytes.length - pcbCount
						* Constant.broadDataLen);
		mBluetoothAdvertiser.startAdvertising(
				createAdvSettings(true, Constant.broadTimeOut),//设置广播超时时间
				createScanAdvertiseData(needbroadData), callback);
		try {
			Thread.sleep(Constant.broadTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		stopAdvertise(callback);
	}

	/**
	 * 设置scan广播数据
	 * 
	 * @param data
	 *            待广播发送的数据
	 * @param isBroad
	 *            是否广播数据，false:应答数据
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public AdvertiseData createScanAdvertiseData(byte[] data) {// TODO
		if (Integer.valueOf(Build.VERSION.SDK_INT) < 21) {
			return null;
		} else {
			byte[] maniData;// 厂商数据
			if (Constant.isDataPkg) {
				if (Constant.isNeedAck) {
					maniData = new byte[] { 0x10 };// 需要+数据包
				} else {
					maniData = new byte[] { 0x00 };// 不需要+数据包
				}
			} else {
				if (Constant.isNeedAck) {
					maniData = new byte[] { 0x11 };// 需要+响应包
				} else {
					maniData = new byte[] { 0x01 };// 不需要+响应包
				}
			}
			AdvertiseData.Builder mDataBuilder = new AdvertiseData.Builder();
			mDataBuilder.setIncludeDeviceName(false); // 不包含广播名称
			mDataBuilder.setIncludeTxPowerLevel(false);// 不包含发射功率信息
			/** 有效利用有效字节，将厂商ID字节腾出来给自定义数据 */
			byte[] maniReplace = new byte[2];
			if (data.length > 2) {
				byte[] dataReplace = new byte[data.length - 1];
				System.arraycopy(maniData, 0, maniReplace, 0, 1);
				System.arraycopy(data, 0, maniReplace, 1, 1);
				System.arraycopy(data, 1, dataReplace, 0, data.length - 1);
				mDataBuilder.addManufacturerData(
						DCCharUtils.byteArrayToInt(maniReplace), dataReplace);
			} else if (data.length == 2) {
				byte[] dataReplace = new byte[1];
				System.arraycopy(maniData, 0, maniReplace, 0, 1);
				System.arraycopy(data, 0, maniReplace, 1, 1);
				System.arraycopy(data, 1, dataReplace, 0, 1);
				mDataBuilder.addManufacturerData(
						DCCharUtils.byteArrayToInt(maniReplace), dataReplace);
			} else if (data.length == 1) {
				byte[] dataReplace = new byte[] { 0x00 };
				System.arraycopy(data, 0, maniReplace, 1, 1);
				System.arraycopy(maniData, 0, maniReplace, 0, 1);
				mDataBuilder.addManufacturerData(
						DCCharUtils.byteArrayToInt(maniReplace), dataReplace);
			}
			DCCharUtils.showLogE(TAG,
					"发送的数据：" + DCCharUtils.bytes2HexString(data) + "--"
							+ DCCharUtils.bytes2HexString(maniReplace));
			AdvertiseData mAdvertiseData = mDataBuilder.build();
			if (mAdvertiseData == null) {
				Log.e(TAG, "mAdvertiseSettings == null");
			}
			return mAdvertiseData;
		}
	}

	/**
	 * 设置scan响应数据
	 * 
	 * @param data
	 * @return
	 */
	@SuppressWarnings("unused")
	private AdvertiseData createScanResonseData() {
		AdvertiseData.Builder builder = new AdvertiseData.Builder();
		builder.setIncludeDeviceName(true);
		byte[] serverData = new byte[] { 0x11, 0x22, 0x33, 0x44, 0x55, 0x66,
				0x77 };
		builder.addManufacturerData(15, serverData);
		AdvertiseData adv = builder.build();
		return adv;
	}

	/**
	 * 广播的一些基本设置
	 * 
	 * @param connectAble
	 *            是否可连接
	 * @param timeoutMillis
	 *            广播超时时间
	 * @return 设置对象
	 */
	public AdvertiseSettings createAdvSettings(boolean connectAble,
			int timeoutMillis) {
		AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
		builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
		builder.setConnectable(connectAble);
		builder.setTimeout(timeoutMillis);
		builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
		AdvertiseSettings mAdvertiseSettings = builder.build();
		if (mAdvertiseSettings == null) {
			Log.e(TAG, "mAdvertiseSettings == null");
			return null;
		}
		return mAdvertiseSettings;
	}

	/**
	 * 停止发送广播
	 * 
	 * @param callback
	 *            要求和发送广播的回调对象为同一个，否则会异常
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void stopAdvertise(AdvertiseCallback callback) {
		if (Integer.valueOf(Build.VERSION.SDK_INT) < 21) {
			Log.e(TAG, "手机系统不支持广播");
		} else {
			if (mBluetoothAdvertiser != null) {
				mBluetoothAdvertiser.stopAdvertising(callback);
			} else {
				Log.e(TAG, "已经停止");
			}
		}
	}
}
