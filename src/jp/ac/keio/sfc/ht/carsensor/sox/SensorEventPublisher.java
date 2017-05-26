package jp.ac.keio.sfc.ht.carsensor.sox;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;

import jp.ac.keio.sfc.ht.carsensor.Sensor;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEvent;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEventListener;
import jp.ac.keio.sfc.ht.carsensor.serialport.SensorSerialReader;
import jp.ac.keio.sfc.ht.sox.protocol.TransducerValue;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxConnection;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxDevice;

public class SensorEventPublisher implements SensorEventListener, Runnable {
	private static String SOX_SERVER = "sox.ht.sfc.keio.ac.jp";
	private static String SOX_USER = "guest";
	private static String SOX_PASSWD = "miroguest";
	private static String SOX_DEVICE = "romenTest";
	private static String SERIAL_PORT = "/dev/sensor";
	private static String CLASS_NAME = "SerialResponsePublisher";
	private static boolean debug = false;
	private SensorSerialReader sensor = null;
	private SoxDevice soxDevice = null;
	private SoxConnection soxConnection = null;
	private Base64.Encoder encoder = Base64.getEncoder();
	private boolean publishable = false;
	private BlockingQueue<TransducerValue> dataQueue = new LinkedBlockingQueue<TransducerValue>();
	private double lastSpeed = 0.0;
	private Vector3D lastAcceleration = null;
	final double ACC_THRESHOLD = 30;
	protected byte[] dataBuffer = null;
	protected int buffCount = 0;
	protected String timeStamps = "";
	protected final int dataBuffSize = 100;

	public SensorEventPublisher() throws TooManyListenersException {
		// TODO Auto-generated constructor stub
		connectToSox();
		connectToSensor();
		startSensing();

	}

	public SensorEventPublisher(String _soxServer, String _soxUser, String _soxPasswd, String _soxDevice,
			String _serialPort, boolean debug) throws TooManyListenersException {
		SOX_SERVER = _soxServer;
		SOX_USER = _soxUser;
		SOX_PASSWD = _soxPasswd;
		SOX_DEVICE = _soxDevice;
		SERIAL_PORT = _serialPort;
		this.debug = debug;

		connectToSox();
		connectToSensor();
		startSensing();

	}

