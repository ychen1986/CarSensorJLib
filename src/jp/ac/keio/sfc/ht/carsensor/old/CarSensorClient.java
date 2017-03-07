package jp.ac.keio.sfc.ht.carsensor.old;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.Properties;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import jp.ac.keio.sfc.ht.carsensor.SIMCard;
import jp.ac.keio.sfc.ht.carsensor.sox.SensorEventPublisher;
import jp.ac.keio.sfc.ht.sox.protocol.TransducerValue;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxConnection;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxDevice;
import jp.ac.keio.sfc.ht.sox.soxlib.event.SoxEvent;
import jp.ac.keio.sfc.ht.sox.soxlib.event.SoxEventListener;

public class CarSensorClient {
	private static String CLASS_NAME = "CarSensorPublisher";
	private String configFile = "carsensor.config";
	private String soxServer = "sox.ht.sfc.keio.ac.jp";
	private String soxUser = "guest";
	private String soxPasswd = "miroguest";
	private String soxCCHDevice = "";
	private String serialPort = "/dev/sensor";
	private String soxPubDevice = "FujiCarSensor2";
	private String phoneNumber = "";
	private String SIMFile = "";
	private static boolean debug = false;
	private static boolean CONFIG_FLAG = false; // CONFIG_FLAG = ture indicates
												// that command
												// line parameters will be used
												// rather than those of
												// configure file.

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new CarSensorClient(args);

	}

	static void debugMSG(String msg) {
		if (debug) {
			System.out.println("[" + CLASS_NAME + "] " + msg);
		}
	}

	public CarSensorClient(String[] args) {
		parseOptions(args);

		init();

		try {

			SensorEventPublisher sensor = new SensorEventPublisher(soxServer, soxUser, soxPasswd, soxPubDevice,
					serialPort, debug);
			Thread.sleep(1000 * 10);
			(new Thread(sensor)).start();
			while (true) {
				Thread.sleep(1000 * 10 * 6);
			}
		} catch (TooManyListenersException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static String requestPubDevice(String _soxSever, String _soxUser, String _soxPasswd, String _soxDevice,
			String id) {

		SoxConnection sox = null;
		SoxDevice device = null;

		for (int i = 1; i <= 5; i++) {
			if (i == 1) {
				System.out.print("Connecting to " + _soxSever + "...");
			} else {
				System.out.print("Retry connecting to " + _soxSever + "...");
			}

			try {
				sox = new SoxConnection(_soxSever, false);
				System.out.println("done!");
				break;
			} catch (SmackException | IOException | XMPPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("fail!");
				continue;
			}

		}
		try {
			device = new SoxDevice(sox, _soxDevice);
			device.subscribe();
			device.addSoxEventListener(new CCHClient(id));

			TransducerValue request = new TransducerValue();
			request.setId("REQUEST");
			request.setRawValue("CND");
			request.setTypedValue(id);
			// request.setCurrentTimestamp();

			for (int i = 1; i <= 5; i++) {
				if (i == 1) {
					System.out.print("Requesting publish device  to " + _soxDevice + "...");
				} else {
					System.out.print("Retry requesting publish device to " + _soxDevice + "...");
				}
				request.setCurrentTimestamp();
				device.publishValue(request);
				Thread.sleep(3000);

				if (CCHClient.getPubDevice() != null) {
					System.out.println("done!");
					return CCHClient.getPubDevice();
				} else {
					System.out.println("fail!");
					continue;
				}

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}

		return null;
	}

	protected static class CCHClient implements SoxEventListener {

		String clientId;
		static String pubDevice = null;
		static String message = null;

		public CCHClient(String _id) {
			clientId = _id;
		}

		@Override
		public void handlePublishedSoxEvent(SoxEvent e) {
			List<TransducerValue> values = e.getTransducerValues();
			for (TransducerValue value : values) {
				String id = value.getId();
				if (id.equals("RESPONSE")) {
					String[] response = value.getRawValue().split(",");
					if (!clientId.equals(response[0])) {
						continue;
					}
					if (response[1].equals("CND_SUC")) {
						pubDevice = value.getTypedValue();
					} else {
						message = value.getTypedValue();
					}

				}
			}

		}

		public static String getPubDevice() {
			return pubDevice;
		}

		public static String getMessage() {
			return message;
		}
	}

	protected void init() {
		if (CONFIG_FLAG) {
			debugMSG("Command line parameters are used!");
			return;
		}
		debugMSG("Configuration file parameters are used!");
		Properties prop = new Properties();
		InputStream input = null;
		try {

			debugMSG("Load configuration parameters from " + configFile + "...");
			input = new FileInputStream(configFile);

			prop.load(input);

			soxServer = prop.getProperty("soxServer", soxServer);
			soxUser = prop.getProperty("soxUser", soxUser);
			soxPasswd = prop.getProperty("soxPasswd", soxPasswd);
			soxCCHDevice = prop.getProperty("soxCCHDevice", soxCCHDevice);
			serialPort = prop.getProperty("serialPort", serialPort);
			soxPubDevice = prop.getProperty("soxPubDevice", soxPubDevice);
			phoneNumber = prop.getProperty("phoneNumber", phoneNumber);
			SIMFile = prop.getProperty("SIMFile", SIMFile);
			if (!SIMFile.isEmpty()) {
				phoneNumber = SIMCard.getPhoneNumber(SIMFile, false);
				prop.setProperty("phoneNumber", phoneNumber);
			}
			debugMSG("Done");
			debugMSG("Request publish device from " + soxCCHDevice + " ...");
			String pubDevice = requestPubDevice(soxServer, soxUser, soxPasswd, soxCCHDevice, phoneNumber);

			if (pubDevice != null) {
				soxPubDevice = pubDevice;
				prop.setProperty("soxPubDevice", soxPubDevice);
				debugMSG("Received device name " + soxPubDevice);
			} else {
				debugMSG("No device name received.");
				debugMSG("Use the default one of configuration file.");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		OutputStream output = null;
		try {
			output = new FileOutputStream(configFile);
			prop.store(output, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	protected void parseOptions(String[] args) {
		if (args.length == 0) {
			System.err.println(
					"Usage: java -jar publisher.jar -f <config> -s <soxServer> -u <soxUser> -p <soxPasswd> -d <soxCCHDevice> -sp <serialPort> -debug <true/flase>");
			System.exit(1);
		}
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-d")) {
				soxCCHDevice = args[++i];
				CONFIG_FLAG = true;
			} else if (args[i].equals("-f")) {
				configFile = args[++i];
			} else if (args[i].equals("-s")) {
				soxServer = args[++i];
				CONFIG_FLAG = true;
			} else if (args[i].equals("-u")) {
				soxUser = args[++i];
				CONFIG_FLAG = true;
			} else if (args[i].equals("-p")) {
				soxPasswd = args[++i];
				CONFIG_FLAG = true;
			} else if (args[i].equals("-sp")) {
				serialPort = args[++i];
				CONFIG_FLAG = true;
			} else if (args[i].equals("-debug")) {
				String debugPara = args[++i];
				if (debugPara.equals("true")) {
					debug = true;
				}
			} else {
				System.err.println("ERROR: invalid option " + args[i]);
				System.err.println(
						"Usage: java -jar publisher.jar -s <soxServer> -u <soxUser> -p <soxPasswd> -d <soxCCHDevice> -sp <serialPort> -debug <true/flase>");
				System.exit(1);
			}
		}
	}
}
