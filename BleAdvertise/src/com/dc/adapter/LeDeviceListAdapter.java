package com.dc.adapter;

import java.util.ArrayList;
import java.util.HashMap;

import com.dc.bluetooth.le.R;
import com.dc.util.DCCharUtils;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class LeDeviceListAdapter extends BaseAdapter {
//	private static final String TAG = "LeDeviceListAdapter";
	private HashMap<String, String> datas;
	private ArrayList<BluetoothDevice> mLeDevices;
	private ArrayList<Integer> mRSSIs;// 新加,Received Signal Strength Indicator
	private ArrayList<byte[]> mRecords;// 新加
	private Context context;
	private LayoutInflater mInflator;

	public LeDeviceListAdapter(Context context) {
		this.context = context;
		mInflator = LayoutInflater.from(this.context);
		this.datas = new HashMap<>();
		this.mLeDevices = new ArrayList<>();
		this.mRSSIs = new ArrayList<>();
		this.mRecords = new ArrayList<>();
	}

	public void clearDevice() {
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

	public void addDevice(BluetoothDevice device, int rssi, byte[] scanRecord,
			boolean filter) {
		if (filter) {
			// 是否继续发送和包序号
			byte[] pacNumBytes = new byte[1];
			System.arraycopy(scanRecord, 5, pacNumBytes, 0, 1);
//			String stopStr = DCCharUtils.bytes2HexString(pacNumBytes)
//					.substring(0, 1);
//			String numStr = DCCharUtils.bytes2HexString(pacNumBytes).substring(
//					1, 2);
			// 数据长度
			byte[] dataLenBytes = new byte[1];
			System.arraycopy(scanRecord, 3, dataLenBytes, 0, 1);
			int dataLen = DCCharUtils.byteArrayToInt(dataLenBytes);
			int realLen = dataLen - 3;

			// 数据内容
			byte[] dataBytes = new byte[realLen];
			System.arraycopy(scanRecord, 7, dataBytes, 0, realLen);
			String dataHex = DCCharUtils.bytes2HexString(dataBytes);
			if (mLeDevices == null || mLeDevices.size() == 0
					|| datas.size() == 0) {
				mLeDevices.add(device);
				mRSSIs.add(rssi);
				mRecords.add(scanRecord);
				datas.put(DCCharUtils.bytes2HexString(pacNumBytes), dataHex);
			} else {
				if (!datas
						.containsKey(DCCharUtils.bytes2HexString(pacNumBytes))) {
					mLeDevices.add(device);
					mRSSIs.add(rssi);
					mRecords.add(scanRecord);
					datas.put(DCCharUtils.bytes2HexString(pacNumBytes), dataHex);
				}
			}
//			if ((stopStr.contains("c") || stopStr.contains("C"))
//					&& Integer.valueOf(numStr, 16) == (datas.size() - 1)) {
//				List<Map.Entry<String, String>> list = new ArrayList<>(
//						datas.entrySet());
//				Collections.sort(list,
//						new Comparator<Map.Entry<String, String>>() {
//
//							@Override
//							public int compare(Entry<String, String> lhs,
//									Entry<String, String> rhs) {
//								return lhs.getKey().compareTo(rhs.getKey());
//							}
//						});
//				for (Entry<String, String> entry : list) {
//					DCCharUtils.showLogE(TAG,
//							entry.getKey() + ":" + entry.getValue());
//				}
//			}
		} else {
			mLeDevices.add(device);
			mRSSIs.add(rssi);
			mRecords.add(scanRecord);
		}
	}

	public BluetoothDevice getDevice(int position) {
		if (mLeDevices != null) {
			return mLeDevices.get(position);
		} else {
			return null;
		}
	}

	@Override
	public int getCount() {
		if (mLeDevices == null)
			return 0;
		else
			return mLeDevices.size();
	}

	@Override
	public Object getItem(int position) {
		return mLeDevices.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int i, View view, final ViewGroup parent) {
		ViewHolder viewHolder;
		if (view == null) {
			view = mInflator.inflate(R.layout.listitem_device, null);
			viewHolder = new ViewHolder();
			viewHolder.deviceAddress = (TextView) view
					.findViewById(R.id.device_address);
			viewHolder.deviceName = (TextView) view
					.findViewById(R.id.device_name);
			viewHolder.deviceRssi = (TextView) view
					.findViewById(R.id.device_rssi);
			viewHolder.deviceBroadcastPack = (TextView) view
					.findViewById(R.id.device_broadcastPack);
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}

		BluetoothDevice device = mLeDevices.get(i);
		int rssi = mRSSIs.get(i);
		byte[] scanRecord = mRecords.get(i);
		byte[] pacNumBytes = new byte[1];
		System.arraycopy(scanRecord, 5, pacNumBytes, 0, 1);
		final String deviceName = "设备名：" + device.getName() + "，序号：" + i
				+ "，包号：" + DCCharUtils.bytesToHex(pacNumBytes);
		final String deviceAddr = "Mac地址：" + device.getAddress();
		final String broadcastPack = "广播包："
				+ DCCharUtils.bytesToHex(scanRecord);
		final String rssiString = "RSSI:" + String.valueOf(rssi) + "dB";
		if (deviceName != null && deviceName.length() > 0) {
			viewHolder.deviceName.setText(deviceName);
			viewHolder.deviceAddress.setText(deviceAddr);
			viewHolder.deviceBroadcastPack.setText(broadcastPack);
			if (Math.abs(rssi) > 80) {
				viewHolder.deviceRssi.setTextColor(Color.RED);
			} else if (Math.abs(rssi) > 70 && Math.abs(rssi) <= 80) {
				viewHolder.deviceRssi.setTextColor(0xFFE4C400);
			} else if (Math.abs(rssi) > 60 && Math.abs(rssi) <= 70) {
				viewHolder.deviceRssi.setTextColor(Color.BLUE);
			} else {
				viewHolder.deviceRssi.setTextColor(Color.GREEN);
			}
			viewHolder.deviceRssi.setText(rssiString);
		} else {
			viewHolder.deviceName.setText(R.string.unknown_device + "，序号:" + i);
			viewHolder.deviceAddress.setText(deviceAddr);
			viewHolder.deviceBroadcastPack.setText(broadcastPack);
			if (Math.abs(rssi) > 80) {
				viewHolder.deviceRssi.setTextColor(Color.RED);
			} else if (Math.abs(rssi) > 70 && Math.abs(rssi) <= 80) {
				viewHolder.deviceRssi.setTextColor(0xFFE4C400);
			} else if (Math.abs(rssi) > 60 && Math.abs(rssi) <= 70) {
				viewHolder.deviceRssi.setTextColor(Color.BLUE);
			} else {
				viewHolder.deviceRssi.setTextColor(Color.GREEN);
			}
			viewHolder.deviceRssi.setText(rssiString);
		}
		return view;
	}

	static class ViewHolder {
		TextView deviceName;
		TextView deviceAddress;
		TextView deviceBroadcastPack;
		TextView deviceRssi;
	}
}
