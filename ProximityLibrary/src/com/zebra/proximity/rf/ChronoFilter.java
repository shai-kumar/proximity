package com.zebra.proximity.rf;

import java.util.HashMap;
import java.util.LinkedList;

import com.zebra.proximity.rf.ProximityEstimator.Distance;

/**
 * This filter looks at the chronological collection of rssi samples to
 * determine if an enter or exit event has occurred.
 */
public class ChronoFilter extends ThresholdFilter {
	final int MAX_SAMPLES = 50;
	
	final int MIN_SAMPLES_ENTER = 1;
	final int MIN_SAMPLES_EXIT = 8;
	
	// TODO: Should consider the time between samples too
	
	HashMap<String, DeviceData> mTrackedDevices = new HashMap<String, DeviceData>();
	
	class DeviceData {
		LinkedList<Integer> mRssiReadings = new LinkedList<Integer>();
		Distance mLastEvent = Distance.UNKNOWN;
	}
	
	@Override
	public int apply(String mac, int rssi) {
		DeviceData data = null;
		
		if (mTrackedDevices.containsKey(mac)) {
			data = mTrackedDevices.get(mac);
		} else {
			data = new DeviceData();
			
			mTrackedDevices.put(mac, data);
		}
		
		if (data.mRssiReadings.size() > MAX_SAMPLES) {
			data.mRssiReadings.removeFirst();
		}
		
		data.mRssiReadings.add(rssi);
		
		boolean exitEvent = true;
		
		for (int i = Math.max(0, data.mRssiReadings.size() - MIN_SAMPLES_EXIT); i < data.mRssiReadings.size(); i++) {
			if (data.mRssiReadings.get(i) >= mThresholdFar) {
				exitEvent = false;
				break;
			}
		}
		
		if (exitEvent) {
			return data.mRssiReadings.getLast();
		}
		
		boolean enterEvent = true;
		
		for (int i = Math.max(0, data.mRssiReadings.size() - MIN_SAMPLES_ENTER); i < data.mRssiReadings.size(); i++) {
			if (data.mRssiReadings.get(i) >= mThresholdNear || data.mRssiReadings.get(i) < mThresholdFar) {
				enterEvent = false;
				break;
			}
		}
		
		if (enterEvent) {
			return data.mRssiReadings.getLast();
		}
		
		
		if(rssi < mThresholdNear) {
			return mThresholdNear - 1; // non conclusive
		} else {
			return rssi;
		}
	}
}
