package com.dc.util;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

/**
 * 
 * @description:土司显示有关
 * @author: liudagang
 * @date:2018-3-10
 * @version:1.0
 */
public class ToastUtil {

	private static Toast toast;

	/**
	 * 显示土司
	 * @param context 上下文
	 * @param text 显示的文本
	 */
	public static void showToast(Context context, String text) {
		if (toast == null) {
			toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
		} else {
			toast.setText(text);
		}
		toast.show();
	}
	
	/**
	 * 显示土司
	 * @param context 上下文
	 * @param text 显示的文本
	 * @param time 显示的时间
	 */
	public static void showToast(Context context, String text,int time) {
		if (toast == null) {
			toast = Toast.makeText(context, text, time);
		} else {
			toast.setText(text);
		}
		toast.show();
	}

	/**
	 * 显示土司
	 * @param context activity
	 * @param text 显示的文本
	 */
	public static void showSafeToast(Context context, String text) {
		if (toast == null) {
			toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
		} else {
			toast.setText(text);
		}
		if (context instanceof Activity)
			((Activity) context).runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					toast.show();
				}
			});
	}
	
	/**
	 * 显示土司
	 * @param context activity
	 * @param text 显示的文本
	 * @param time 显示的时间
	 */
	public static void showSafeToast(Context context, String text,int time) {
		if (toast == null) {
			toast = Toast.makeText(context, text, time);
		} else {
			toast.setText(text);
		}
		if (context instanceof Activity)
			((Activity) context).runOnUiThread(new Runnable() {

				@Override
				public void run() {
					toast.show();
				}
			});
	}
}
