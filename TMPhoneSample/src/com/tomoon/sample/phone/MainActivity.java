package com.tomoon.sample.phone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tomoon.sdk.TMPhoneSender;
import com.tomoon.sdk.pebble.PebbleDictionary;
import com.tomoon.sdk.pebble.PebbleKit;
import com.tomoon.sdk.pebble.PebbleKit.PebbleAckReceiver;
import com.tomoon.sdk.pebble.PebbleKit.PebbleDataReceiver;
import com.tomoon.sdk.pebble.PebbleKit.PebbleNackReceiver;
import com.tomoon.watch.utils.TMLog;

public class MainActivity extends Activity implements View.OnClickListener {
	private TextView mTextView;
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			onMessage();
		}
	};

	private void onMessage() {
		mTextView.setText(String.format(getResources().getString(R.string.get_requests), SampleReceiver.sReceiverCount));
	}

	private PebbleAckReceiver mPebbleAckRecv;
	private PebbleNackReceiver mPebbleNackRecv;
	private PebbleDataReceiver mPebbleDataRecv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_activity);
		mTextView = (TextView) findViewById(R.id.tv_req);
		if (!isTomoonAppInstalled()) {
			Toast.makeText(MainActivity.this, getResources().getString(R.string.please_download_tomoon_app),
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		SampleReceiver.setHandler(mHandler);

		// test sending msg from phone
		findViewById(R.id.btn_pebble_music).setOnClickListener(this);
		findViewById(R.id.btn_pebble_noti).setOnClickListener(this);
		findViewById(R.id.btn_pebble_data).setOnClickListener(this);

		UUID uuid = UUID.fromString("ee4d768c-84c7-4352-8e4f-eef31194b183");
		mPebbleAckRecv = new PebbleAckReceiver(uuid) {

			@Override
			public void receiveAck(Context context, int transactionId) {
				Toast.makeText(MainActivity.this, getResources().getString(R.string.phone_received_pebble_ack),
						Toast.LENGTH_SHORT).show();
				UUID uuid = UUID
						.fromString("ee4d768c-84c7-4352-8e4f-eef31194b182");
				TMPhoneSender.sendNackToPebble(context, uuid, transactionId);
			}
		};
		mPebbleNackRecv = new PebbleNackReceiver(uuid) {

			@Override
			public void receiveNack(Context context, int transactionId) {
				Toast.makeText(MainActivity.this, getResources().getString(R.string.phone_received_pebble_nack),
						Toast.LENGTH_SHORT).show();
				UUID uuid = UUID
						.fromString("ee4d768c-84c7-4352-8e4f-eef31194b182");
				TMPhoneSender.sendAckToPebble(context, uuid, transactionId);
			}
		};
		mPebbleDataRecv = new PebbleDataReceiver(uuid) {

			@Override
			public void receiveData(Context context, int transactionId,
					PebbleDictionary data) {
				Toast.makeText(
						MainActivity.this,
						"data "
								+ (data == null ? "" : data.toJsonString()),
						Toast.LENGTH_SHORT).show();
			}
		};

		PebbleKit.registerReceivedAckHandler(this, mPebbleAckRecv);
		PebbleKit.registerReceivedDataHandler(this, mPebbleDataRecv);
		PebbleKit.registerReceivedNackHandler(this, mPebbleNackRecv);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		SampleReceiver.setHandler(null);
		unregisterReceiver(mPebbleAckRecv);
		unregisterReceiver(mPebbleNackRecv);
		unregisterReceiver(mPebbleDataRecv);
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.btn_pebble_music) {
			sendMusicUpdateToPebble();
		} else if (R.id.btn_pebble_noti == id) {
			sendAlertToPebble();
		} else if (R.id.btn_pebble_data == id) {
			UUID uuid = UUID.fromString("ee4d768c-84c7-4352-8e4f-eef31194b182");
			PebbleDictionary pd = new PebbleDictionary();
			pd.addString(10, "data");
			PebbleKit.sendDataToPebble(this, uuid, pd);
		}
	}

	private void sendMusicUpdateToPebble() {
		final Intent i = new Intent("com.getpebble.action.NOW_PLAYING");
		i.putExtra("artist", "Carly Rae Jepsen");
		i.putExtra("album", "Kiss");
		i.putExtra("track", "Call Me Maybe");

		sendBroadcast(i);
	}

	private void sendAlertToPebble() {
		final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");

		final Map<String, String> data = new HashMap<String, String>();
		data.put("title", "Test Message");
		data.put("body",
				"Whoever said nothing was impossible never tried to slam a revolving door.");
		final JSONObject jsonData = new JSONObject(data);
		final String notificationData = new JSONArray().put(jsonData)
				.toString();

		i.putExtra("messageType", "PEBBLE_ALERT");
		i.putExtra("sender", "MyAndroidApp");
		i.putExtra("notificationData", notificationData);

		TMLog.LOGD("About to send a modal alert to Pebble: " + notificationData);
		sendBroadcast(i);
	}
	
	//check if tomoon App is installed
	public boolean isTomoonAppInstalled() {
		List<PackageInfo> packages = getPackageManager().getInstalledPackages(0);
		for(int i=0; i<packages.size(); i++) {
			if(packages.get(i).packageName.contains("com.tomoon.launcher"))
				return true;
		}
		return false;	
	}
}
