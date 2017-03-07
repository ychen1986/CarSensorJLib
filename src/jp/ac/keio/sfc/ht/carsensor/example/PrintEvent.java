/**
 * Copyright (C) 2015,  @author
 * Keio University, Japan
 */
package jp.ac.keio.sfc.ht.carsensor.example;

import java.util.Map;

import jp.ac.keio.sfc.ht.carsensor.SensorCMD;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEvent;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEventListener;
import jp.ac.keio.sfc.ht.carsensor.serialport.SensorSerialReader;
import jp.ac.keio.sfc.ht.carsensor.serialport.SensorSerialReaderTest;

/**
 * @author chenyin
 *
 */
public class PrintEvent implements SensorEventListener {

	private static String CLASS_NAME = "SerialResponsePublisher";
	private static boolean debug = true;

	static void debugMSG(String msg) {
		if (debug) {
			System.out.println("[" + CLASS_NAME + "] " + msg);
		}
	}

	static void done() {
		debugMSG("Done...");
	}

	static void failed() {
		debugMSG("Failed...");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jp.ac.keio.sfc.ht.carsensor.SensorEventListener#handleSensorEvent(jp.ac.
	 * keio.sfc.ht.carsensor.SensorEvent)
	 */
	@Override
	public void handleSensorEvent(SensorEvent ev) {
		// if(ev.getEventType() == SensorCMD.EVENT_DATA_C){
		// System.out.println("PM2.5=" + ev.getDatas().get("PM2.5"));
		// String gpsData = "";
		/*
		 * for(Map.Entry<String, String> entry : ev.getDatas().entrySet()){
		 * gpsData+=entry.getValue(); }
		 * 
		 */
		// System.out.println(ev.getDatas().get("GPS Sentence"));
		System.out.println(ev);
		// }

	}

	public PrintEvent() {
		this(false);
	}

	public PrintEvent(boolean _debug) {
		this.debug = _debug;
		SensorSerialReader sensor = null;
		String portPath = "/dev/sensor";
		// String portPath = "/dev/tty.usbmodem1411";

		// System.out.println("Starting Sensor....");
		try {
			debugMSG("Connecting to serial port " + portPath + "...");
			sensor = new SensorSerialReader(portPath, false);
			// sensor = new SensorSerialReaderTest(portPath, false);
			done();
			sensor.stopSensor();
			debugMSG("Add sensor event listener...");
			sensor.addSensorEventListener(this);
			done();
			debugMSG("Get sensor information...");
			sensor.getSensorInfo();
			done();
			debugMSG("Get Vs information...");
			sensor.getVS();
			Thread.sleep(1000);
			System.out.print(sensor);
			debugMSG("Start sensing...");
			// sensor.startSensor();
			sensor.startSensorWithoutFan();
			done();
			Thread.sleep(1000 * 60);

			sensor.stopSensor();

			Thread.sleep(1000);
			debugMSG("Sensing is stopped...");

			// sensor.getGPSStatus();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			failed();
			e.printStackTrace();
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new PrintEvent(true);

	}

}
