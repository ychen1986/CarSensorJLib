package jp.ac.keio.sfc.ht.carsensor.old;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.PublishModel;

import jp.ac.keio.sfc.ht.carsensor.SIMCard;
import jp.ac.keio.sfc.ht.sox.protocol.Device;
import jp.ac.keio.sfc.ht.sox.protocol.DeviceType;
import jp.ac.keio.sfc.ht.sox.protocol.Transducer;
import jp.ac.keio.sfc.ht.sox.protocol.TransducerValue;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxConnection;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxDevice;
import jp.ac.keio.sfc.ht.sox.soxlib.event.SoxEvent;
import jp.ac.keio.sfc.ht.sox.soxlib.event.SoxEventListener;

public class CarSensorCCH implements SoxEventListener {
	private static final String CLASS_NAME = "CarSensorCCH";
	private static boolean debug = false;
	private static boolean CONFIG_FLAG = false; // CONFIG_FLAG = ture indicates
												// that command
												// line parameters will be used
												// rather than those of
												// configure file.

	private String configFile = CLASS_NAME + ".config";
	private String soxServer = "sox.ht.sfc.keio.ac.jp";
	private String soxUser = "guest";
	private String soxPasswd = "miroguest";
	private String soxCCHDevice = "FujiCarCCH";
	private String sensorDeviceTable = "sensor_device.table";
	private SoxConnection soxConnection = null;
	private SoxDevice CCHDevice = null;

	static void debugMSG(String msg) {
		if (debug) {
			System.out.println("[" + (new Date()).toString() + "] "+"[" + CLASS_NAME + "] " + msg);
		}
	}