	private void startSensing() {
		while (true) {
			try {

				// sensor.stopSensor();
				Thread.sleep(1000);
				debugMSG("Start Sensing...");
				sensor.getSensorInfo();
				sensor.getVS();
				sensor.startSensor();
				debugMSG("Done...");
				break;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				debugMSG("Failed...");
				System.exit(-1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	static void debugMSG(String msg) {
		if (debug) {
			System.out.println("[" + CLASS_NAME + "] " + msg);
		}
	}

	@Override
	public void handleSensorEvent(SensorEvent ev) {
		debugMSG("New sensor event arrived!");
		TransducerValue value = new TransducerValue();
		byte[] cmd = null;
		String gpsData = "";
		switch (ev.getEventType()) {
		case Sensor.EVENT_DATA_C:
			value.setId("GPS Data Event");

			gpsData = ev.getDatas().get("GPS Sentence");

			value.setTypedValue(gpsData);

			break;

		case Sensor.EVENT_DATA_A:
		case Sensor.EVENT_DATA_B:
			value.setId("Data Event");
			cmd = ev.getCmd();

			if (dataBuffer == null) {
				// System.out.println("bufferspace: " + cmd.length *
				// dataBuffSize);
				dataBuffer = new byte[cmd.length * dataBuffSize];
			}
			// System.out.println("Copy from" + cmd.length * buffCount + "to" +
			// (cmd.length * dataBuffSize+cmd.length));
			System.arraycopy(cmd, 0, dataBuffer, cmd.length * buffCount, cmd.length);
			timeStamps += ev.getTimestamp();
			buffCount++;

			if (buffCount < dataBuffSize) {
				return;
			}

			value.setTypedValue(encoder.encodeToString(dataBuffer));
			value.setRawValue(timeStamps);
			timeStamps = "";
			buffCount = 0;

			break;
		default:
			value.setId("Response Event");
			cmd = ev.getCmd();
			value.setTypedValue(encoder.encodeToString(cmd));
			if (debug) {
				value.setRawValue("0x" + Sensor.bytesToHexString(cmd));
			}

			break;

		}
		value.setTimestamp(ev.getTimestampString());

		if (publishable) {
			try {
				dataQueue.put(value);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	protected double accelerationDiff(double x, double y, double z) {

		Vector3D newAcce = new Vector3D(x, y, z);
		double diff = 0;
		if (lastAcceleration == null) {
			diff = 1000;
		} else {
			diff = Vector3D.distance(lastAcceleration, newAcce);
		}

		lastAcceleration = newAcce;
		return diff;

	}

	private void connectToSox() {
		try {
			if (soxConnection != null) {
				debugMSG("Reconnect to sox server " + SOX_SERVER + "...");
				soxConnection.connect();
			} else {
				debugMSG("Connect to sox server " + SOX_SERVER + "...");
				soxConnection = new SoxConnection(SOX_SERVER, false);
			}

			debugMSG("Done...");
			debugMSG("Create sox device " + SOX_DEVICE + "...");
			soxDevice = new SoxDevice(soxConnection, SOX_DEVICE);
			debugMSG("Done...");

		} catch (SmackException e1) {
			// TODO Auto-generated catch block
			debugMSG("Failed...");
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			debugMSG("Failed...");
			e1.printStackTrace();
		} catch (XMPPException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			debugMSG("Failed...");
			e1.printStackTrace();
		}

	}

	private void connectToSensor() {
		while (true) {
			try {

				debugMSG("Connect to sensor...");
				sensor = new SensorSerialReader(SERIAL_PORT);
				debugMSG("Done...");
				debugMSG("Add sensor event listener...");
				sensor.addSensorEventListener(this);
				debugMSG("Done...");
				return;
			} catch (Exception e) {
				debugMSG("Failed...");
			}
		}
	}

	public static void main(String argv[]) throws Exception {
		parseOptions(argv);
		SensorEventPublisher sensor = new SensorEventPublisher();
		Thread.sleep(1000 * 10);
		(new Thread(sensor)).start();
		while (true) {
			Thread.sleep(1000 * 60);
		}

	}

	protected static void parseOptions(String[] args) {
		if (args.length == 0) {
			System.err.println(
					"Usage: java -jar publisher.jar -s <soxServer> -u <soxUser> -p <soxPasswd> -d <soxDevice> -sp <serialPort> -debug <true/flase>");
			System.exit(1);
		}
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-d")) {
				SOX_DEVICE = args[++i];
			} else if (args[i].equals("-s")) {
				SOX_SERVER = args[++i];
			} else if (args[i].equals("-u")) {
				SOX_USER = args[++i];
			} else if (args[i].equals("-p")) {
				SOX_PASSWD = args[++i];
			} else if (args[i].equals("-sp")) {
				SERIAL_PORT = args[++i];
			} else if (args[i].equals("-debug")) {
				String debugPara = args[++i];
				if (debugPara.equals("true")) {
					debug = true;
				}
			} else {
				System.err.println("ERROR: invalid option " + args[i]);
				System.err.println(
						"Usage: java -jar publisher.jar -s <soxServer> -u <soxUser> -p <soxPasswd> -d <soxDevice> -sp <serialPort> -debug <true/flase>");
				System.exit(1);
			}
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		// set the publish flag true so that the data will be input.
		publishable = true;
		int pubAttemptCount = 1; //
		TransducerValue value = null;
		List<TransducerValue> values = new ArrayList<TransducerValue>();
		while (true) {

			try {

				if (values.isEmpty()) {
					// indicate that the last publish is successful
					value = dataQueue.take();
					do {
						values.add(value);
					} while ((value = dataQueue.poll()) != null);

				}

				if (values.isEmpty()) {
					continue;
				}
				debugMSG("Publish to " + SOX_DEVICE + "...");
				soxDevice.publishValues(values);
				debugMSG("Done...");
				// Published successfully. Clear the list and set count to 1
				values.clear();
				pubAttemptCount = 1;

			} catch (NotConnectedException e) {
				// TODO Auto-generated catch block
				pubAttemptCount++;
				debugMSG("Failed...");
				e.printStackTrace();
				if (pubAttemptCount == 5) {
					// reconnect to sox server
					connectToSox();
				}

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
