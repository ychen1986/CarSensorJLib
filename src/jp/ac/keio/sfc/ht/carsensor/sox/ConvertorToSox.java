package jp.ac.keio.sfc.ht.carsensor.sox;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.PublishModel;

import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.VTGSentence;
import net.sf.marineapi.nmea.util.Position;
import jp.ac.keio.sfc.ht.carsensor.SensorCMD;
import jp.ac.keio.sfc.ht.carsensor.Utility;
import jp.ac.keio.sfc.ht.carsensor.protocol.CarSensorException;
import jp.ac.keio.sfc.ht.carsensor.protocol.RawSensorData;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEvent;
import jp.ac.keio.sfc.ht.sox.protocol.Device;
import jp.ac.keio.sfc.ht.sox.protocol.DeviceType;
import jp.ac.keio.sfc.ht.sox.protocol.Transducer;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxConnection;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxDevice;

public class ConvertorToSox extends SensorCMD implements Runnable,
		AutoCloseable {
		

	
	
	// Socket socket;
	int publishRate = 100; // Hz 1<= publishRate <=100
	final static int SENSOR_SAMPLE_RATE = 100; // Hz
	

	ObjectInputStream inFromClient = null;
	Socket socket = null;
	//SoxConnection soxConnection = null;
	SoxDevice device = null;
	String sensorNo = null;
	static boolean debug = false;
	
	private static final String CLASS_NAME = "ConvertorToSox";
	//private static String configFile = CLASS_NAME + ".config";
	protected static SoxConnection soxConnection = null;
	protected static String soxServer = "soxfujisawa.ht.sfc.keio.ac.jp";
	protected static String soxUser = "guest";
	protected static String soxPasswd = "miroguest";
	protected static Map<String, SoxDevice> publishDeviceMap = new HashMap<String, SoxDevice>(); 
	
	public ConvertorToSox(){
		super();
	}
	public ConvertorToSox(Socket _socket, ObjectInputStream _inFromClient,
			SoxDevice _device, boolean _debug) {

		this(_socket, _inFromClient, _device, _debug, 100);

	}
	public ConvertorToSox(Socket _socket,  boolean _debug,int _publishRate) {
		this(_socket,"sox.ht.sfc.keio.ac.jp", "guest","miroguest", _debug, _publishRate);
	}
	public ConvertorToSox(Socket _socket, String _soxServer, String _soxUser, String _soxPasswd,  boolean _debug,int _publishRate) {
		super();
		
		socket = _socket;
		soxServer = _soxServer;
		soxUser = _soxUser;
		soxPasswd = _soxPasswd;
		debug = _debug;
		publishRate = _publishRate;
		
		try {
			inFromClient = new ObjectInputStream(socket.getInputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
			
		//connectToDevice();

		

	}
	/**
	 * 
	 * @param data: a raw  data sent from a sensor
	 * @return the text of the serial number 
	 */
	protected static String getSensorNO(RawSensorData data) {
		// TODO Auto-generated method stub
		byte [] cmd = new byte[SensorCMD.EVENT_DATA_A_SERO_SIZE];
		for(int i =0; i < cmd.length; i++){
			cmd[i] = data.cmd[1+i];
		}
		
		
		return SensorCMD.littleEndianBytesToDecimalString(cmd,0,cmd.length);
	}
	public int getPublishRate() {
		return publishRate;
	}

	public void setPublishRate(int publishRate) {
		this.publishRate = publishRate;
	}
	
	public ConvertorToSox(Socket _socket, ObjectInputStream _inFromClient,
			SoxDevice _device, boolean _debug, int _publishRate) {
		super();
		socket = _socket;
		inFromClient = _inFromClient;
		device = _device;
		debug = _debug;
		publishRate = _publishRate;

	}

	
	void publish() {
		
		RawSensorData data = null;
		SensorEvent se = null;
		int publishCount = 0;
		while (true) {
			try {
				data = (RawSensorData) inFromClient.readObject();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				infoMSG("Connection from " + socket.getInetAddress()
						+ " is interrupted!");
				return;
			}
			if(data.cmd[0] != -118){
				data.cmd.hashCode();
			}
			try {
				se = parseDataEvent(data);
				
				switch (se.getEventType()) {
				case EVENT_DATA_C:// A gps Event is always published.
					break;

				case EVENT_DATA_A:// Other data are published with rate = publishRate 
					if (++publishCount % (SENSOR_SAMPLE_RATE / publishRate) == 0) { 
						publishCount = 0;
						break;
					} else {
						
						continue;
					}
				}
				//se.toTranducerValueList();
				
				device.publishValues(se.toTranducerValueList());
				debugMSG(se.toString());
			} catch (CarSensorException e) {				
				System.err.println(e.getMessage());
			} catch (NotConnectedException e) {
				// TODO Auto-generated catch block
				
				reconnectToDevice();
			}

		}
	}

	protected void reconnectToDevice() {
		// TODO Auto-generated method stub
		publishDeviceMap.remove(sensorNaming(sensorNo));
		connectToDevice();
		
	}
	protected void connectToDevice() {
		
		debugMSG("Creat sox device...");
		
		
		
		RawSensorData data = null;
		
		try {
			data = (RawSensorData) inFromClient.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		sensorNo = getSensorNO(data);
		debugMSG("Sensor Number:" + sensorNo);
		
		try {
			device = findSoxDevice(sensorNo);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		debugMSG("done!");
	}
	protected static SensorEvent parseDataEvent(RawSensorData data) throws CarSensorException {
		// TODO Auto-generated method stub
		SensorEvent sev = null;

		// String msg="";
		try{
			
	
		switch (data.getCMDCode()) {
		case EVENT_DATA_A:
			sev = dataEventA(data);
			break;
		case EVENT_DATA_B:
			sev = dataEventB(data);
			break;
		/**
		 * Removed from version 0.1 2015/12/12
		 */
		/*
		 * case EVENT_DATA_ERR: sev = dataEventERR(cmd); break;
		 */
		case EVENT_DATA_C:
			sev = dataEventC(data);
			break;
		default:
			throw new CarSensorException("["+Utility.getFormatedTimestamp(data.time)+"]"+"[Sensor "+data.getSensorSerialNo()+"] "+ "Sensor data with Undefined response code! "
					+ data.toString());
			
		}
		}catch (Exception e){
			switch(data.getCMDCode()){
			case EVENT_DATA_A :
			case EVENT_DATA_B :
				throw new CarSensorException("["+Utility.getFormatedTimestamp(data.time)+"]"+"[Sensor "+data.getSensorSerialNo()+"] "+"Sensor data is in error!");		
			case EVENT_DATA_C:
				e.printStackTrace();
				throw new CarSensorException("["+Utility.getFormatedTimestamp(data.time)+"]"+"[Sensor "+data.getSensorSerialNo()+"] "+ "GSP data is in error!");
			default:
				throw (CarSensorException) e;
			}
			
		}
		/*
		 * if ( sev != null) { debugMSG(sev.getMsg()); }
		 */
		return sev;
	}
	protected static SensorEvent dataEventB(RawSensorData data) {
		byte[] cmd = data.cmd;
		String msg = "Data Event B:";
		if (debug) {
			msg += " " + bytesToHexString(cmd);
		}
		msg += "\n";
		Map<String, String> datas = new HashMap<String, String>();
		// String hexString = bytesToHex(cmd);
		if (cmd.length != EVENT_DATA_B_PARA_SIZE + 1
				&& cmd.length != EVENT_DATA_B_PARA_SIZE) {
			msg += "Parameter size is illegal!";
		} else {
			int offset = 1;
			String serialNo = littleEndianBytesToDecimalString(cmd, offset,
					offset += EVENT_DATA_B_SERO_SIZE);
			msg += "Serial number: " + serialNo + "\n";
			datas.put("Serial Number", serialNo);
			String dataIndex = littleEndianBytesToDecimalString(cmd, offset,
					offset += EVENT_DATA_B_DATA_INDEX_SIZE);
			msg += "Data index: " + dataIndex + "\n";
			datas.put("Data Index", dataIndex);
			if (cmd.length == EVENT_DATA_B_PARA_SIZE + 1) {
				/***
				 * Parse and identify the error message
				 */
				String errMsg = identifyErrFlag(cmd[offset]);
				if (errMsg != null) {
					datas.put("Error Message", errMsg);
					msg += errMsg;
				}
				offset += EVENT_DATA_B_ERR_FLAG_SIZE;
			}
			

			
			

			
			
			double pm25 = PM25(littleEndianBytesToInt(cmd, offset,
					offset += EVENT_DATA_A_PM25_SIZE));
			msg += "PM2.5: " + pm25 + "\n";
			datas.put("PM2.5", Double.toString(pm25));
			double atmoTemp = atmosphericTemperature(littleEndianBytesToInt(
					cmd, offset, offset += EVENT_DATA_B_ATOMS_TEMP_SIZE));
			msg += "Atmospheric Temperature: " + atmoTemp + "\n";
			datas.put("Atmospheric Temperature", Double.toString(atmoTemp));
			
			
			

		}
		return new SensorEvent( EVENT_DATA_B, cmd, msg, datas,data.time);
		

	}
	
	protected static SensorEvent dataEventC(RawSensorData data) throws Exception{
		// TODO Auto-generated method stub
		byte[] cmd = data.cmd;
		String msg = "Data Event C:";
		if (debug) {
			msg += " " + bytesToHexString(cmd);
		}
		msg += "\n";
		Map<String, String> datas = new HashMap<String, String>();
		// String hexString = bytesToHex(cmd);

		int offset = 1;
		String serialNo = littleEndianBytesToDecimalString(cmd, offset,
				offset += EVENT_DATA_C_SERO_SIZE);
		msg += "Serial number: " + serialNo + "\n";
		datas.put("Serial Number", serialNo);
		String dataIndex = littleEndianBytesToDecimalString(cmd, offset,
				offset += EVENT_DATA_C_DATA_INDEX_SIZE);

		msg += "Data index: " + dataIndex + "\n";
		datas.put("Data Index", dataIndex);
		String gpsString = "";

		/***
		 * Parse and identify the error message
		 */
		String errMsg = identifyErrFlag(cmd[offset]);
		if (errMsg != null) {
			datas.put("Error Message", errMsg);
			msg += errMsg;
		}
		offset += EVENT_DATA_C_ERR_FLAG_SIZE;
		offset += EVENT_DATA_C_GPS_DATA_SIZE;
		gpsString = bytesToUTF8String(cmd, offset, cmd.length - 2);

		SentenceFactory sf = SentenceFactory.getInstance();

		Sentence sen = sf.createParser(gpsString);
		try {
			if (sen.isValid()) {
				String senid = sen.getSentenceId();
				if (senid.equals("GGA")) {
					GGASentence ggaSen = (GGASentence) sen;
					int satelliteCount = ggaSen.getSatelliteCount();
					msg += "Satellite Number: "
							+ Integer.toString(satelliteCount) + "\n";
					datas.put("Satellite Number",
							Integer.toString(satelliteCount));
					Position pos = ggaSen.getPosition();
					msg += "Longitude: " + Double.toString(pos.getLongitude())
							+ "\n";
					datas.put("Longitude", Double.toString(pos.getLongitude()));
					msg += "Latitude: " + Double.toString(pos.getLatitude())
							+ "\n";
					datas.put("Latitude", Double.toString(pos.getLatitude()));
					msg += "Altitude: " + Double.toString(pos.getAltitude())
							+ "\n";
					datas.put("Altitude", Double.toString(pos.getAltitude()));

				} else if (senid.equals("GSV")) {
					// GSVSentence gsvSen = (GSVSentence) sen;
					// TODO not used
				} else if (senid.equals("VTG")) {
					VTGSentence vtgSen = (VTGSentence) sen;
					double sppedKmh = vtgSen.getSpeedKmh();
					msg += "Speed: " + Double.toString(sppedKmh) + "\n";
					datas.put("Speed", Double.toString(sppedKmh));

					double coureToGround = vtgSen.getTrueCourse();
					msg += "Cource: " + Double.toString(coureToGround) + "\n";
					datas.put("Cource", Double.toString(coureToGround));

				} else {
					debugMSG("Unexpected senstence type:" + senid);
					msg += "Unexpected senstence type:" + senid + "\n";
				}
			} else {
				debugMSG("Format is invalid: " + gpsString);

			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return new SensorEvent( EVENT_DATA_C, cmd, msg, datas, data.time);

	}
	final static String SENSOR_NAMING_PREFIX = "carsensor";
	public static String sensorNaming(String sensorNo) {
		return SENSOR_NAMING_PREFIX + sensorNo;
	}
	public  SoxDevice findSoxDevice(String sensorNo) throws Exception {
		String deviceName = sensorNaming(sensorNo);
		SoxDevice  dev = publishDeviceMap.get(deviceName);
		if (dev != null)
		{
			return dev;
		}
		
		//if (soxConnection == null){
		//	soxConnection = connectToSox();
		//}
		
		for (int i = 0; i < 5; i++) {
			try {
				if (i == 0) {
					debugMSG("Connect to device " + deviceName + "...");
				}
				dev = new SoxDevice(connectToSox(), deviceName);
				debugMSG("done!");
				publishDeviceMap.put(deviceName, dev);
				return dev;
			} catch (Exception e) {
				debugMSG("failed!");				
				e.printStackTrace();
				if (i == 0) {
					createNewTypedDevice(deviceName);
				}

			}
			Thread.sleep(3000);
		}

		throw new Exception("Creating SoxDevice: "+deviceName+ "failed!");

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
		// TODO Auto-generated method stub
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
				try {
					Thread.sleep(1000 * 3);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				e.printStackTrace();
				continue;
			}

		}
		try {
			con.deleteNode(deviceName);
		} catch (NoResponseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (XMPPErrorException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NotConnectedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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

	public static SoxConnection connectToSox() {

		if(soxConnection != null){
			return soxConnection;
		}
		//SoxConnection  soxConnection = null;
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
		return soxConnection;
	}
	/**
	 * @param cmd
	 * @return SensorEvent containing data and message
	 */
	protected static SensorEvent dataEventA(RawSensorData data) {
		String msg = "Data Event A:";
		byte[] cmd = data.cmd;
		if (debug) {
			msg += " " + bytesToHexString(cmd);
		}
		msg += "\n";
		Map<String, String> datas = new HashMap<String, String>();
		// String hexString = bytesToHex(cmd);
		if (cmd.length != EVENT_DATA_A_PARA_SIZE + 1 // cmd.length ==
														// EVENT_DATA_A_PARA_SIZE
														// + 1 => The final
														// version sensor
				&& cmd.length != EVENT_DATA_A_PARA_SIZE) { // cmd.length ==
															// EVENT_DATA_A_PARA_SIZE
															// => The test
															// version sensor
			msg += "Parameter size is illegal! " + cmd.length;
		} else {
			int offset = 1;
			String serialNo = littleEndianBytesToDecimalString(cmd, offset,
					offset += EVENT_DATA_A_SERO_SIZE);
			msg += "Serial number: " + serialNo + "\n";
			datas.put("Serial Number", serialNo);
			String dataIndex = littleEndianBytesToDecimalString(cmd, offset,
					offset += EVENT_DATA_A_DATA_INDEX_SIZE);
			msg += "Data index: " + dataIndex + "\n";
			datas.put("Data Index", dataIndex);
			if (cmd.length == EVENT_DATA_A_PARA_SIZE + 1) {
				/***
				 * The version 0.1 Sensor Parse and identify the error message
				 */
				String errMsg = identifyErrFlag(cmd[offset]);
				if (errMsg != null) {
					datas.put("Error Message", errMsg);
					msg += errMsg;
				}
				offset += EVENT_DATA_A_ERR_FLAG_SIZE;
			}

			/***
			 * Parse and convert acceleration data
			 */
			double accX = acceleration(littleEndianBytesToSignedInt(cmd,
					offset, offset += EVENT_DATA_A_ACC_X_SIZE));
			msg += "Acceleration_X: " + accX + "\n";
			datas.put("Acceleration X", Double.toString(accX));

			double accY = acceleration(littleEndianBytesToSignedInt(cmd,
					offset, offset += EVENT_DATA_A_ACC_Y_SIZE));
			msg += "Acceleration_Y: " + accY + "\n";
			datas.put("Acceleration Y", Double.toString(accY));

			double accZ = acceleration(littleEndianBytesToSignedInt(cmd,
					offset, offset += EVENT_DATA_A_ACC_Z_SIZE));
			msg += "Acceleration_Z: " + accZ + "\n";
			datas.put("Acceleration Z", Double.toString(accZ));

			/***
			 * Parse and convert angular velocity data
			 */
			double anve_X = angularVelocity(littleEndianBytesToSignedInt(cmd,
					offset, offset += EVENT_DATA_A_ANVE_X_SIZE));
			msg += "Angular_Velocity_X: " + anve_X + "\n";
			datas.put("Angular Velocity X", Double.toString(anve_X));

			double anve_Y = angularVelocity(littleEndianBytesToSignedInt(cmd,
					offset, offset += EVENT_DATA_A_ANVE_Y_SIZE));
			msg += "Angular_Velocity_Y: " + anve_Y + "\n";
			datas.put("Angular Velocity Y", Double.toString(anve_Y));

			double anve_Z = angularVelocity(littleEndianBytesToSignedInt(cmd,
					offset, offset += EVENT_DATA_A_ANVE_Z_SIZE));
			msg += "Angular_Velocity_Z: " + anve_Z + "\n";
			datas.put("Angular Velocity Z", Double.toString(anve_Z));

			/***
			 * Parse and convert geomagnetism data
			 */
			double geomagX = geomagnetism(littleEndianBytesToSignedInt(cmd,
					offset, offset += EVENT_DATA_A_GEMG_X_SIZE));
			msg += "Geomagnetism_X: " + geomagX + "\n";
			datas.put("Geomagnetism X", Double.toString(geomagX));

			double geomagY = geomagnetism(littleEndianBytesToSignedInt(cmd,
					offset, offset += EVENT_DATA_A_GEMG_Y_SIZE));
			msg += "Geomagnetism_Y: " + geomagY + "\n";
			datas.put("Geomagnetism Y", Double.toString(geomagY));

			double geomagZ = geomagnetism(littleEndianBytesToSignedInt(cmd,
					offset, offset += EVENT_DATA_A_GEMG_Z_SIZE));
			msg += "Geomagnetism_Z: " + geomagZ + "\n";
			datas.put("Geomagnetism Z", Double.toString(geomagZ));
			/***
			 * Parse and convert atmospheric and environmental data data
			 */
			double atmoPress = atmosphericPressure(littleEndianBytesToInt(cmd,
					offset, offset += EVENT_DATA_A_ATOMS_PRSR_SIZE));
			msg += "Atmospheric Pressure: " + atmoPress + "\n";
			datas.put("Atmospheric Pressure", Double.toString(atmoPress));

			double atmoHum = atmosphericHumidity(littleEndianBytesToInt(cmd,
					offset, offset += EVENT_DATA_A_ATOMS_HUM_SIZE));
			msg += "Atmospheric Humidity: " + atmoHum + "\n";
			datas.put("Atmospheric Humidity", Double.toString(atmoHum));

			double atmoTemp = atmosphericTemperature(littleEndianBytesToInt(
					cmd, offset, offset += EVENT_DATA_A_ATOMS_TEMP_SIZE));
			msg += "Atmospheric Temperature: " + atmoTemp + "\n";
			datas.put("Atmospheric Temperature", Double.toString(atmoTemp));

			double uv_A = UV_A(littleEndianBytesToSignedInt(cmd, offset,
					offset += EVENT_DATA_A_UV_SIZE));
			msg += "UV: " + uv_A + "\n";
			datas.put("UV", Double.toString(uv_A));

			double illu = illuminance(littleEndianBytesToSignedInt(cmd, offset,
					offset += EVENT_DATA_A_ILLU_SIZE));
			msg += "Illuminance: " + illu + "\n";
			datas.put("Illuminance", Double.toString(illu));

			// double v0 = PM25(littleEndianBytesToInt(cmd, offset,
			// offset + EVENT_DATA_A_PM25_SIZE));
			double alpha = 0.6;
			double beta;
			if (atmoHum > 50.0) {
				beta = 1 - 0.01467 * (atmoHum - 50);
			} else {
				beta = 1;
			}
			/*
			 * double vs = 0; if(this.Vs > -50){ if(atmoTemp <= 40){ vs =
			 * this.Vs - 6 * (this.VsTemp - atmoTemp); }else{ vs = this.Vs -
			 * 1.5* (this.VsTemp - atmoTemp); } } //double pm25 = alpha * beta *
			 * (v0 - vs);
			 * 
			 * 
			 * 
			 * short vDiff = (short) ((v0 - vs)*4095/6600);
			 * 
			 * ByteBuffer buffer = ByteBuffer.allocate(2);
			 * buffer.order(ByteOrder.LITTLE_ENDIAN); buffer.putShort(vDiff);
			 * byte[] a = buffer.array();
			 * 
			 * 
			 * 
			 * cmd[offset] = a[0]; cmd[offset + 1] = a[1]; //int ln =
			 * cmd.length; //cmd[ln-1] = XorBCCGenerating(cmd, ln-1);
			 * 
			 * //double vdiff2 = littleEndianBytesToSignedInt(a,0,2); //double
			 * vdiff3 = littleEndianBytesToSignedInt(cmd, offset, // offset +
			 * EVENT_DATA_A_PM25_SIZE);
			 */
			double vd = PM25(littleEndianBytesToSignedInt(cmd, offset, offset
					+ EVENT_DATA_A_PM25_SIZE));
			double pm25 = alpha * beta * vd;
			// datas.put("PM2.5-2", Double.toString(pm252));
			// msg += "vd: " + vd + "\n";
			// msg += "vdiff: " + vDiff + "\n";
			// msg += "vdiff2: " + vdiff2 + "\n";
			// /msg += "vdiff3: " + vdiff3 + "\n";
			// msg += "a[0]: " + a[0] + "\n";
			// msg += "a[1]: " + a[1] + "\n";
			// msg += "cmd[offset]: " + cmd[offset] + "\n";
			// msg += "cmd[offset + 1]: " + cmd[offset + 1] + "\n";

			// msg += "PM2.5test: " + pm25test + "\n";
			msg += "PM2.5: " + pm25 + "\n";
			// msg += "OFFset" + offset + "\n";
			// msg += "cmd.length" + cmd.length + "\n";
			// msg += "V0: " + v0 + "\n";
			// msg += "Vs: " + vs + "\n";

			datas.put("PM2.5", Double.toString(pm25));

		}
		return new SensorEvent(EVENT_DATA_A, cmd, msg, datas, data.time);
	}

	public static void main(String[] args) {

		ServerSocket welcomeSocket = null;
		try {
			welcomeSocket = new ServerSocket(6222);
			Socket connectionSocket = null;
			connectionSocket = welcomeSocket.accept();
			String soxServer = "sox.ht.sfc.keio.ac.jp";
			String soxDevice = "FujisawaCarSensorTyped4";
			SoxConnection con = new SoxConnection(soxServer, false);
			SoxDevice device = new SoxDevice(con, soxDevice);
			ObjectInputStream out = new ObjectInputStream(
					connectionSocket.getInputStream());
			Thread thread = new Thread(new ConvertorToSox(connectionSocket,out, device, true));
			thread.start();

		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (SmackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		inFromClient.close();
		if (socket != null) {
			socket.close();
		}
		

	}

	static void infoMSG(String msg) {
		{
			Date now = new Date();
			SimpleDateFormat sdfDate = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss.SSSS");// dd/MM/yyyy

			String strDate = sdfDate.format(now);

			System.out.println("[" + sdfDate.format(now) + "] " + "["
					+ CLASS_NAME + "] " + msg);
		}
	}

	static void debugMSG(String msg) {
		if (debug) {
			Date now = new Date();
			SimpleDateFormat sdfDate = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss.SSSS");// dd/MM/yyyy

			String strDate = sdfDate.format(now);

			System.out.println("[" + sdfDate.format(now) + "] " + "["
					+ CLASS_NAME + "] " + msg);
		}
	}
	

	@Override
	public void run() {
		
		connectToDevice();
		// TODO Auto-generated method stub
		infoMSG("Start conversion to " + device.getNodeId());
		try {
			socket.setSoTimeout(1000 * 60 *2);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		publish();

		try {
			infoMSG("Stop conversion to " + device.getNodeId());
			close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