	CarSensorCCH(String[] args) {
		parseOptions(args);
		init();
		connectToSox();
		if (CCHDevice != null) {
			debugMSG("Subscribe event from " + soxCCHDevice + "...");
			CCHDevice.subscribe();
			CCHDevice.addSoxEventListener(this);
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
			sensorDeviceTable = prop.getProperty("sensorDeviceTable",
					sensorDeviceTable);
			prop.setProperty("soxServer", soxServer);
			prop.setProperty("soxUser", soxUser);
			prop.setProperty("soxPasswd", soxPasswd);
			prop.setProperty("soxCCHDevice", soxCCHDevice);
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
		OutputStream output = null;
		try {
			debugMSG("Store configuration parameters to " + configFile + "...");
			output = new FileOutputStream(configFile);
			prop.store(output, null);
			debugMSG("Done!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			debugMSG("Fail!");
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

	public static String typedDeviceName(String nodeName){
		Pattern rawNodeParttern = Pattern.compile("([a-zA-Z]+)(Raw)(\\d+)");
		Matcher m = rawNodeParttern.matcher(nodeName);
		if (m.matches()){
			String typedNodeName = m.group(1) + "Typed"+m.group(3);
			return typedNodeName;
		}else{
			return nodeName + "Typed";
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

		for (int i = 1; i <= 5; i++) {
			if (i == 1) {
				debugMSG("Creat CCH device " + soxCCHDevice + "...");
			} else {
				debugMSG("Retry creat CCH device " + soxCCHDevice + "...");
			}

			try {
				CCHDevice = new SoxDevice(soxConnection, soxCCHDevice);
				debugMSG("Done!");
				break;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				debugMSG("Fail!");
				e.printStackTrace();

				continue;
			}

		}

	}

	@Override
	public void handlePublishedSoxEvent(SoxEvent e) {

		// debugMSG("New sox event: " + e.toString());
		List<TransducerValue> values = e.getTransducerValues();

		for (TransducerValue value : values) {
			debugMSG("Id: " + value.getId());
			debugMSG("Raw value: " + value.getRawValue());
			debugMSG("Typed value: " + value.getTypedValue());
			if (value.getId().equals("REQUEST")) {
				debugMSG("New Request " + value.toString());
				TransducerValue response = handleRequest(value);
				
				try {
					CCHDevice.publishValue(response);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}

		}
	}

	private TransducerValue handleRequest(TransducerValue request) {
		debugMSG("Repquest received!");
		TransducerValue response;
		String requestCMD = request.getRawValue();
		if (requestCMD.equals("CND")) {
			response = createNewDevice(request);

		} else {
			response = unknownRequest(request);

		}

		debugMSG("Repquest handled!");

		return response;
	}

	private TransducerValue unknownRequest(TransducerValue request) {
		// TODO Auto-generated method stub
		TransducerValue response = new TransducerValue();
		response.setId("RESPONSE");
		response.setRawValue("[Unknown Request:" + request.getRawValue() + "]");
		response.setTypedValue(request.getTypedValue());
		response.setCurrentTimestamp();
		return response;

	}

	private TransducerValue createNewDevice(TransducerValue request) {

		SoxConnection con = null;
		// connect to sox server
		for (int i = 1; i <= 5; i++) {
			if (i == 1) {
				debugMSG("Connect to sox server " + soxServer + "...");
			} else {
				debugMSG("Retry connect to sox server " + soxServer + "...");
			}

			try {
				con = new SoxConnection(soxServer, soxUser, soxPasswd, false);
				debugMSG("Done!");
				break;
			} catch (SmackException | IOException | XMPPException e) {
				// TODO Auto-generated catch block
				debugMSG("Fail!");
				e.printStackTrace();
				continue;
			}

		}

		TransducerValue response = new TransducerValue();
		// Get request parameter
		String requestPara = request.getTypedValue();
		String arr[] = requestPara.split(" ", 2);

		String sensorID = arr[0]; // id of the sensor node. Currently, the phone
									// number of the SIM Card is used
		// String other = arr[1];
		// Load sensor-device table
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

		// Search device name with sensorID
		String nodeName = prop.getProperty(sensorID);

		if (nodeName == null) {
			debugMSG("Item for "+sensorID + " not found in "+sensorDeviceTable);
			// The corresponding sensor ID does not exist.
			// Get the current number of devices registered
			String total = prop.getProperty("total");
			if (total == null) {
				total = "0";
			}
			int i = Integer.parseInt(total);
			i++;
			total = Integer.toString(i);
			// Create a new device name;
			nodeName = "FujisawaCarSensorRaw" + total;
			
			// Add it to the table
			prop.setProperty(sensorID, nodeName);
			prop.setProperty("total", total);
			// Store the new table to sensorDeviceTable file
			OutputStream output = null;
			try {
				debugMSG("Restore sensor device table.");
				output = new FileOutputStream(sensorDeviceTable);
				prop.store(output, null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			} finally {
				if (output != null) {
					try {
						output.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}

		}
		debugMSG("Item for "+sensorID + " found in "+sensorDeviceTable + " as " + nodeName);
		// Try to create a new device with the nodeName
		Device device = new Device();
		device.setId(nodeName);
		device.setDeviceType(DeviceType.OUTDOOR_WEATHER);
		device.setName(nodeName);

		List<Transducer> transducers = new ArrayList<Transducer>();

		Transducer resp = new Transducer();
		resp.setName("Response Event");
		resp.setId("Response Event");
		transducers.add(resp);
		
		Transducer gps = new Transducer();
		gps.setName("GPS Data Event");
		gps.setId("GPS Data Event");
		transducers.add(gps);

		Transducer data = new Transducer();
		data.setName("Data Event");
		data.setId("Data Event");
		transducers.add(data);

		device.setTransducers(transducers);

		// create node

		try {
			debugMSG("Create new device " + nodeName + "...");
			con.createNode(nodeName, device, AccessModel.open,
					PublishModel.open);
			response.setId("RESPONSE");
			response.setRawValue(sensorID + ",CND_SUC");
			response.setTypedValue(nodeName);
			response.setCurrentTimestamp();
			debugMSG("Done!");						
			
			
			
		} catch (NoResponseException | XMPPErrorException
				| NotConnectedException e) {
			if (e instanceof XMPPErrorException) {
				XMPPError err = ((XMPPErrorException) e).getXMPPError();
				if (err.getCondition() == XMPPError.Condition.conflict) {
					response.setId("RESPONSE");
					response.setRawValue(sensorID + ",CND_SUC");
					response.setTypedValue(nodeName);
					response.setCurrentTimestamp();
				}
			} else {

				debugMSG("Fail!");
				e.printStackTrace();
				response.setId("RESPONSE");
				response.setRawValue(sensorID + ",CND_FAIL");
				// response.setTypedValue(sensorName);
				response.setCurrentTimestamp();
				;
				debugMSG("Device  " + nodeName + " !");
			}

		}
		String typedNodeName = typedDeviceName(nodeName);
		debugMSG("Typed node name: " + typedNodeName);
		createNewTypedDevice(con, typedNodeName);
		        
/*		if(debug){
        		if (m.matches()) {
                System.out.println(nodeName + " matches;");
                for(int i = 1; i <= m.groupCount(); i++){
                		System.out.println(m.group(i));;
                }
                	
        		} else {
                System.out.println(nodeName + " does not match.");
            }
        }*/
		

		
		con.disconnect();
		con = null;
		return response;
		
	}

	private void createNewTypedDevice(SoxConnection con, String typedNodeName) {
		// TODO Auto-generated method stub
		Device device = new Device();
		device.setId(typedNodeName);
		device.setDeviceType(DeviceType.OUTDOOR_WEATHER);
		device.setName(typedNodeName);

		List<Transducer> transducers = new ArrayList<Transducer>();

		String[] dataIds = {"Acceleration X",
				"Acceleration Y",
				"Acceleration Z",
				"Angular Velocity X",
				"Angular Velocity Y",
				"Angular Velocity Z",
				"Geomagnetism X",
				"Geomagnetism Y",
				"Geomagnetism Z",
				"Atmospheric Pressure",
				"Atmospheric Temperature",
				"Illuminace",
				"PM2.5",
				"Satellite Number", 
				"Longitude",
				"Latitude",
				"Altitude",
				"Speed",
				"Cource",				
				};
		for(String s: dataIds){
			Transducer metaValue = new Transducer();
			metaValue.setName(s);
			metaValue.setId(s);
			transducers.add(metaValue);
		}
		
		
		device.setTransducers(transducers);

		
		debugMSG("Create new typed device " + typedNodeName + "...");
			try {
				con.createNode(typedNodeName, device, AccessModel.open,
						PublishModel.open);
				debugMSG("done!");
			} catch (NoResponseException | XMPPErrorException
					| NotConnectedException e) {
				// TODO Auto-generated catch block
				debugMSG("fail!");
				e.printStackTrace();
			}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CarSensorCCH cch = new CarSensorCCH(args);
		while (true) {
			try {
				Thread.sleep(1000*60);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			};
		}
	}

	protected void parseOptions(String[] args) {
		if (args.length == 0) {
			System.err
					.println("Usage: java "
							+ CLASS_NAME
							+ " -f <config> -s <soxServer> -u <soxUser> -p <soxPasswd> -d <soxCCHDevice> -t <sensorDeviceTable> -debug <true/flase>");
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
								+ " -f <config> -s <soxServer> -u <soxUser> -p <soxPasswd> -d <soxCCHDevice> -t <sensorDeviceTable> -debug <true/flase>");
				System.exit(1);
			}
		}
	}
}
