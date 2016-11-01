package jp.ac.keio.sfc.ht.carsensor.sox;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
		if (soxServer != null ){
			try {
				debugMSG("Connect to sox server " + soxServer + "...");
				con = new SoxConnection(soxServer, soxUser, soxPasswd, false);
			} catch (SmackException | IOException | XMPPException e) {
				// TODO Auto-generated catch block
				
				System.err.println("Cannot connect to SoxServer" + soxServer );
				e.printStackTrace();
				System.exit(-1);
			}
			debugMSG("Done!");
		}
		
		for(int i = 0; i <= 100; i++){
			
			String deviceName = "carsensor"+String.format("%03d", i)+"_100Hz";
			createNewTypedDevice(deviceName);
		}
	}
	static boolean createNewTypedDevice(String deviceName) {
		String[] dataIds = { "Acceleration X", "Acceleration Y",
				"Acceleration Z", "Angular Velocity X", "Angular Velocity Y",
				"Angular Velocity Z", "Geomagnetism X", "Geomagnetism Y",
				"Geomagnetism Z", "Atmospheric Pressure",
				"Atmospheric Temperature", "Illuminance", "PM2.5",
				"Satellite Number", "Longitude", "Latitude", "Altitude",
				"Speed", "Cource", };
		return createNewTypedDevice(deviceName, dataIds);
	}

	private static boolean createNewTypedDevice(String deviceName,
			String[] dataIds) {

		Device device = new Device();
		device.setId(deviceName);
		device.setDeviceType(DeviceType.OUTDOOR_WEATHER);
		device.setName(deviceName);

		List<Transducer> transducers = new ArrayList<Transducer>();

		for (String s : dataIds) {
			Transducer metaValue = new Transducer();
			metaValue.setName(s);
			metaValue.setId(s);
			transducers.add(metaValue);
		}

		device.setTransducers(transducers);

		for (int i = 0; i < 5; i++) {
			if (i == 0) {
				debugMSG("Create new typed device " + deviceName + "...");
			} else {
				debugMSG("Create new typed device " + deviceName + "...");
			}
			try {
			
				con.createNode(deviceName, device, AccessModel.open,
						PublishModel.open);
				debugMSG("done!");

				return true;
			} catch (NoResponseException | XMPPErrorException
					| NotConnectedException e) {
				// TODO Auto-generated catch block
				debugMSG("fail!");
				e.printStackTrace();
//				try {
//					//Thread.sleep(1000);
//					con.deleteNode("deviceName");
//				} catch (NoResponseException | XMPPErrorException
//						| NotConnectedException | InterruptedException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
				
				
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
			SimpleDateFormat sdfDate = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss.SSSS");// dd/MM/yyyy

			String strDate = sdfDate.format(now);

			System.out.println("[" + sdfDate.format(now) + "] " + "["
				 + "] " + msg);
		}
	}

	static void debugMSG(String msg) {
		if (debug) {
			Date now = new Date();
			SimpleDateFormat sdfDate = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss.SSSS");// dd/MM/yyyy

			String strDate = sdfDate.format(now);

			System.out.println("[" + sdfDate.format(now) + "] " + "["
					+ "] " + msg);
		}
	}
	protected static void parseOptions(String[] args) {
		
		String usage =  "Usage: java -jar CreateSoxDevice.jar -s <soxServer> -u <soxUser> -p <soxPasswd> -debug <true/flase>";
		if (args.length == 0) {
			System.err.println("ERROR: arguments required!");
			System.err
					.println(usage);
			System.exit(1);
		}
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-s")) {
				soxServer = args[++i];
			} else if (args[i].equals("-u")) {
				soxUser = args[++i];
			} else if (args[i].equals("-p")) {
				soxPasswd = args[++i];
			} else if(args[i].equals("-debug")){
				String debugPara= args[++i];
				if (debugPara.equals("true")){
					debug = true;
				}
			}else {
				System.err.println("ERROR: invalid option " + args[i]);
				System.err
						.println(usage);
				System.exit(1);
			}
		}
	}

}
