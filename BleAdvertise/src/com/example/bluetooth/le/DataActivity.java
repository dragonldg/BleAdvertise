/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluetooth.le;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dc.adapter.LeDeviceListAdapter;
import com.dc.bluetooth.le.R;
import com.dc.internal.LeBroadcastCtrl;
import com.dc.listener.IBroadcast;
import com.dc.util.Constant;
import com.dc.util.DCCharUtils;
import com.dc.util.ErrorCode;
import com.dc.util.SpUtil;
import com.dc.util.ToastUtil;
import com.dc.util.Constant.SpKey;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DataActivity extends Activity implements OnClickListener {

	private static long lastTime = System.currentTimeMillis();
	private static final String TAG = "DataActivity";
	private EditText ble_msg;
	private TextView tv_statistics, tv_data_sta;// 统计用
	private ListView ble_device_list;
	private LeDeviceListAdapter mAdapter;
	private Button device, device1;
	private Button pressure;
	private static String data = null;// 单次发送数据

	private static int startBytes = 50;
	private static int addBytes = 20;

	private static int successCount = 0;
	private static int failCount = 0;
	private static int recycleCount = 0;//循环次数
	private static int currentCount = 0;//当前次数
	private static int normalDaLen = 0;//一次发送的字节数
	private static boolean isRecyclerSend = false;

	private static HashMap<Integer, Integer> statisData = new HashMap<Integer, Integer>();
	private static HashMap<Integer, Integer> statisAllData = new HashMap<Integer, Integer>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.data_activity);
		startBytes = SpUtil.getIntPreferences(this, SpKey.START_BYTES, 50);
		addBytes = SpUtil.getIntPreferences(this, SpKey.ADD_BYTES, 20);
		recycleCount = SpUtil.getIntPreferences(this, SpKey.RETRY_TIMES, 1000);
		LeBroadcastCtrl.init(this, new IBroadcast() {

			@Override
			public void onTimeout() {
				long time = (System.currentTimeMillis() - lastTime) / 1000;
				ToastUtil
						.showSafeToast(DataActivity.this, "广播-超时" + time + "秒");
				DCCharUtils.showLogE(TAG, "广播-超时" + time + "秒");
				setAllStatis(false);
				setStatis(false);
				if (isRecyclerSend) {
					currentCount++;
					if (currentCount <= recycleCount) {
						restart();
					} else {
						ToastUtil.showSafeToast(DataActivity.this, "压测结束0");
					}
				}
			}

			@Override
			public void onBroadcastData(BluetoothDevice device, int rssi,
					byte[] scanRecord) {
				ToastUtil.showSafeToast(DataActivity.this, "广播-有收到数据");
				mAdapter.addDevice(device, rssi, scanRecord, true);
				mAdapter.notifyDataSetChanged();
			}

			@Override
			public void onAllData(String data) {
				long time = (System.currentTimeMillis() - lastTime) / 1000;
				ToastUtil.showSafeToast(DataActivity.this, "广播-所有数据" + time
						+ "秒");
				DCCharUtils.showLogE(TAG, "所有数据：" + data + "\n时间" + time + "秒");
				setAllStatis(true);
				setStatis(true);
				if (isRecyclerSend) {
					currentCount++;
					if (currentCount <= recycleCount) {
						restart();
					} else {
						ToastUtil.showSafeToast(DataActivity.this, "压测结束1");
					}
				}
			}

			@Override
			public void onError(int errCode) {
				switch (errCode) {
				case ErrorCode.RECEIVE_DATA_TYPE_ERROR:
					ToastUtil.showSafeToast(DataActivity.this,
							"广播-POS返回数据包类型错误");
					break;

				case ErrorCode.RECEIVE_DATA_LENGTH_ERROR:
					ToastUtil.showSafeToast(DataActivity.this,
							"广播-POS返回确认包中关于长度的错误");
					break;

				default:
					break;
				}
				DCCharUtils.showLogE(TAG, "广播-错误onError");
//				setAllStatis(false);
//				setStatis(false);
//				currentCount++;
//				if (currentCount <= recycleCount) {
//					restart();
//				} else {
//					ToastUtil.showSafeToast(DataActivity.this, "压测结束2");
//				}
			}

			@Override
			public void onWaiting() {
				ToastUtil.showSafeToast(DataActivity.this, "广播-等待接收数据中");
			}
		});
		initView();
	}
	/**
	 * 初始化控件
	 */
	private void initView() {
		device = (Button) findViewById(R.id.btn_device);
		device1 = (Button) findViewById(R.id.btn_device1);
		pressure = (Button) findViewById(R.id.btn_device2);
		tv_statistics = (TextView) findViewById(R.id.tv_statistics);
		tv_data_sta = (TextView) findViewById(R.id.tv_data_sta);
		ble_msg = (EditText) findViewById(R.id.ble_msg);
		ble_device_list = (ListView) findViewById(R.id.ble_device_list);
		mAdapter = new LeDeviceListAdapter(this);
		ble_device_list.setAdapter(mAdapter);
		device.setOnClickListener(this);
		device1.setOnClickListener(this);
		pressure.setOnClickListener(this);
		Constant.broadMaxTime = SpUtil.getIntPreferences(this, SpKey.NUMBER, 3);
		Constant.broadTime = SpUtil.getIntPreferences(this, SpKey.TIME, 300);
		Constant.scanNormalTime = SpUtil.getIntPreferences(this,
				SpKey.RETRY_INTERVAL, 2000);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_device:
			ToastUtil.showSafeToast(DataActivity.this, "发送无确认包广播",
					Toast.LENGTH_SHORT);
			if (mAdapter != null) {
				mAdapter.clearDevice();
			}
			isRecyclerSend = false;
			lastTime = System.currentTimeMillis();
			if (!TextUtils.isEmpty(ble_msg.getText())
					&& ble_msg.getText().toString().length() > 0
					&& ble_msg.getText().toString().length() % 2 == 0) {
				LeBroadcastCtrl.getDeviceInfo(ble_msg.getText().toString());
			} else {
				initData(SpUtil.getIntPreferences(this, SpKey.DATA_LENGTH, 300));
				LeBroadcastCtrl.getDeviceInfo(data);
			}
			break;
		case R.id.btn_device1:
			ToastUtil.showSafeToast(DataActivity.this, "发送有确认包广播",
					Toast.LENGTH_SHORT);
			if (mAdapter != null) {
				mAdapter.clearDevice();
			}
			resetData();
			isRecyclerSend = false;
			lastTime = System.currentTimeMillis();
			if (!TextUtils.isEmpty(ble_msg.getText())
					&& ble_msg.getText().toString().length() > 0
					&& ble_msg.getText().toString().length() % 2 == 0) {
				LeBroadcastCtrl.getCardInf(ble_msg.getText().toString());
			} else {
				normalDaLen = SpUtil.getIntPreferences(this, SpKey.DATA_LENGTH, 300);
				statisData.put(normalDaLen, 0);//成功次数
				statisAllData.put(normalDaLen, 0);
				initData(normalDaLen);
				LeBroadcastCtrl.getCardInf(data);
			}
			break;

		case R.id.btn_device2:// TODO
			ToastUtil.showSafeToast(DataActivity.this, "发送压测广播",
					Toast.LENGTH_SHORT);
			resetData();
			isRecyclerSend = true;
//			checkBytes(startBytes);
			statisData.put(startBytes, 0);//成功次数
			statisAllData.put(startBytes, 0);
			initData(startBytes);
			LeBroadcastCtrl.getCardInf(data);
			break;
		default:
			break;
		}
	}
	
	private void resetData(){
		successCount = 0;
		failCount = 0;
		currentCount = 1;
		statisData.clear();
		statisAllData.clear();
	}
	

	String frag = "01020304050607080911";
	/**
	 * 根据长度设置字节数据
	 * @param length
	 */
	private void initData(int length) {
		if (length < 10) {
			data = frag.substring(0, length * 2);
		} else {
			int step = 0;
			if (length % 10 != 0) {
				step = length / 10 + 1;
			} else {
				step = length / 10;
			}

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < step; i++) {
				if (i == step - 1) {
					sb.append(frag.subSequence(0,
							(length - (step - 1) * 10) * 2));
				} else {
					sb.append(frag);
				}
			}
			data = sb.toString();
		}
	}

	/**
	 * 检查并累加字节数
	 * @param count
	 */
	private void checkBytes(int count) {
		if (count + addBytes <= 300) {
			startBytes = count + addBytes;
		} else {
			startBytes = SpUtil.getIntPreferences(this, SpKey.START_BYTES, 50);
		}
	}
	

	/**
	 * 循环发送
	 */
	private void restart() {// TODO
		new Handler(getMainLooper()).postDelayed(new Runnable() {
			
			@Override
			public void run() {
				lastTime = System.currentTimeMillis();
				checkBytes(startBytes);
				initData(startBytes);
				LeBroadcastCtrl.getCardInf(data);				
			}
		}, 3000);
	}

	/**
	 * 设置全部统计
	 * @param isSuccess
	 */
	private void setAllStatis(boolean isSuccess) {
		if (isSuccess) {
			successCount++;
		} else {
			failCount++;
		}
		tv_statistics.setText("成功：" + successCount + "，失败：" + failCount
				+ "，总成功率："
				+ ((float) successCount / (successCount + failCount))*100 + "%");
	}
	
	/**
	 * 设置字节对应统计
	 * @param isSuccess
	 */
	private void setStatis(boolean isSuccess) {
		if (isRecyclerSend) {
			if (statisAllData.containsKey(startBytes)) {
				statisAllData.put(startBytes, statisAllData.get(startBytes) + 1);
			} else {
				statisAllData.put(startBytes, 1);
			}
			if (isSuccess) {
				if (statisData.containsKey(startBytes)) {
					statisData.put(startBytes, statisData.get(startBytes) + 1);
				} else {
					statisData.put(startBytes, 1);
				}
			}
		}else{
			if (statisAllData.containsKey(normalDaLen)) {
				statisAllData.put(normalDaLen, statisAllData.get(normalDaLen) + 1);
			} else {
				statisAllData.put(normalDaLen, 1);
			}
			if (isSuccess) {
				if (statisData.containsKey(normalDaLen)) {
					statisData.put(normalDaLen, statisData.get(normalDaLen) + 1);
				} else {
					statisData.put(normalDaLen, 1);
				}
			}
		}
		
		
		List<Map.Entry<Integer, Integer>> list = new ArrayList<Map.Entry<Integer,Integer>>(statisAllData.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() {

			@Override
			public int compare(Entry<Integer, Integer> lhs,
					Entry<Integer, Integer> rhs) {
				return lhs.getKey().compareTo(rhs.getKey());
			}
		});
		
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Integer, Integer> entry : list) {
			sb.append("字节数："
					+ entry.getKey()
					+ "，总次数："
					+ entry.getValue()
					+ "，成功次数："
					+ (statisData.get(entry.getKey()) == null ? 0
							: statisData.get(entry.getKey())) + "\n");
		}
		tv_data_sta.setText(sb.toString());
	}
}