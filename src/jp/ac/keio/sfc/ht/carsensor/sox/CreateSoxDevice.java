package jp.ac.keio.sfc.ht.carsensor.sox;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.ac.keio.sfc.ht.sox.protocol.Device;
import jp.ac.keio.sfc.ht.sox.protocol.DeviceType;
import jp.ac.keio.sfc.ht.sox.protocol.Transducer;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxConnection;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.PublishModel;

public class CreateSoxDevice {

	static String soxServer = null;
	static String soxUser = null;
	static String soxPasswd = null;
	static SoxConnection con = null;
	static boolean debug = false;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		parseOptions(args);
		if (soxServer != null) {
			try {
				debugMSG("Connect to sox server " + soxServer + "...");
				con = new SoxConnection(soxServer, soxUser, soxPasswd, false);
				// con = new SoxConnection(soxServer, false);
			} catch (SmackException | IOException | XMPPException e) {
				// TODO Auto-generated catch block

				System.err.println("Cannot connect to SoxServer " + soxServer);
				e.printStackTrace();
				System.exit(-1);
			}
			debugMSG("Done!");
		}
		// createNewTypedDevice("carsensor_test");
		/*
		 * createNewTypedDevice("carsensor026_replay");
		 * createNewTypedDevice("carsensor027_replay");
		 * createNewTypedDevice("carsensor029_replay");
		 * createNewTypedDevice("carsensor030_replay");
		 * createNewTypedDevice("carsensor031_replay");
		 * createNewTypedDevice("carsensor033_replay");
		 */
		for (int i = 0; i <= 100; i++) {

			 String deviceName = "carsensor"+String.format("%03d",
			 i)+"_100Hz";
			//String deviceName = "carsensor" + String.format("%03d", i)
			createNewTypedDevice(deviceName);
		}
	}

	private static final Map<String, String> metaDatasMap;
	static {
		Map<String, String> metaDataMap = new HashMap();
		metaDataMap.put("Data Index", "");
		metaDataMap.put("Serial Number", "");
		metaDataMap.put("Acceleration X", "m/s^2");
		metaDataMap.put("Acceleration Y", "m/s^2");
		metaDataMap.put("Acceleration Z", "m/s^2");
		metaDataMap.put("Angular Velocity X", "rad/s");
		metaDataMap.put("Angular Velocity Y", "rad/s");
		metaDataMap.put("Angular Velocity Z", "rad/s");
		metaDataMap.put("Geomagnetism X", "\u00B5T");
		metaDataMap.put("Geomagnetism Y", "\u00B5T");
		metaDataMap.put("Geomagnetism Z", "\u00B5T");
		metaDataMap.put("Atmospheric Pressure", "hPa");
		metaDataMap.put("Atmospheric Temperature", "°C");
		metaDataMap.put("Atmospheric Humidity", "%RH");
		metaDataMap.put("UV", "W/m^2");
		metaDataMap.put("Illuminance", "Lx");
		metaDataMap.put("PM2.5", "\u00B5g/m^3");
		metaDataMap.put("Satellite Number", "");
		metaDataMap.put("Longitude", "");
		metaDataMap.put("Latitude", "");
		metaDataMap.put("Altitude", "m");
		metaDataMap.put("Speed", "km/hr");
		metaDataMap.put("Cource", "°");

		metaDatasMap = Collections.unmodifiableMap(metaDataMap);
	}
	/*
	 * static boolean createNewTypedDevice(String deviceName) { String[] dataIds
	 * = { "Data Index", "Serial Number", "Acceleration X", "Acceleration Y",
	 * "Acceleration Z", "Angular Velocity X", "Angular Velocity Y",
	 * "Angular Velocity Z", "Geomagnetism X", "Geomagnetism Y",
	 * "Geomagnetism Z", "Atmospheric Pressure", "Atmospheric Temperature",
	 * "Atmospheric Humidity", "UV", "Illuminance", "PM2.5", "Satellite Number",
	 * "Longitude", "Latitude", "Altitude", "Speed", "Cource"}; return
	 * createNewTypedDevice(deviceName, dataIds); }
	 */

	// private static boolean createNewTypedDevice(String deviceName,
	// String[] dataIds) {
	static boolean createNewTypedDevice(String deviceName) {
		Device device = new Device();
		device.setId(deviceName);
		device.setDeviceType(DeviceType.OUTDOOR_WEATHER);
		device.setName(deviceName);

		List<Transducer> transducers = new ArrayList<Transducer>();
		for (Map.Entry<String, String> entry : metaDatasMap.entrySet()) {
			Transducer metaValue = new Transducer();
			metaValue.setName(entry.getKey());
			metaValue.setId(entry.getKey());
			metaValue.setUnits(entry.getValue());
			transducers.add(metaValue);

		}
		/*
		 * for (String s : dataIds) { Transducer metaValue = new Transducer();
		 * metaValue.setName(s); metaValue.setId(s); metaValue.setUnits(arg0);
		 * transducers.add(metaValue); }
		 */

		device.setTransducers(transducers);

		for (int i = 0; i < 5; i++) {
			if (i == 0) {
				debugMSG("Create new typed device " + deviceName + "...");
			} else {
				debugMSG("Create new typed device " + deviceName + "...");
			}
			try {

				con.createNode(deviceName, device, AccessModel.open, PublishModel.open);
				debugMSG("done!");

				return true;
			} catch (NoResponseException | XMPPErrorException | NotConnectedException e) {
				// TODO Auto-generated catch block
				debugMSG("fail!");
				e.printStackTrace();
				// try {
				// //Thread.sleep(1000);
				// con.deleteNode("deviceName");
				// } catch (NoResponseException | XMPPErrorException
				// | NotConnectedException | InterruptedException e1) {
				// // TODO Auto-generated catch block
				// e1.printStackTrace();
				// }

			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;

	}

	static void infoMSG(String msg) {
		{
			Date now = new Date();
			SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");// dd/MM/yyyy

			String strDate = sdfDate.format(now);

			System.out.println("[" + sdfDate.format(now) + "] " + "[" + "] " + msg);
		}
	}

	static void debugMSG(String msg) {
		if (debug) {
			Date now = new Date();
			SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");// dd/MM/yyyy

			String strDate = sdfDate.format(now);

			System.out.println("[" + sdfDate.format(now) + "] " + "[" + "] " + msg);
		}
	}

	protected static void parseOptions(String[] args) {

		String usage = "Usage: java -jar CreateSoxDevice.jar -s <soxServer> -u <soxUser> -p <soxPasswd> -debug <true/flase>";
		if (args.length == 0) {
			System.err.println("ERROR: arguments required!");
			System.err.println(usage);
			System.exit(1);
		}
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-s")) {
				soxServer = args[++i];
			} else if (args[i].equals("-u")) {
				soxUser = args[++i];
			} else if (args[i].equals("-p")) {
				soxPasswd = args[++i];
			} else if (args[i].equals("-debug")) {
				String debugPara = args[++i];
				if (debugPara.equals("true")) {
					debug = true;
				}
			} else {
				System.err.println("ERROR: invalid option " + args[i]);
				System.err.println(usage);
				System.exit(1);
			}
		}
	}

}
