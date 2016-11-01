package jp.ac.keio.sfc.ht.carsensor.old;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import jp.ac.keio.sfc.ht.sox.soxlib.SoxConnection;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxDevice;

public class CarSensorServer {
	private static final String CLASS_NAME = "CarSensorServer";
	private String configFile = "CarSensorCCH.config";
	private String soxServer = "sox.ht.sfc.keio.ac.jp";
	private static boolean debug = true;
	private String soxUser = "guest";
	private String soxPasswd = "miroguest";
	private String sensorDeviceTable = "sensor_device.table";
	private static  boolean CONFIG_FLAG = false;
	private Map<String, CarSensorDecoder> sensorList = new HashMap<String, CarSensorDecoder>() ;
	private SoxConnection soxConnection = null;
	
	public CarSensorServer(String[] args) {
		// TODO Auto-generated constructor stub
		parseOptions(args);
		init();
		connectToSox();
		loadSensors();
	}
	private void loadSensors() {
		InputStream input = null;
		Properties prop = new Properties();
		try {
			debugMSG("Load sensor device table.");
			input = new FileInputStream(sensorDeviceTable);
			prop.load(input);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		for(Object value : prop.values()){
			String rawDataSensor = (String) value;
			if(sensorList.containsKey(rawDataSensor)){
				continue;
			}
			if(!rawDataSensor.matches("([a-zA-Z]+)(\\d+)")){
				continue;
			}
			debugMSG("Add new sensor "+ rawDataSensor +" to the decoding list");
			String sourceName = rawDataSensor;
			
			String destinationName = CarSensorCCH.typedDeviceName(rawDataSensor);
			
			SoxDevice sourceDevice = null, destinationDevice = null;
			
			for (int i = 1; i <= 5; i++){
				try {
					if(i == 1){
						debugMSG("Create source soxdevice "+sourceName);
					}else{
						debugMSG("Retry to create source soxdevice "+sourceName);
					}
					sourceDevice = new SoxDevice(soxConnection, sourceName);
					debugMSG("done");
					break;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					debugMSG("fail");
					e.printStackTrace();
					
				}
			}
			for (int i = 1; i <= 5; i++){
				try {
					if(i == 1){
						debugMSG("Create destiantion soxdevice "+destinationName);
					}else{
						debugMSG("Retry to create destiantion soxdevice "+destinationName);
					}
					destinationDevice = new SoxDevice(soxConnection, destinationName);
					debugMSG("done");
					break;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					debugMSG("fail");
					e.printStackTrace();
				}
			}
			
			if(sourceDevice != null && destinationDevice != null){
				debugMSG("Create decoder");
				CarSensorDecoder decoder = new  CarSensorDecoder(sourceDevice, destinationDevice,debug);
				debugMSG("done.");
				sensorList.put(rawDataSensor, decoder);
				debugMSG("done.");
				continue;
			}
			
			
		}
		
		
		
	}
	protected void parseOptions(String[] args) {
		if (args.length == 0) {
			return;
		}
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-f")) {
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
			} else if (args[i].equals("-t")) {
				sensorDeviceTable = args[++i];
				CONFIG_FLAG = true;
			} else if (args[i].equals("-debug")) {
				String debugPara = args[++i];
				if (debugPara.equals("true")) {
					debug = true;
				}
			} else {
				System.err.println("ERROR: invalid option " + args[i]);
				System.err
						.println("Usage: java "
								+ CLASS_NAME
								+ " -f <config> -s <soxServer> -u <soxUser> -p <soxPasswd>  -t <sensorDeviceTable> -debug <true/flase>");
				System.exit(1);
			}
		}
	}
	
	static void debugMSG(String msg) {
		if (debug) {
			System.out.println("[" + CLASS_NAME + "] " + msg);
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
			sensorDeviceTable = prop.getProperty("sensorDeviceTable",
					sensorDeviceTable);
			prop.setProperty("soxServer", soxServer);
			prop.setProperty("soxUser", soxUser);
			prop.setProperty("soxPasswd", soxPasswd);
			prop.setProperty("sensorDeviceTable", sensorDeviceTable);
			debugMSG("Done!");

		} catch (Exception ex) {
			debugMSG("Fail!");
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

	}
	private void connectToSox() {

		for (int i = 1; i <= 5; i++) {
			if (i == 1) {
				debugMSG("Connect to sox server " + soxServer + "...");
			} else {
				debugMSG("Retry connect to sox server " + soxServer + "...");
			}

			try {
				soxConnection = new SoxConnection(soxServer, false);
				debugMSG("Done!");
				break;
			} catch (SmackException | IOException | XMPPException e) {
				// TODO Auto-generated catch block
				debugMSG("Fail!");
				e.printStackTrace();
				continue;
			}

		}



	}
	public static void main(String[] args) {
		
		CarSensorServer ser = new CarSensorServer(args);
		while(true){
			try {
				Thread.sleep(1000*60);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			ser.loadSensors();
		}
	}

}
