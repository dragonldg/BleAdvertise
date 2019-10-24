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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.dc.bluetooth.le.R;
import com.dc.util.ToastUtil;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class scaleUI extends Activity {

	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning = false;


	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";



	private static final int REQUEST_ENABLE_BT = 1;
	// Stops scanning after 20 seconds.
//	private static final long SCAN_PERIOD = 20000;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.scaleui);
		setContentView(new LinearLayout(getApplicationContext()));

//		final Intent intent = getIntent();
		// mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		// mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

//		SharedPreferences settings = getSharedPreferences("setting", 0);
//		mDeviceAddress = settings.getString("my_address", "");
//		mDeviceName = settings.getString("my_name", "");
//
//		if (mDeviceName == null) {
//			getActionBar().setTitle("未知名称");
//		} else
//			getActionBar().setTitle(mDeviceName);
//
//		m_addr = (TextView) findViewById(R.id.device_address);
//		m_addr.setText(mDeviceAddress);
//		m_weight = (TextView) findViewById(R.id.weight_value);
//		m_weight.setTextSize(32);
//		m_weight.setTextColor(Color.GREEN);
//
//		m_data = (TextView) findViewById(R.id.data_dbg);
//		m_data.setTextSize(8);
//		m_data.setTextColor(Color.BLUE);
//
//		mHandler = new Handler() {
//
//			@Override
//			public void handleMessage(Message msg) {
//				// TODO Auto-generated method stub
//				// super.handleMessage(msg);
//				switch (msg.what) {
//				case 5:
//					m_weight.setText(Integer.toString(scale_v));
//					scale_v = ((int) data_dbg[0] + 256) % 256;
//					m_data.setText(Integer.toString(scale_v));
//					for (int i = 1; i < 18; i++) {
//						scale_v = ((int) data_dbg[i] + 256) % 256;
//						m_data.append(" " + Integer.toHexString(scale_v));
//					}
//
//					break;
//				default:
//					break;
//				}
//			}
//
//		};
//
//		// Use this check to determine whether BLE is supported on the device.
//		// Then you can
//		// selectively disable BLE-related features.
//		if (!getPackageManager().hasSystemFeature(
//				PackageManager.FEATURE_BLUETOOTH_LE)) {
//			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT)
//					.show();
//			finish();
//		}
//
//		// Initializes a Bluetooth adapter. For API level 18 and above, get a
//		// reference to
//		// BluetoothAdapter through BluetoothManager.
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported,
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		scanLeDevice(true);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Ensures Bluetooth is enabled on the device. If Bluetooth is not
		// currently enabled,
		// fire an intent to display a dialog asking the user to grant
		// permission to enable it.
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}

		scanLeDevice(true);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT
				&& resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		super.onPause();
		scanLeDevice(false);
	}

	@SuppressWarnings("deprecation")
	private void scanLeDevice(final boolean enable) {
		if (enable && !mScanning) {

			mScanning = true;
			mBluetoothAdapter.startLeScan(mLeScaleCallback);
		} else {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScaleCallback);
		}
		// invalidateOptionsMenu();
	}

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScaleCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				final byte[] scanRecord) {
			// runOnUiThread(new Runnable() {
			// @Override
			// public void run() {
			ToastUtil.showSafeToast(scaleUI.this, "测试");
//			if (device.getAddress().toUpperCase().equals(mDeviceAddress)) {// "88:0F:10:10:AF:B3"
//				if ((scanRecord[14] == 3) && (scanRecord[15] == -1)) {
//
//					Log.w(TAG, "weight = " + scanRecord[16] + " Kg");
//					scale_v = ((int) scanRecord[16] + 256) % 256;
//					data_dbg = scanRecord;
//					Message message = new Message();
//					message.what = 5;
//					mHandler.sendMessage(message);
//				}
//			}
			// }
			// });
		}
	};

}