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
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.dc.bluetooth.le.R;
import com.dc.util.SpUtil;
import com.dc.util.Constant.SpKey;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class SettingActivity extends Activity implements OnClickListener {
	private Button save;
	private EditText number, time,data,retry_interval;
	private EditText et_start_bytes, et_add_bytes,et_retry_times;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting_activity);
		initView();
	}

	private void initView() {
		number = (EditText) findViewById(R.id.et_number);
		time = (EditText) findViewById(R.id.et_time);
		save = (Button) findViewById(R.id.btn_save);
		data = (EditText)findViewById(R.id.et_data);
		retry_interval = (EditText)findViewById(R.id.et_retry_interval);
		et_start_bytes = (EditText)findViewById(R.id.et_start_bytes);
		et_add_bytes = (EditText)findViewById(R.id.et_add_bytes);
		et_retry_times = (EditText)findViewById(R.id.et_retry_times);
		save.setOnClickListener(this);
		
		number.setText(""+SpUtil.getIntPreferences(this, SpKey.NUMBER, 3));
		time.setText(""+SpUtil.getIntPreferences(this, SpKey.TIME, 300));
		data.setText(""+SpUtil.getIntPreferences(this, SpKey.DATA_LENGTH, 300));
		retry_interval.setText(""+SpUtil.getIntPreferences(this, SpKey.RETRY_INTERVAL, 2000));
		et_start_bytes.setText(""+SpUtil.getIntPreferences(this, SpKey.START_BYTES, 50));
		et_add_bytes.setText(""+SpUtil.getIntPreferences(this, SpKey.ADD_BYTES, 30));
		et_retry_times.setText(""+SpUtil.getIntPreferences(this, SpKey.RETRY_TIMES, 100));
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
		case R.id.btn_save:
			saveAllData();
			finish();
			break;
		default:
			break;
		}
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		saveAllData();
	}
	
	private void saveAllData(){
		SpUtil.setIntSave(SettingActivity.this, SpKey.NUMBER, Integer.valueOf(number.getText().toString()));
		SpUtil.setIntSave(SettingActivity.this, SpKey.TIME, Integer.valueOf(time.getText().toString()));
		SpUtil.setIntSave(SettingActivity.this, SpKey.DATA_LENGTH, Integer.valueOf(data.getText().toString()));
		SpUtil.setIntSave(SettingActivity.this, SpKey.RETRY_INTERVAL, Integer.valueOf(retry_interval.getText().toString()));
		SpUtil.setIntSave(SettingActivity.this, SpKey.START_BYTES, Integer.valueOf(et_start_bytes.getText().toString()));
		SpUtil.setIntSave(SettingActivity.this, SpKey.ADD_BYTES, Integer.valueOf(et_add_bytes.getText().toString()));
		SpUtil.setIntSave(SettingActivity.this, SpKey.RETRY_TIMES, Integer.valueOf(et_retry_times.getText().toString()));
	}
}