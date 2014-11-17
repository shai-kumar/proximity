package com.zebra.proximity;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.zebra.proximity.DeviceManager.DeviceData;

public class MonitorService extends Service {
	final String TAG = "RfMonitorService";

	final int SCAN_DURATION = 1000;
	final int SCAN_INTERVAL = 0;

	Handler mScanHandler = new Handler();

	BluetoothAdapter mBluetoothAdapter;

	boolean mScanEnabled = false;
	boolean mIsScanning = false;

	static DeviceManager mDeviceManager = null;

	MonitorBinder mBinder = new MonitorBinder();
	
	@Override
	public void onCreate() {
		Log.d(TAG, "Rf service started.");

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (mBluetoothAdapter != null) {
			if (mDeviceManager == null)
				mDeviceManager = new DeviceManager();
			
			Log.d(TAG, "mDeviceManager initialized.");
			
			mDeviceManager.setCatchAll(false);
			
			mDeviceManager.registerDevice("C2:22:4A:D9:42:B4");
			
			startScan();
		}
	}

	@Override
	public void onDestroy() {
		stopScan();
	}

	public static DeviceManager getDeviceManager() {
		return mDeviceManager;
	}

	public void startScan() {
		mScanEnabled = true;

		if (mBluetoothAdapter.isEnabled() && !mIsScanning) {
			mIsScanning = true;

			mDeviceManager.onScanStart();

			mBluetoothAdapter.startLeScan(mLeScanCallback);

			mScanHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					restartScan();
				}
			}, SCAN_DURATION);
		}
	}

	public void restartScan() {
		if (mBluetoothAdapter.isEnabled() && mIsScanning) {
			mIsScanning = false;

			mBluetoothAdapter.stopLeScan(mLeScanCallback);

			mDeviceManager.onScanComplete();

			if (mScanEnabled) {
				mScanHandler.postDelayed(new Runnable() {

					@Override
					public void run() {
						startScan();
					}
				}, SCAN_INTERVAL);
			}
		}
	}

	public void stopScan() {
		mScanEnabled = false;

		if (mBluetoothAdapter != null) {
			mBluetoothAdapter.stopLeScan(mLeScanCallback);

			mDeviceManager.onScanComplete();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			// Process data
			new DeviceProcessor().execute(new DeviceData(device, rssi, scanRecord));
		}
	};

	private class DeviceProcessor extends AsyncTask<DeviceData, Void, Void> {

		@Override
		protected Void doInBackground(DeviceData... params) {
			// Process device using DeviceManager
			if (mDeviceManager != null)
				mDeviceManager.processDevice(params[0]);
			else
				Log.e(TAG, "No device manager found.");
			
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected void onProgressUpdate(Void... values) {
		}
	}
	
	public class MonitorBinder extends Binder {
		MonitorBinder getService() {
            // Return this instance of MonitorBinder so clients can call public methods
            return MonitorBinder.this;
        }
    };
}
