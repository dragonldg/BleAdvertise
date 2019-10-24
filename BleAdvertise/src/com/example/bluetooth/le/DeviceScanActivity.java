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
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.dc.adapter.LeDeviceListAdapter;
import com.dc.bluetooth.le.R;
import com.dc.util.DCCharUtils;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
// https://blog.csdn.net/hechao3225/article/details/54342172
public class DeviceScanActivity extends ListActivity {
	private LeDeviceListAdapter mLeDeviceListAdapter;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothManager bluetoothManager;
	private boolean mScanning;
	private boolean isFilter = true;
	private MenuItem gMenuItem;

	SharedPreferences settings;

	private static final int REQUEST_ENABLE_BT = 1;
	// Stops scanning after 10 seconds.
	@SuppressWarnings("unused")
	private static final long SCAN_PERIOD = 20000;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle(R.string.title_devices);
		// Use this check to determine whether BLE is supported on the device.
		// Then you can
		// selectively disable BLE-related features.
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT)
					.show();
			finish();
		}

		// Initializes a Bluetooth adapter. For API level 18 and above, get a
		// reference to
		// BluetoothAdapter through BluetoothManager.
		bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported,
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		settings = getSharedPreferences("setting", 0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		if (isFilter) {
			menu.findItem(R.id.menu_dc).setTitle("DC-data");
		} else {
			menu.findItem(R.id.menu_dc).setTitle("All-data");
		}
		menu.findItem(R.id.menu_data).setTitle("StartAdv");
		gMenuItem = menu.findItem(R.id.menu_dc);
		menu.findItem(R.id.menu_setting).setTitle("setting");
		menu.findItem(R.id.menu_refresh).setTitle("detail");
		if (!mScanning) {
			menu.findItem(R.id.menu_stop).setVisible(false);
			menu.findItem(R.id.menu_scan).setVisible(true);

		} else {
			menu.findItem(R.id.menu_stop).setVisible(true);
			menu.findItem(R.id.menu_scan).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_dc:
			if (isFilter) {
				gMenuItem.setTitle("ALL-data");
			} else {
				gMenuItem.setTitle("DC-data");
			}
			isFilter = !isFilter;
			break;
		case R.id.menu_data:
			Intent intent = new Intent();
			intent.setClass(this, DataActivity.class);
			startActivity(intent);
			break;
		case R.id.menu_scan:
			mLeDeviceListAdapter.clearDevice();
			getActionBar().setTitle(R.string.title_devices);
			setListAdapter(mLeDeviceListAdapter);
			scanLeDevice(true);
			break;
		case R.id.menu_stop:
			scanLeDevice(false);
			break;
		case R.id.menu_refresh:
			Intent enableWeight = new Intent(this, scaleUI.class);
			startActivity(enableWeight);
			break;
		case R.id.menu_setting:
			Intent enableSetting = new Intent(this, SettingActivity.class);
			startActivity(enableSetting);
			break;
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		getActionBar().setTitle(R.string.title_devices);

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

		// Initializes list view adapter.
		mLeDeviceListAdapter = new LeDeviceListAdapter(this);
		setListAdapter(mLeDeviceListAdapter);
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
		mLeDeviceListAdapter.clearDevice();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
		if (device == null)
			return;

		Toast.makeText(this, "Selected " + device.getAddress().toString(),
				Toast.LENGTH_LONG).show();
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("my_address", device.getAddress().toUpperCase());
		// editor.putString("my_name", device.getName().toString());
		editor.commit();

		final Intent intent = new Intent(this, scaleUI.class);
		intent.putExtra(scaleUI.EXTRAS_DEVICE_NAME, device.getName());
		intent.putExtra(scaleUI.EXTRAS_DEVICE_ADDRESS, device.getAddress());
		if (mScanning) {
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			mScanning = false;
		}
		startActivity(intent);
	}

	@SuppressWarnings("deprecation")
	private void scanLeDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			// mHandler.postDelayed(new Runnable() {
			// @Override
			// public void run() {
			// mScanning = false;
			// mBluetoothAdapter.stopLeScan(mLeScanCallback);
			// // if (datas != null) {
			// // datas.clear();
			// // mLeDevices.clear();
			// // mRSSIs.clear();
			// // mRecords.clear();
			// // }
			// invalidateOptionsMenu();
			// }
			// }, SCAN_PERIOD);
			mScanning = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
		invalidateOptionsMenu();
	}

	// // Adapter for holding devices found through scanning.
	// private class LeDeviceListAdapter extends BaseAdapter {
	//
	//
	// // [例1] 如果发射功率P为1mw，折算为dBm后为0dBm。
	// // [例2] 对于40W的功率，按dBm单位进行折算后的值应为：
	// // 10lg（40W/1mw)=10lg（40000）=10lg4+10lg10+10lg1000=46dBm。
	// // 一般情况下，经典蓝牙强度
	// // -50 ~ 0dBm 信号强
	// // -70 ~-50dBm信号中
	// // <-70dBm 信号弱
	// //
	// // 低功耗蓝牙分四级
	// // -60 ~ 0 4
	// // -70 ~ -60 3
	// // -80 ~ -70 2
	// // <-80 1
	//
	// private LayoutInflater mInflator;
	//
	// public LeDeviceListAdapter() {
	// super();
	//
	// mInflator = DeviceScanActivity.this.getLayoutInflater();
	// }
	//
	// public void addDevice(BluetoothDevice device, int rssi,
	// byte[] scanRecord) {
	// byte[] pacNumBytes = new byte[1];
	// System.arraycopy(scanRecord, 5, pacNumBytes, 0, 1);
	// String stopStr = DCCharUtils.bytes2HexString(pacNumBytes)
	// .substring(0, 1);
	// byte[] dataLenBytes = new byte[1];
	// System.arraycopy(scanRecord, 3, dataLenBytes, 0, 1);
	// int dataLen = byteArrayToInt(dataLenBytes);
	// int realLen = dataLen - 3;
	// byte[] dataBytes = new byte[realLen];
	// System.arraycopy(scanRecord, 7, dataBytes, 0, realLen);
	// String dataHex = DCCharUtils.bytes2HexString(dataBytes);
	// if (mLeDevices == null || mLeDevices.size() == 0
	// || datas.size() == 0) {
	// Log.e("ok", DCCharUtils.bytes2HexString(pacNumBytes));
	//
	// mLeDevices.add(device);
	// mRSSIs.add(rssi);
	// mRecords.add(scanRecord);
	// datas.put(DCCharUtils.bytes2HexString(pacNumBytes), dataHex);
	// } else {
	// if (!datas
	// .containsKey(DCCharUtils.bytes2HexString(pacNumBytes))) {
	// Log.e("ok", DCCharUtils.bytes2HexString(pacNumBytes));
	//
	// mLeDevices.add(device);
	// mRSSIs.add(rssi);
	// mRecords.add(scanRecord);
	// datas.put(DCCharUtils.bytes2HexString(pacNumBytes), dataHex);
	// }
	// }
	// if (stopStr.contains("c") || stopStr.contains("C")) {
	// List<Map.Entry<String, String>> list = new ArrayList<>(
	// datas.entrySet());
	// Collections.sort(list,
	// new Comparator<Map.Entry<String, String>>() {
	//
	// @Override
	// public int compare(Entry<String, String> lhs,
	// Entry<String, String> rhs) {
	// return lhs.getKey().compareTo(rhs.getKey());
	// }
	// });
	// for (Entry<String, String> entry : list) {
	// Log.e("OK", entry.getKey() + "==" + entry.getValue());
	// }
	// }
	// }
	//
	// public BluetoothDevice getDevice(int position) {
	// return mLeDevices.get(position);
	// }
	//
	// public void clear() {
	// mLeDevices.clear();
	// }
	//
	// @Override
	// public int getCount() {
	// return mLeDevices.size();
	// }
	//
	// @Override
	// public Object getItem(int i) {
	// return mLeDevices.get(i);
	// }
	//
	// @Override
	// public long getItemId(int i) {
	// return i;
	// }
	//
	// @Override
	// public View getView(int i, View view, ViewGroup viewGroup) {
	// ViewHolder viewHolder;
	// // General ListView optimization code.
	// if (view == null) {
	// view = mInflator.inflate(R.layout.listitem_device, null);
	// viewHolder = new ViewHolder();
	// viewHolder.deviceAddress = (TextView) view
	// .findViewById(R.id.device_address);
	// viewHolder.deviceName = (TextView) view
	// .findViewById(R.id.device_name);
	// viewHolder.deviceRssi = (TextView) view
	// .findViewById(R.id.device_rssi);
	// viewHolder.deviceBroadcastPack = (TextView) view
	// .findViewById(R.id.device_broadcastPack);
	// view.setTag(viewHolder);
	// } else {
	// viewHolder = (ViewHolder) view.getTag();
	// }
	//
	// BluetoothDevice device = mLeDevices.get(i);
	// int rssi = mRSSIs.get(i);
	// byte[] scanRecord = mRecords.get(i);
	// byte[] pacNumBytes = new byte[1];
	// System.arraycopy(scanRecord, 5, pacNumBytes, 0, 1);
	// final String deviceName = "设备名：" + device.getName() + "，序号：" +
	// i+"，包号："+bytesToHex(pacNumBytes);
	// final String deviceAddr = "Mac地址：" + device.getAddress();
	// final String broadcastPack = "广播包：" + bytesToHex(scanRecord);
	// final String rssiString = "RSSI:" + String.valueOf(rssi) + "dB";
	// if (deviceName != null && deviceName.length() > 0) {
	// viewHolder.deviceName.setText(deviceName);
	// viewHolder.deviceAddress.setText(deviceAddr);
	// viewHolder.deviceBroadcastPack.setText(broadcastPack);
	// if (Math.abs(rssi) > 80) {
	// viewHolder.deviceRssi.setTextColor(Color.RED);
	// } else if (Math.abs(rssi) > 70 && Math.abs(rssi) <= 80) {
	// viewHolder.deviceRssi.setTextColor(0xFFE4C400);
	// } else if (Math.abs(rssi) > 60 && Math.abs(rssi) <= 70) {
	// viewHolder.deviceRssi.setTextColor(Color.BLUE);
	// } else {
	// viewHolder.deviceRssi.setTextColor(Color.GREEN);
	// }
	// viewHolder.deviceRssi.setText(rssiString);
	// } else {
	// viewHolder.deviceName.setText(R.string.unknown_device + "，序号:"
	// + i);
	// viewHolder.deviceAddress.setText(deviceAddr);
	// viewHolder.deviceBroadcastPack.setText(broadcastPack);
	// if (Math.abs(rssi) > 80) {
	// viewHolder.deviceRssi.setTextColor(Color.RED);
	// } else if (Math.abs(rssi) > 70 && Math.abs(rssi) <= 80) {
	// viewHolder.deviceRssi.setTextColor(0xFFE4C400);
	// } else if (Math.abs(rssi) > 60 && Math.abs(rssi) <= 70) {
	// viewHolder.deviceRssi.setTextColor(Color.BLUE);
	// } else {
	// viewHolder.deviceRssi.setTextColor(Color.GREEN);
	// }
	// viewHolder.deviceRssi.setText(rssiString);
	// }
	//
	// return view;
	// }
	// }
	//
	// static class ViewHolder {
	// TextView deviceName;
	// TextView deviceAddress;
	// TextView deviceBroadcastPack;
	// TextView deviceRssi;
	// }
	//
	// static final char[] hexArray = "0123456789ABCDEF".toCharArray();
	//
	// private static String bytesToHex(byte[] bytes) {
	//
	// char[] hexChars = new char[bytes.length * 2];
	//
	// for (int j = 0; j < bytes.length; j++) {
	//
	// int v = bytes[j] & 0xFF;
	//
	// hexChars[j * 2] = hexArray[v >>> 4];
	//
	// hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	//
	// }
	//
	// return new String(hexChars);
	// }

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,
				final byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (isFilter) {
						byte[] head = new byte[2];
						System.arraycopy(scanRecord, 0, head, 0, 2);
						byte[] mani = new byte[1];
						System.arraycopy(scanRecord, 4, mani, 0, 1);
						byte[] temp = new byte[1];
						System.arraycopy(scanRecord, 5, temp, 0, 1);
						byte[] id = new byte[1];
						System.arraycopy(scanRecord, 6, id, 0, 1);
						String tempStr = DCCharUtils.bytes2HexString(temp)
								.substring(0, 1);
						if (DCCharUtils.bytes2HexString(head).equals("0201")
								&& DCCharUtils.bytes2HexString(mani).equals(
										"FF")
								&& (tempStr.contains("C") || tempStr
										.contains("D")
										&& DCCharUtils.bytes2HexString(id)
												.equals("00"))) {
							mLeDeviceListAdapter.addDevice(device, rssi,
									scanRecord,true);
							mLeDeviceListAdapter.notifyDataSetChanged();
						}
					} else {
						mLeDeviceListAdapter
								.addDevice(device, rssi, scanRecord,false);
						mLeDeviceListAdapter.notifyDataSetChanged();
					}
				}
			});
		}
	};

	// public static int byteArrayToInt(byte[] b) {
	// if (b.length == 2) {
	// return b[1] & 0xFF | (b[0] & 0xFF) << 8;
	// } else {
	// return b[0] & 0xFF;
	// }
	// }
}