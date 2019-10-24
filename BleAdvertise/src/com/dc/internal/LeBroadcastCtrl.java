package com.dc.internal;

import com.dc.listener.IBroadcast;

import android.content.Context;

public class LeBroadcastCtrl {
	public static void init(Context context,IBroadcast broadcastListener){
		LeBroadcastInternal.init(context,broadcastListener);
	}
	
	public static void getDeviceInfo(String data){
		LeBroadcastInternal.broadcastData(data, true, false);
	}
	
	public static void getCardInf(String data){
		LeBroadcastInternal.broadcastData(data, true, true);
	}
}
