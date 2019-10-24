package com.dc.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.dc.listener.IBroadcast;
import com.dc.thread.DataRunnable;
import com.dc.util.Constant;
import com.dc.util.Constant.PackageType;
import com.dc.util.DCCharUtils;
import com.dc.util.ErrorCode;
import com.dc.util.ToastUtil;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class LeBroadcastInternal {
	private static final String TAG = "LeBraodcastInternal";
	private static BluetoothAdapter mBluetoothAdapter;// 蓝牙适配器
	private static BluetoothManager bluetoothManager;// 蓝牙管理器
	private static BluetoothLeAdvertiser mBluetoothAdvertiser;// 广播器对象
	private static ScheduledExecutorService threadPool;// 发送广播的线程池

	private static HashMap<String, String> datas;// 子包数据
	private static ArrayList<BluetoothDevice> mLeDevices;// 设备列表
	private static ArrayList<Integer> mRSSIs;// 信号强度
	private static ArrayList<byte[]> mRecords;// 广播数据

	private static int broadDataNeedNo = 0;// 要发包次数
	private static final int REQUEST_ENABLE_BT = 1;
	private static boolean isBroadcasting = false;// 是否正在广播
	private static boolean mScanning = false;// 是否正在扫描
	private static boolean isBroadSuccess = false;// 广播是否成功
	private static boolean isReceiveSuccess = false;// 广播后返回数据接收是否成功
	private static boolean initSuccess = false;// 初始化是否成功
	private static boolean isAck = false;// 是否发送的响应包
	private static int broadCurrTime = 0;// 已经重试的次数
	private static int packageSendCount = 0;// 已经发送的包的个数，数据包或者应答包
	private static int dataSendLength = 0;// 发送的所有数据的长度
	// private static int dataReceiveLength = 0;// 接收的所有数据的长度
	private static Handler mHandler;// 用来将广播消息加入队列，线性广播
	private static Context mContext;// 当前的上下文
	private static IBroadcast mBroadListener;// 回调监听器

	private static int step = 0;// 每固定跨度的执行次数，10个包4跨度执行3次，2*4+2=10

	/**
	 * sdk初始化
	 * 
	 * @param context
	 *            上下文
	 * @param broadcastListener
	 *            监听器，如果{@code broadcastListener}，必须单独调用{@link setListener()}方法
	 */
	public static void init(Context context, IBroadcast broadcastListener) {
		mContext = context;
		// 防止单独设置监听后，再次初始化，并且初始化时监听器置为了空。
		if (broadcastListener != null) {
			mBroadListener = broadcastListener;
		}
		// Initializes a Bluetooth adapter. For API level 18 and above
		// threadPool = Executors.newFixedThreadPool(4);//广播最多存在4个
		threadPool = Executors.newScheduledThreadPool(4);// 广播最多存在4个
		bluetoothManager = (BluetoothManager) context
				.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			ToastUtil.showSafeToast(context, "设备不支持蓝牙功能", Toast.LENGTH_SHORT);
			return;
		}

		// Use this check to determine whether BLE is supported on the device.
		if (!context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			ToastUtil
					.showSafeToast(context, "设备不支持低功耗蓝牙功能", Toast.LENGTH_SHORT);
			return;
		}

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			((Activity) mContext).startActivityForResult(enableBtIntent,
					REQUEST_ENABLE_BT);
		}

		// 初始化对象
		mHandler = new Handler(context.getMainLooper());
		// 获取蓝牙ble广播
		if (Integer.valueOf(Build.VERSION.SDK_INT) >= 21) {
			if (!checkBLEBroadcast())
				return;
		}

		datas = new HashMap<>();
		mLeDevices = new ArrayList<>();
		mRSSIs = new ArrayList<>();
		mRecords = new ArrayList<>();
		initSuccess = true;
	}

	/**
	 * 设置广播监听器
	 * 
	 * @param broadcastListener
	 *            广播监听
	 */
	public static void setListener(IBroadcast broadcastListener) {
		mBroadListener = broadcastListener;
	}

	/**
	 * 获取信息
	 * 
	 * @param data
	 *            指令数据
	 * @param isDataPkg
	 *            是否为数据包，true：数据包；false：确认包
	 * @param isNeedAck
	 *            是否需要确认包，true：需要确认包；false：不需要确认包
	 */
	public static void broadcastData(String data, boolean isDataPkg,
			boolean isNeedAck) {
		if (initSuccess) {
			if (isBroadcasting) {
				Log.e(TAG, "广播正在进行中，请先结束本次广播");
				return;
			}
			broadCurrTime = 0;
			isBroadcasting = true;// 定义广播正在进行
			isBroadSuccess = false;// 定义广播尚未成功
			isAck = false;// 是否应答广播
			isReceiveSuccess = false;// 广播后接收数据尚未成功
			Constant.currentBoradData = data;
			Constant.isDataPkg = isDataPkg;
			Constant.isNeedAck = isNeedAck;
			// if (isDataPkg) {// 发送的是数据包
			startAdvertiseData(data, true, isNeedAck);
			// } else {
			// isNeedAck = false;// 确认包不需要确认包
			// startAdvertiseData(data, false, isNeedAck);
			// }
		} else {
			ToastUtil.showSafeToast(mContext, "请先初始化");
		}
	}

	// （1）AdvertiseSettings.ADVERTISE_MODE_LOW_POWER 0
	// ,ADVERTISE_MODE_BALANCED 1
	// ,ADVERTISE_MODE_LOW_LATENCY 2,从左右到右，广播的间隔会越来越短
	// （2）广播分为可连接广播和不可连接广播，一般不可连接广播应用在iBeacon设备上，这样APP无法连接上iBeacon设备
	// （3）setTimeout(int timeoutMillis)设置广播的最长时间
	// 最大值为常量AdvertiseSettings.LIMITED_ADVERTISING_MAX_MILLIS
	// = 180 * 1000; 180秒,设为0时，代表无时间限制会一直广播
	// （4）setTxPowerLevel(int txPowerLevel)设置广播的信号强度
	// 常量有AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW 0,
	// ADVERTISE_TX_POWER_LOW 1, ADVERTISE_TX_POWER_MEDIUM 2,
	// ADVERTISE_TX_POWER_HIGH 3 从左到右分别表示强度越来越强.
	/**
	 * 开始发送广播数据
	 * 
	 * @param data
	 *            待发送数据
	 * @param isDataPkg
	 *            是否为数据包，true：数据包；false：确认包
	 * @param needAck
	 *            是否需要确认包，true：需要确认包；false：不需要确认包
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static void startAdvertiseData(String data, boolean isDataPkg,
			boolean isNeedAck) {// TODO
		// 发送广播
		if (Integer.valueOf(Build.VERSION.SDK_INT) < 21) {
			ToastUtil.showSafeToast(mContext, "不支持广播", Toast.LENGTH_SHORT);
		} else {
			broadDataNeedNo = 0;// 要发包次数
			packageSendCount = 0;// 已经发送的包的个数，数据包或者应答包
			broadCurrTime++;// 已经重试的次数
			final byte[] dataBytes = DCCharUtils.hexString2ByteArray(data);
			DCCharUtils.showLogE(TAG, "待广播自定义数据的长度：" + dataBytes.length);
			dataSendLength = dataBytes.length;
			if (dataSendLength > Constant.broadDataLen) {
				if (dataSendLength % Constant.broadDataLen == 0) {
					broadDataNeedNo = dataSendLength / Constant.broadDataLen;
				} else {
					broadDataNeedNo = dataSendLength / Constant.broadDataLen
							+ 1;
				}
				DCCharUtils.showLogE(TAG, "待广播自定义数据需要分包：" + broadDataNeedNo
						+ "次");
			} else {
				broadDataNeedNo = 1;
			}
			if (broadDataNeedNo == 1) {
				postAdvertiseData(new DataRunnable(mContext,
						mBluetoothAdvertiser, 0, dataBytes, createCallback(),
						true));
			} else {
				if (broadDataNeedNo <= Constant.permenantStep) {// 如果不大于4包，《=4
					for (int i = 0; i < broadDataNeedNo; i++) {
						if (i == broadDataNeedNo - 1) {
							postAdvertiseData(new DataRunnable(mContext,
									mBluetoothAdvertiser, i, dataBytes,
									createCallback(), true));
						} else {
							postAdvertiseData(new DataRunnable(mContext,
									mBluetoothAdvertiser, i, dataBytes,
									createCallback(), false));
						}
					}
				} else {// 大于4个包
					// 按照4个为步调，step不会小于2
					if (broadDataNeedNo % Constant.permenantStep != 0) {// 除不尽+1
						step = broadDataNeedNo / Constant.permenantStep + 1;
					} else {
						step = broadDataNeedNo / Constant.permenantStep;
					}

					for (int i = 0; i < Constant.permenantStep; i++) {// 0--3
						postAdvertiseData(new DataRunnable(mContext,
								mBluetoothAdvertiser, i, dataBytes,
								createCallback(), false));
					}

					for (int i = 1; i < step; i++) {
						final int tempi = i;
						mHandler.postDelayed(new Runnable() {

							@Override
							public void run() {
								if (tempi == step - 1) {
									int temp = broadDataNeedNo - (step - 1)
											* Constant.permenantStep;
									switch (temp) {
									case 1:// 4，8，12
										postAdvertiseData(new DataRunnable(
												mContext,
												mBluetoothAdvertiser,
												(step - 1)
														* Constant.permenantStep,
												dataBytes, createCallback(),
												true));
										break;
									case 2:// 4--5，8--9，12--13
										postAdvertiseData(new DataRunnable(
												mContext,
												mBluetoothAdvertiser,
												(step - 1)
														* Constant.permenantStep,
												dataBytes, createCallback(),
												false));
										postAdvertiseData(new DataRunnable(
												mContext,
												mBluetoothAdvertiser,
												(step - 1)
														* Constant.permenantStep
														+ 1, dataBytes,
												createCallback(), true));
										break;
									case 3:// 4--6，8--10，12--14
										postAdvertiseData(new DataRunnable(
												mContext,
												mBluetoothAdvertiser,
												(step - 1)
														* Constant.permenantStep,
												dataBytes, createCallback(),
												false));
										postAdvertiseData(new DataRunnable(
												mContext,
												mBluetoothAdvertiser,
												(step - 1)
														* Constant.permenantStep
														+ 1, dataBytes,
												createCallback(), false));
										postAdvertiseData(new DataRunnable(
												mContext,
												mBluetoothAdvertiser,
												(step - 1)
														* Constant.permenantStep
														+ 2, dataBytes,
												createCallback(), true));
										break;
									case 4:// 4--7，8--11，12--15
										postAdvertiseData(new DataRunnable(
												mContext,
												mBluetoothAdvertiser,
												(step - 1)
														* Constant.permenantStep,
												dataBytes, createCallback(),
												false));
										postAdvertiseData(new DataRunnable(
												mContext,
												mBluetoothAdvertiser,
												(step - 1)
														* Constant.permenantStep
														+ 1, dataBytes,
												createCallback(), false));
										postAdvertiseData(new DataRunnable(
												mContext,
												mBluetoothAdvertiser,
												(step - 1)
														* Constant.permenantStep
														+ 2, dataBytes,
												createCallback(), false));
										postAdvertiseData(new DataRunnable(
												mContext,
												mBluetoothAdvertiser,
												(step - 1)
														* Constant.permenantStep
														+ 3, dataBytes,
												createCallback(), true));
										break;
									default:
										break;
									}
								} else {// 4的倍数的包全部数据发送 4--7，8--11
									postAdvertiseData(new DataRunnable(
											mContext, mBluetoothAdvertiser,
											Constant.permenantStep * tempi,
											dataBytes, createCallback(), false));
									postAdvertiseData(new DataRunnable(
											mContext, mBluetoothAdvertiser,
											Constant.permenantStep * tempi + 1,
											dataBytes, createCallback(), false));
									postAdvertiseData(new DataRunnable(
											mContext, mBluetoothAdvertiser,
											Constant.permenantStep * tempi + 2,
											dataBytes, createCallback(), false));
									postAdvertiseData(new DataRunnable(
											mContext, mBluetoothAdvertiser,
											Constant.permenantStep * tempi + 3,
											dataBytes, createCallback(), false));
								}
							}
						}, Constant.broadInterval);// 间隔执行
					}
				}

				// 顺序发送广播
				// for (int i = 0; i < broadDataNeedNo; i++) {
				// final int count = i;
				// if (i == broadDataNeedNo - 1) {
				// mHandler.postDelayed(new Runnable() {
				//
				// @Override
				// public void run() {
				// DCCharUtils.showLogE(TAG, "最后一个包的序号:" + count);
				// postAdvertiseData(new DataRunnable(mContext,
				// mBluetoothAdvertiser, count, dataBytes,
				// createCallback(), true));
				// }
				// }, Constant.broadInterval);
				// } else {
				// mHandler.postDelayed(new Runnable() {
				//
				// @Override
				// public void run() {
				// postAdvertiseData(new DataRunnable(mContext,
				// mBluetoothAdvertiser, count, dataBytes,
				// createCallback(), false));
				// }
				// }, Constant.broadInterval);
				// }
				// }
			}
		}
	}

	/**
	 * 发送应答包，在收到POS的数据包时返回
	 * 
	 * @param dataLength
	 *            数据长度
	 * @param isNeedAck
	 *            是否需要应答包
	 */
	private static void startAdvertiseAck(int dataLength, boolean isNeedAck) {// TODO
		Constant.isDataPkg = false;
		Constant.isNeedAck = isNeedAck;
		packageSendCount = 0;// 已经发送的包的个数，数据包或者应答包
		broadDataNeedNo = 1;// 要发包次数
		// isBroadSuccess = false;// 定义广播尚未成功
		isAck = true;
		String hexStr = DCCharUtils.intTo2BHexStr(dataLength);
		byte[] dataBytes = DCCharUtils.hexString2ByteArray(hexStr);
		DCCharUtils.showLogE(TAG, "响应包数据的长度：" + dataLength);
		postAdvertiseData(new DataRunnable(mContext, mBluetoothAdvertiser, 0,
				dataBytes, createCallback(), true));
	}

	/**
	 * 检查广播支持性
	 * 
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static boolean checkBLEBroadcast() {
		mBluetoothAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
		if (mBluetoothAdvertiser == null) {
			ToastUtil
					.showSafeToast(mContext, "设备不支持蓝牙广播功能", Toast.LENGTH_SHORT);
			DCCharUtils.showLogE(TAG, "the device not support peripheral");
			return false;
		}
		return true;
	}

	/**
	 * 创建广播回调，每个广播必须使用单独唯一的回调
	 * 
	 * @return 回调对象
	 */
	private static AdvertiseCallback createCallback() {
		final AdvertiseCallback fullCallback = new AdvertiseCallback() {
			@Override
			public void onStartSuccess(AdvertiseSettings settingsInEffect) {// TODO
				super.onStartSuccess(settingsInEffect);
				packageSendCount++;
				if (settingsInEffect != null) {
					DCCharUtils.showLogE(TAG, "onStartSuccess TxPowerLv="
							+ settingsInEffect.getTimeout() + ",计数"
							+ packageSendCount);
				} else {
					DCCharUtils.showLogE(TAG,
							"onStartSuccess, settingInEffect is null");
				}
				if (isAck) {// 如果是发送响应包的回调，不需要再次监听
					scanLeDevice(false);
					return;
				}
				/**
				 * 广播完成，开始扫描
				 */
				if (packageSendCount == broadDataNeedNo
						&& broadCurrTime <= Constant.broadMaxTime
						&& !isBroadSuccess) {
					DCCharUtils.showLogE(TAG, "广播全部发送完成，开始扫描");
					isBroadcasting = false;
					if (!mScanning) {
						mHandler.postDelayed(retryRun, Constant.scanNormalTime);
						scanLeDevice(true);
					}
				} else if (!isBroadSuccess
						&& (broadCurrTime == Constant.broadMaxTime + 1)) {// 表示仅执行一次
					broadCurrTime++;// 处理多次执行的问题
					isBroadcasting = false;
					ToastUtil.showSafeToast(mContext, "重试3次仍然没成功");
					DCCharUtils.showLogE(TAG, "重试3次仍然没成功");
					mBroadListener.onTimeout();
				}
			}

			@Override
			public void onStartFailure(int errorCode) {
				super.onStartFailure(errorCode);
				packageSendCount++;
				if (packageSendCount == broadDataNeedNo) {
					isBroadcasting = false;
				}
				DCCharUtils.showLogE(TAG, "onStartFailure errorCode="
						+ errorCode);
				if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE) {
					ToastUtil.showSafeToast(mContext, "广播数据太大",
							Toast.LENGTH_LONG);
					DCCharUtils
							.showLogE(
									TAG,
									"Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.");
				} else if (errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
					ToastUtil.showSafeToast(mContext, "广播个数太多",
							Toast.LENGTH_LONG);
					DCCharUtils
							.showLogE(TAG,
									"Failed to start advertising because no advertising instance is available.");
				} else if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
					ToastUtil.showSafeToast(mContext, "广播已经启动",
							Toast.LENGTH_LONG);
					DCCharUtils
							.showLogE(TAG,
									"Failed to start advertising as the advertising is already started");
				} else if (errorCode == ADVERTISE_FAILED_INTERNAL_ERROR) {
					ToastUtil.showSafeToast(mContext, "广播错误，请重启蓝牙",
							Toast.LENGTH_LONG);
					DCCharUtils.showLogE(TAG,
							"Operation failed due to an internal error");
				} else if (errorCode == ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
					ToastUtil.showSafeToast(mContext, "广播的特性不支持",
							Toast.LENGTH_LONG);
					DCCharUtils.showLogE(TAG,
							"This feature is not supported on this platform");
				}
			}
		};
		return fullCallback;
	}

	private static Runnable retryRun = new Runnable() {

		@Override
		public void run() {
			if (!isBroadSuccess) {// 广播成功就不再二次发送广播
				DCCharUtils.showLogE(TAG, "广播全部发送完成，未扫描到数据，重新广播数据");
				scanLeDevice(false);
				startAdvertiseData(Constant.currentBoradData,
						Constant.isDataPkg, Constant.isNeedAck);
			} else {
				DCCharUtils.showLogE(TAG, "广播全部发送完成，不再发送广播");
			}
		}
	};

	private static Runnable waitRun = new Runnable() {

		@Override
		public void run() {
			if (!isReceiveSuccess) {
				DCCharUtils.showLogE(TAG, "没有收到确认包后面的数据包");
				scanLeDevice(false);// 停止扫描
				mBroadListener.onTimeout();
			} else {
				DCCharUtils.showLogE(TAG, "已经收到确认后面的数据包");
			}
		}
	};

	/**
	 * 扫描设备，利用该方法获取广播数据
	 * 
	 * @param enable
	 *            true:扫描,false:停止扫描
	 */
	@SuppressWarnings({ "deprecation" })
	private static void scanLeDevice(final boolean enable) {
		if (enable && !mScanning) {
			mScanning = true;
			clearDevice();// 重新扫描时要清空设备、数据、信号等数据
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}

	/**
	 * 扫描回调
	 */
	private static BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,
				final byte[] scanRecord) {
			((Activity) mContext).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (!mScanning) {
						return;
					}
					// DCCharUtils.showLogE(TAG, "扫描响应数据中。。。");// TODO
					// broadCurrTime = 0;
					byte[] head02 = new byte[2];
					System.arraycopy(scanRecord, 0, head02, 0, 2);
					byte[] mani4 = new byte[1];
					System.arraycopy(scanRecord, 4, mani4, 0, 1);
					byte[] pcbTag5 = new byte[1];
					System.arraycopy(scanRecord, 5, pcbTag5, 0, 1);
					String pcbStr5 = DCCharUtils.bytes2HexString(pcbTag5)
							.substring(0, 1);

					if (DCCharUtils.bytes2HexString(head02).equals("0201")
							&& (DCCharUtils.bytes2HexString(mani4).equals("FF") || DCCharUtils
									.bytes2HexString(mani4).equals("ff"))
							&& (pcbStr5.contains("C") || pcbStr5.contains("c")
									|| pcbStr5.contains("D") || pcbStr5
										.contains("d"))) {

						DCCharUtils.showLogE(
								TAG,
								"所有收到的数据："
										+ DCCharUtils
												.bytes2HexString(scanRecord));
						// 是否为数据包，是否需要响应包
						// byte[] paraTag = new byte[1];
						// System.arraycopy(scanRecord, 6, paraTag, 0, 1);
						// String paraStr =
						// DCCharUtils.bytes2HexString(paraTag);
						// if (paraStr.equals(PackageType.NEED_DATA) ||
						// paraStr.equals(PackageType.NONEED_DATA))
						// {//不将应答包返回给上层app
						mBroadListener
								.onBroadcastData(device, rssi, scanRecord);
						// }
						addDevice(device, rssi, scanRecord, true);
					}
				}
			});
		}
	};

	/**
	 * 添加设备
	 * 
	 * @param device
	 * @param rssi
	 * @param scanRecord
	 * @param filter
	 */
	private static void addDevice(BluetoothDevice device, int rssi,
			byte[] scanRecord, boolean filter) {// TODO
		if (filter) {
			// 是否继续发送和包序号
			byte[] pacNumBytes = new byte[1];
			System.arraycopy(scanRecord, 5, pacNumBytes, 0, 1);
			String stopStr = DCCharUtils.bytes2HexString(pacNumBytes)
					.substring(0, 1);
			String numStr = DCCharUtils.bytes2HexString(pacNumBytes).substring(
					1, 2);
			// 数据长度
			byte[] dataLenBytes = new byte[1];
			System.arraycopy(scanRecord, 3, dataLenBytes, 0, 1);
			int dataLen = DCCharUtils.byteArrayToInt(dataLenBytes);
			int realLen = dataLen - 3;

			// 数据内容
			byte[] dataBytes = new byte[realLen];
			System.arraycopy(scanRecord, 7, dataBytes, 0, realLen);
			String dataHex = DCCharUtils.bytes2HexString(dataBytes);

			// 是否为数据包，是否需要响应包
			byte[] paraTag = new byte[1];
			System.arraycopy(scanRecord, 6, paraTag, 0, 1);
			String paraStr = DCCharUtils.bytes2HexString(paraTag);

			DCCharUtils.showLogE(TAG, "判断返回数据是否需要确认包,"
					+ ((paraStr.equals("10") || paraStr.equals("11")) ? "需要确认包"
							: "不需要确认包"));
			if (Constant.isNeedAck) {// 手机端需要响应包
				if (paraStr.equals("01")) {// 处理POS端返回的响应包，POS端不需要确认包+是确认包
					ToastUtil.showSafeToast(mContext, "收到POS应答包");
					DCCharUtils.showLogE(TAG,
							"发送数据长度：" + dataSendLength + "-----" + "POS收到的长度"
									+ Integer.valueOf(dataHex, 16));
					if (dataSendLength != Integer.valueOf(dataHex, 16)) {
						scanLeDevice(false);
						mBroadListener
								.onError(ErrorCode.RECEIVE_DATA_LENGTH_ERROR);
						return;
					}
					DCCharUtils.showLogE(TAG, "手机端需要确认包，数据广播完成");
					isBroadSuccess = true;
					mHandler.removeCallbacks(retryRun);
					mBroadListener.onWaiting();// 等待真正的数据包
					mHandler.postDelayed(waitRun, Constant.scanTimeOut);
				}
				if (paraStr.equals(PackageType.NEED_DATA)
						|| paraStr.equals(PackageType.NONEED_DATA)) {// 处理POS端返回的数据包，
					// 需要确认包+数据包/不需要确认包+数据包
					DCCharUtils.showLogE(TAG, "对收到的数据进行处理if");
					trimData(device, rssi, scanRecord, stopStr, numStr,
							pacNumBytes, dataHex, paraStr);
				}
			} else {// 手机端不需要响应包，此时接收的是数据包
				ToastUtil.showSafeToast(mContext, "手机端不需要确认包，数据广播完成");
				isBroadSuccess = true;
				if (paraStr.equals(PackageType.NEED_DATA)
						|| paraStr.equals(PackageType.NONEED_DATA)) {
					DCCharUtils.showLogE(TAG, "对收到的数据进行处理else");
					mHandler.removeCallbacks(retryRun);
					trimData(device, rssi, scanRecord, stopStr, numStr,
							pacNumBytes, dataHex, paraStr);
				}
			}

		} else {
			mLeDevices.add(device);
			mRSSIs.add(rssi);
			mRecords.add(scanRecord);
		}
	}

	/**
	 * 处理返回的数据包，数据包需要手机返回确认包
	 * 
	 * @param device
	 * @param rssi
	 * @param scanRecord
	 * @param stopStr
	 * @param numStr
	 * @param pacNumBytes
	 * @param dataHex
	 */
	private static void trimData(BluetoothDevice device, int rssi,
			byte[] scanRecord, String stopStr, String numStr,
			byte[] pacNumBytes, String dataHex, String paraStr) {// TODO
		if (mLeDevices == null || mLeDevices.size() == 0 || datas.size() == 0) {
			mLeDevices.add(device);
			mRSSIs.add(rssi);
			mRecords.add(scanRecord);
			datas.put(DCCharUtils.bytes2HexString(pacNumBytes), dataHex);
		} else {
			if (!datas.containsKey(DCCharUtils.bytes2HexString(pacNumBytes))) {
				mLeDevices.add(device);
				mRSSIs.add(rssi);
				mRecords.add(scanRecord);
				datas.put(DCCharUtils.bytes2HexString(pacNumBytes), dataHex);
			}
		}
		if ((stopStr.contains("c") || stopStr.contains("C"))
				&& Integer.valueOf(numStr, 16) == (datas.size() - 1)) {
			DCCharUtils.showLogE(TAG, "接收数据完成，正在解析中");
			scanLeDevice(false);
			isReceiveSuccess = true;
			mHandler.removeCallbacks(waitRun);
			List<Map.Entry<String, String>> list = new ArrayList<>(
					datas.entrySet());
			Collections.sort(list, new Comparator<Map.Entry<String, String>>() {

				@Override
				public int compare(Entry<String, String> lhs,
						Entry<String, String> rhs) {
					return lhs.getKey().compareTo(rhs.getKey());
				}
			});
			StringBuilder sb = new StringBuilder();
			for (Entry<String, String> entry : list) {
				DCCharUtils.showLogE(TAG, "过滤后收到的数据：" + entry.getKey() + "=="
						+ entry.getValue() + "--" + paraStr);
			}

			for (int i = 1; i < list.size(); i++) {
				sb.append(list.get(i).getValue());
			}
			sb.append(list.get(0).getValue());
			mBroadListener.onAllData(sb.toString());
			if (paraStr.equals(PackageType.NEED_DATA)) {
				DCCharUtils.showLogE(TAG, "接收数据完成，解析完成，并发送应答包");
				ToastUtil.showSafeToast(mContext, "发送应答包");
				isBroadcasting = false;
				startAdvertiseAck(sb.toString().length() / 2, false);
			}
		}
	}

	private static void postAdvertiseData(Runnable runnable) {
		// ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(runnable);
		threadPool.execute(runnable);
		// threadPool.scheduleAtFixedRate(runnable, 0, 30,
		// TimeUnit.MILLISECONDS);

		// threadPool.execute(new DataRunnable(DataType.DEVICE, requestData,
		// RequestUrl.DEVICE_START));

		// 核心线程是固定的，非核心线程没有限制，非核心线程闲置时会被回收。会创建一个定长线程池，执行定时任务和固定周期的任务。
		// ScheduledExecutorService scheduledThereadPool = Executors
		// .newScheduledThreadPool(3);
		// scheduledThereadPool.execute(runnable);
		// scheduledThereadPool.schedule(runnable, 0, TimeUnit.SECONDS);//
		// 2000ms后执行。
		// 该模式全部由核心线程去实现，并不会被回收，没有超时限制和任务队列的限制，会创建一个定长线程池，可控制线程最大并发数，超出的线程会在队列中等待。
		// ExecutorService fixedThreadPool = Executors.newFixedThreadPool(4);
		// fixedThreadPool.execute(runnable);
	}

	private static void clearDevice() {
		if (datas != null) {
			datas.clear();
		}
		if (mLeDevices != null) {
			mLeDevices.clear();
		}
		if (mRSSIs != null) {
			mRSSIs.clear();
		}
		if (mRecords != null) {
			mRecords.clear();
		}
	}
}
