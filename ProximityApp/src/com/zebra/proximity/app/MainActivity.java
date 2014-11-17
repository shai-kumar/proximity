package com.zebra.proximity.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.zebra.proximity.DeviceManager;
import com.zebra.proximity.DeviceManager.OnDeviceChangeListener;
import com.zebra.proximity.rf.ProximityEstimator.Distance;
import com.zebra.proximity.MonitorService;
import com.zebra.proximity.command_manager.CommandManager;
import com.zebra.proximity.slideshow.Slideshow;
import com.zebra.proximity.videoplayer.VideoPlayer;
import com.zebra.proximityapp.R;

public class MainActivity extends Activity {
	final static String TAG = "RfSenseApp";
	
	DeviceManager mDeviceManager;
	CommandManager mCommandManager;
	TextView mTextView;
	
	Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			mTextView.setText(msg.obj.toString());
		}
		
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mTextView = (TextView) findViewById(R.id.textView1);
		
		Log.d(TAG, "Attempting to start service.");
		
		bindService(new Intent(this, MonitorService.class), mConnection, Context.BIND_AUTO_CREATE);
		
	}
	
	void initialize() {
		
		Button slideshow = (Button) findViewById(R.id.button1);
		slideshow.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(getApplicationContext(), Slideshow.class));
				
			}
			
		});
		
		Button vidoplayer = (Button) findViewById(R.id.button2);
		vidoplayer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent in = new Intent(getApplicationContext(), VideoPlayer.class);
				in.putExtra("path", "/sdcard/lifesense/videos/1.mp4");
				startActivity(in);
				
			}
			
		});
		mDeviceManager = MonitorService.getDeviceManager();
		
		if (mDeviceManager != null) {
			mDeviceManager.addOnDeviceListener(new OnDeviceChangeListener() {
				
				@Override
				public void onLoss(String mac) {
					Log.d(TAG, "Lost device at "+ mac);
					
					Message msg = mHandler.obtainMessage();
					msg.obj = "Lost";
					
					mHandler.sendMessage(msg);
				}
				
				@Override
				public void onDistanceMeasure(String mac, Distance distance) {
					Log.d(TAG, "Distance measured for "+ mac +" at "+ distance.name());
					
					Message msg = mHandler.obtainMessage();
					msg.obj = distance.name();
					
					mHandler.sendMessage(msg);
				}
				
				@Override
				public void onDiscover(String mac) {
					Log.d(TAG, "Discovered device at "+ mac);
					
					Message msg = mHandler.obtainMessage();
					msg.obj = "Found";
					
					mHandler.sendMessage(msg);
				}
			});
		} else {
			Log.e(TAG, "mDeviceManager is null");
		}
		
		mCommandManager = CommandManager.getInstance(this);

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		boolean mBound;
		MonitorService mService;
		
        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to MonitorBinder, cast the IBinder and get LocalService instance
            //MonitorBinder binder = (MonitorBinder) service;
            //mService = binder.getService();
            mBound = true;
            
            initialize();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
