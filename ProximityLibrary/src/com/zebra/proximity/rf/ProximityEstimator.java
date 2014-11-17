package com.zebra.proximity.rf;

import java.util.ArrayList;

import android.util.Log;

public class ProximityEstimator {
	final String TAG = "RfProximityEstimator";

	final int DISTANCE_DISCRETE_UNKNOWN = -1;
	
	ArrayList<IRssiFilter> mFilters = new ArrayList<IRssiFilter>();
	
	public enum Distance {
		CLOSE, NEAR, FAR, UNKNOWN
	}
	
	public ProximityEstimator() {
		mFilters.add(new ThresholdFilter());
	}
	
	public void clearFilters() {
		mFilters.clear();
	}
	
	public void addFilter(IRssiFilter filter, int index) {
		mFilters.add(index, filter);
	}
	
	public void removeFilter(int index) {
		mFilters.remove(index);
	}
	
	public IRssiFilter getFilter(int index) {
		return mFilters.get(index);
	}

	public Distance estimate(String mac, int rssi) {
		int filteredRssi = 0;
		
		for(int i = 0; i < mFilters.size(); i++) {
			filteredRssi = mFilters.get(i).apply(mac, rssi);
			
			if (i == (mFilters.size() - 1)) {
				Log.d(TAG, "Return distance result.");
				return mFilters.get(i).finalize(mac, filteredRssi);
			}
		}
		
		return Distance.UNKNOWN;
	}
	
	public int estimateDiscrete(String mac, int rssi) {
		int filteredRssi = 0;
		
		for(int i = 0; i < mFilters.size(); i++) {
			filteredRssi = mFilters.get(i).apply(mac, rssi);
			
			if (i == (mFilters.size() - 1)) {
				return mFilters.get(i).finalizeDiscrete(mac, filteredRssi);
			}
		}
		
		return DISTANCE_DISCRETE_UNKNOWN;
	}
}
