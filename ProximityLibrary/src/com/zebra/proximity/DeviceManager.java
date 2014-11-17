package com.zebra.proximity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.zebra.proximity.rf.ProximityEstimator;
import com.zebra.proximity.rf.ProximityEstimator.Distance;

public class DeviceManager {
	final static String TAG = "RfDeviceManager";
	
	final static int MANUFACTURER_SPECIFIC_TYPE = 0xFF;
	final static int COMPANY_IDENTIFIER = 0x75;
	final static int BEACON_TYPE = 0x01;
	final static int DATA_LENGTH = 0x01;

	boolean mCatchAll = false;

	ProximityEstimator mProximityEstimator = new ProximityEstimator();

	HashSet<String> mRecognizedDevices = new HashSet<String>();

	HashMap<String, DeviceData> mTrackedDevices = new HashMap<String, DeviceData>();

	HashSet<OnDeviceChangeListener> mOnDeviceListeners = new HashSet<OnDeviceChangeListener>();

	public static interface OnDeviceChangeListener {
		void onDiscover(String mac);

		void onDistanceMeasure(String mac, Distance distance);

		void onLoss(String mac);
	}

	static class DeviceData {
		BluetoothDevice mDevice;
		int mRssi;
		byte[] mScanRecord;
		String mMac;
		Distance mDistance = Distance.UNKNOWN;
		boolean mActive = false;

		public DeviceData(BluetoothDevice device, int rssi, byte[] scanRecord) {
			mMac = device.getAddress();
			mDevice = device;
			mRssi = rssi;
			mScanRecord = scanRecord;
		}
	}

	public void registerDevice(String mac) {
		if (!mRecognizedDevices.contains(mac)) {
			mRecognizedDevices.add(mac);
		}
	}

	public void unregisterDevice(String mac) {
		if (mRecognizedDevices.contains(mac)) {
			mRecognizedDevices.remove(mac);
		}
	}

	public void addOnDeviceListener(OnDeviceChangeListener listener) {
		Log.d(TAG, "addOnDeviceListener");
		
		mOnDeviceListeners.add(listener);
	}

	public void removeOnDeviceListener(OnDeviceChangeListener listener) {
		mOnDeviceListeners.remove(listener);
	}

	public void setCatchAll(boolean value) {
		mCatchAll = value;
	}

	public void onScanStart() {
		synchronized (mTrackedDevices) {
			for (DeviceData data : mTrackedDevices.values()) {
				data.mActive = false;
			}
		}
	}

	public void onScanComplete() {
		synchronized (mTrackedDevices) {
			Iterator<String> iterator = mTrackedDevices.keySet().iterator();
	
			while (iterator.hasNext()) {
				String key = iterator.next();
				
				final DeviceData data = mTrackedDevices.get(key);
	
				if (data.mActive == false) {
					dispatchLoss(key);
	
					iterator.remove();
				}
			}
		}
	}

	private void dispatchDiscovery(String mac) {
		Log.d(TAG, "mOnDeviceListeners count = "+ mOnDeviceListeners.size());
		
		for (OnDeviceChangeListener l : mOnDeviceListeners) {
			l.onDiscover(mac);
		}
	}

	private void dispatchDistanceMeassure(String mac, Distance distance) {
		for (OnDeviceChangeListener l : mOnDeviceListeners) {
			l.onDistanceMeasure(mac, distance);
		}
	}

	private void dispatchLoss(String mac) {
		for (OnDeviceChangeListener l : mOnDeviceListeners) {
			l.onLoss(mac);
		}
	}

	protected void processDevice(DeviceData data) {
		if (mCatchAll || recognizeAddress(data.mMac)) {
			synchronized (mTrackedDevices) {
				if (!mTrackedDevices.containsKey(data.mMac)) {
					mTrackedDevices.put(data.mMac, data);
	
					dispatchDiscovery(data.mMac);
				}
	
				mTrackedDevices.get(data.mMac).mActive = true;
				
				final Distance distance = mProximityEstimator.estimate(data.mMac, data.mRssi);
				
				if (mTrackedDevices.get(data.mMac).mDistance != distance) {
					mTrackedDevices.get(data.mMac).mDistance = distance;
					data.mDistance = distance;
					
					dispatchDistanceMeassure(data.mMac, data.mDistance);
				}
			}
		}
	}

	protected boolean recognizeAddress(String mac) {
		return mRecognizedDevices.contains(mac);
	}

	protected boolean recognizeType(byte[] advertisement) {
		short companyIdentifier = (short) (((advertisement[6] << 8) | advertisement[5]) & 0xFFFF);

		if (advertisement[4] != MANUFACTURER_SPECIFIC_TYPE)
			return false;

		if (companyIdentifier != COMPANY_IDENTIFIER)
			return false;

		if (advertisement[7] != BEACON_TYPE)
			return false;

		if (advertisement[8] != DATA_LENGTH)
			return false;

		return true;
	}
}
