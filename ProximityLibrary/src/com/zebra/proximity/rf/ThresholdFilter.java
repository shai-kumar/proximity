package com.zebra.proximity.rf;

import com.zebra.proximity.rf.ProximityEstimator.Distance;

public class ThresholdFilter implements IRssiFilter {
	final int DEFAULT_THRESHOLD_FAR = -70;
	final int DEFAULT_THRESHOLD_NEAR = -40;
	final int DEFAULT_THRESHOLD_CLOSE = 0;
	
	int mThresholdFar = DEFAULT_THRESHOLD_FAR;
	int mThresholdNear = DEFAULT_THRESHOLD_NEAR;
	int mThresholdClose = DEFAULT_THRESHOLD_CLOSE;
	
	public void setThresholds(int far, int near, int close) {
		mThresholdFar = far;
		mThresholdNear = near;
		mThresholdClose = close;
	}
	
	@Override
	public int apply(String mac, int rssi) {
		return rssi;
	}

	@Override
	public Distance finalize(String mac, int rssi) {
		if (rssi < mThresholdFar) {
			return Distance.FAR;
		} else if (rssi < mThresholdNear) {
			return Distance.NEAR;
		} else if (rssi < mThresholdClose) {
			return Distance.CLOSE;
		}
		
		return Distance.UNKNOWN;
	}

	@Override
	public int finalizeDiscrete(String mac, int rssi) {
		throw new UnsupportedOperationException();
	}
	
}
