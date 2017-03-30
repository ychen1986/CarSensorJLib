/**
 * Copyright (C) 2015  @author Yin Chen 
 * Keio University, Japan
 */

package jp.ac.keio.sfc.ht.carsensor.protocol;

import java.util.EventListener;

/**
 * @author chenyin
 *
 */
public interface SensorEventListener extends EventListener {
	public abstract void handleSensorEvent(SensorEvent ev) throws Exception;
}
