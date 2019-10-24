package com.dc.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SpUtil {
	
	   public static void setStringsave(Context context, String name, String values) {
	        SharedPreferences preferences = getDefaultSharedPreferences(context);
	        synchronized (preferences) {
	            Editor editor = preferences.edit();
	            editor.putString(name, values);
	            editor.commit();
	        }
	    }

	    public static String getStringPerferences(Context context, String name,
	                                              String defValues) {
	        SharedPreferences preferences = getDefaultSharedPreferences(context);
	        synchronized (preferences) {
	            return preferences.getString(name, defValues);
	        }
	    }

	    public static int getIntPreferences(Context context, String name,
	                                        int defValues) {
	        SharedPreferences preferences = getDefaultSharedPreferences(context);
	        synchronized (preferences) {
	            return preferences.getInt(name, defValues);
	        }
	    }

	    public static void setIntSave(Context context, String name, int value) {
	        SharedPreferences preferences = getDefaultSharedPreferences(context);
	        synchronized (preferences) {
	            Editor editor = preferences.edit();
	            editor.putInt(name, value);
	            editor.commit();
	        }
	    }

	    public static void setBooleanSave(Context context, String name, Boolean value) {
	        SharedPreferences preferences = getDefaultSharedPreferences(context);
	        synchronized (preferences) {
	            Editor editor = preferences.edit();
	            editor.putBoolean(name, value);
	            editor.commit();
	        }
	    }

	    public static boolean getBooleanPreferences(Context context, String name,
	                                                Boolean defValues) {
	        SharedPreferences preferences = getDefaultSharedPreferences(context);
	        synchronized (preferences) {
	            return preferences.getBoolean(name, defValues);
	        }
	    }


	    static SharedPreferences getDefaultSharedPreferences(Context context) {
	        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
	    }

}
