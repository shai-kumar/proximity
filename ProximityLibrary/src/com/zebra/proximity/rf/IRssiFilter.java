package com.zebra.proximity.rf;

import com.zebra.proximity.rf.ProximityEstimator.Distance;

public interface IRssiFilter {
	int apply(String mac, int rssi);
	
	/**
	 * Return the discrete distance estimate in centimeters.
	 * @param mac
	 * @param rssi
	 * @return
	 */
	int finalizeDiscrete(String mac, int rssi);
	
	/**
	 * Return a relative distance estimate.
	 * @param mac
	 * @param rssi
	 * @return
	 */
	Distance finalize(String mac, int rssi);
}
