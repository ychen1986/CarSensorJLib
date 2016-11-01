package jp.ac.keio.sfc.ht.carsensor.old;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.GSVSentence;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.VTGSentence;
import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.SatelliteInfo;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;

import jp.ac.keio.sfc.ht.carsensor.SensorCMD;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEvent;
import jp.ac.keio.sfc.ht.sox.protocol.TransducerValue;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxConnection;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxDevice;
import jp.ac.keio.sfc.ht.sox.soxlib.event.SoxEvent;
import jp.ac.keio.sfc.ht.sox.soxlib.event.SoxEventListener;


public class CarSensorDecoder extends SensorCMD implements SoxEventListener{
	SoxDevice source = null;
	SoxDevice destination = null;
	static boolean debug = false;
	static final String CLASS_NAME = "CarSensorDecoder";
	static Base64.Decoder decoder = Base64.getDecoder();
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SoxConnection sox = null;
		try {
			sox = new SoxConnection("sox.ht.sfc.keio.ac.jp", false);
		} catch (SmackException | IOException | XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SoxDevice source = null, destination = null;
		try {
			source = new SoxDevice(sox,"FujisawaCarSensorRaw3");
			destination = new SoxDevice(sox,"FujisawaCarSensorTyped3");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		new CarSensorDecoder(source,destination,true);
		while(true){
			;
		}
		
		
	}
	static void debugMSG(String msg) {
		if (debug) {
			System.out.println("[" + CLASS_NAME + "] " + msg);
		}
	}
	
	public CarSensorDecoder(SoxDevice _source, SoxDevice _destination, boolean _debug){
		source = _source;
		destination = _destination;
		source.addSoxEventListener(this);
		source.subscribe();
		debug = _debug;
	}
	public CarSensorDecoder(SoxDevice _source, SoxDevice _destination){
		this(_source, _destination, false);
	}
	
	@Override
	public void handlePublishedSoxEvent(SoxEvent e) {
		// TODO Auto-generated method stub
		debugMSG("New raw data received.");
		List<TransducerValue> values = e.getTransducerValues();
		List<SensorEvent> events = new ArrayList<SensorEvent>();
		for(TransducerValue value: values){
			String id = value.getId();
			SensorEvent event = null ;
			
			if(id.equalsIgnoreCase("Data Event")){
				
				String encodedData = value.getTypedValue();
				String timeStampData = value.getRawValue();
				String timeZoneString = "(.*)(\\+(\\d{2}):((\\d{2})))";
				Pattern timeZonePattern = Pattern.compile(timeZoneString);
				Matcher m = timeZonePattern.matcher(timeStampData);
				String timeZone = null;
				if(m.matches()){
					timeZone = m.group(2);
				}else{
					debugMSG("Timestamp String not mathed!");
					continue;
				}
				debugMSG("Time Zone:" + timeZone);
				
				String[] timeStamps = timeStampData.split("\\"+timeZone);
				byte[] cmds = decoder.decode(encodedData);
				
				int cmd_len = 1 + SensorCMD.EVENT_DATA_A_PARA_SIZE;
				byte[] cmd = new byte[cmd_len]; 
				for(int i = 0 ; i< timeStamps.length; i++){
					
					System.arraycopy(cmds, i * cmd_len, cmd, 0, cmd_len);
					event = parseDataEvent(cmd);
					//event.setTimestamp(timeStamps[i]+timeZone);
					events.add(event);
				}
									
				
			}else if(id.equalsIgnoreCase("Response Event") ){
				
				String encodedData = value.getTypedValue();
				byte[] response = decoder.decode(encodedData);
				event = parseResponse(response);		
				//event.setTimestamp(value.getTimestamp());				
				events.add(event);
			}else if(id.equalsIgnoreCase("GPS Data Event")){
				
				//event = parseGPS(value.getTypedValue());
				//event.setTimestamp(value.getTimestamp());				
				//events.add(event);
			}else{
				debugMSG("Unknown Event: " + value.getTypedValue());
			}
			
			
			for(SensorEvent ev: events){
				debugMSG(ev.getMsg());
				publishSensorEvent(ev);
			}
			events.clear();
			
			
		}
		
		
		
	}
	protected SensorEvent parseGPS(String gpsString) {
		// TODO Auto-generated method stub
		String msg = "GPS Data:";
		if (debug) {
			msg += " " + gpsString;
		}
		msg += "\n";
		Map<String, String> datas = new HashMap<String, String>();
		// TODO Auto-generated method stub
		String[] gpsSenteces = gpsString.split(" ");
		debugMSG("sentences: "+gpsString);
		debugMSG("sentence number: "+gpsSenteces.length);
		SentenceFactory sf = SentenceFactory.getInstance();
		for(String gpsSentence:gpsSenteces){
			debugMSG("sentence: "+gpsSentence);
			if(!gpsSentence.startsWith("$")){
				continue;
			}
			Sentence sen = sf.createParser(gpsSentence);
			try{
			if(sen.isValid()){
				String senid = sen.getSentenceId();
				if(senid.equals("GGA")){
					GGASentence ggaSen = (GGASentence)sen;
					int satelliteCount =  ggaSen.getSatelliteCount();
					msg += "Satellite Number: " + Integer.toString(satelliteCount)+ "\n";
					datas.put("Satellite Number", Integer.toString(satelliteCount));
					Position pos = ggaSen.getPosition();
					msg += "Longitude: " + Double.toString(pos.getLongitude())+ "\n";
					datas.put("Longitude", Double.toString(pos.getLongitude()));
					msg += "Latitude: " + Double.toString(pos.getLatitude())+ "\n";
					datas.put("Latitude", Double.toString(pos.getLatitude()));
					msg += "Altitude: " + Double.toString(pos.getAltitude())+ "\n";
					datas.put("Altitude", Double.toString(pos.getAltitude()));															
					
				}else if(senid.equals("GSV")){
					GSVSentence gsvSen = (GSVSentence)sen;
					//TODO not used
				}else if(senid.equals("VTG")){
					VTGSentence vtgSen = (VTGSentence)sen;
					double sppedKmh = vtgSen.getSpeedKmh();
					msg += "Speed: " + Double.toString(sppedKmh)+ "\n";
					datas.put("Speed", Double.toString(sppedKmh)); 
					
					double coureToGround = vtgSen.getTrueCourse();
					msg += "Cource: " + Double.toString(coureToGround)+ "\n";
					datas.put("Cource", Double.toString(coureToGround)); 					

				}else {
					debugMSG("Unexpected senstence type:" + senid);
					msg += "Unexpected senstence type:" + senid +"\n";
				}
			}else{
				debugMSG("Format is invalid: " + gpsSentence);
				continue;
			}}catch(Throwable t){
				t.printStackTrace();
				continue;
			}
			
		}
		
		
		return new SensorEvent(this, EVENT_DATA_C, null, msg, datas);
	}

	private void publishSensorEvent(SensorEvent event){

		
		for(int i =1; i <=5; i++){
			if(i == 1){
				debugMSG("Publishing sensor data...");
			}else{
				debugMSG("Retry publishing sensor data...");
			}
			
			try{
				destination.publishValues(event.toTranducerValueList());
				debugMSG("done!");
				break;
			}catch (NotConnectedException e1) {
				// TODO Auto-generated catch block
				debugMSG("fail!");
				e1.printStackTrace();
			}
		}
		return;
	}

	protected SensorEvent parseDataEvent(byte[] cmd) {
		// TODO Auto-generated method stub
		SensorEvent sev = null;
		// String msg="";
		switch (cmd[0]) {
		case EVENT_DATA_A:
			sev = dataEventA(cmd);
			break;
		/**
		 * Not implemented yet
		case EVENT_DATA_B:
			sev = dataEventB(cmd);
			break;
			
		*/
		/**
		 * Removed from version 0.1 2015/12/12
		 */
		/*
		 * case EVENT_DATA_ERR: sev = dataEventERR(cmd); break;
		 */
		default:
			System.out.println("Error: Undefined response code! ");
			break;
		}
		
		/*if ( sev != null) {
			debugMSG(sev.getMsg());
		}*/
		return sev;
	}
	protected SensorEvent parseResponse(byte[] cmd) {
		// String msg = "";
		SensorEvent sev = null;
		debugMSG("Sensor Response Received!\n");
		switch (cmd[0]) {
		case RES_INFO:
			sev = responseINFO(cmd);
			break;
		case RES_CMD:
			sev = responseCMD(cmd);
			break;
		case RES_VS:
			sev = responseVS(cmd);
			break;
		case RES_GPS:
			sev = responseGPS(cmd);
			break;
		default:
			System.out.println("Error: Undefined response code!");
			break;
		}
/*		if ( sev != null) {
			debugMSG(sev.getMsg());
		}*/
		return sev;

	}
	/**
	 * @param cmd
	 * @return SensorEvent containing data and message
	 */
	protected SensorEvent dataEventA(byte[] cmd) {
		String msg = "Data Event A:";
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
			msg += "Illuminace: " + illu + "\n";
			datas.put("Illuminace", Double.toString(illu));

			//double v0 = PM25(littleEndianBytesToInt(cmd, offset,
			//		offset + EVENT_DATA_A_PM25_SIZE));
			double alpha = 0.6;
			double beta;
			if(atmoHum > 50.0){
				beta = 1-0.01467*(atmoHum - 50);
			}else{
				beta = 1;
			}
			/*
			double vs = 0;
			if(this.Vs > -50){
				if(atmoTemp <= 40){
					vs = this.Vs - 6 * (this.VsTemp - atmoTemp);
				}else{
					vs = this.Vs - 1.5* (this.VsTemp - atmoTemp);
				}
			}
			//double pm25 = alpha * beta * (v0 - vs);
			
			
			
			short vDiff = (short) ((v0 - vs)*4095/6600);
			
			ByteBuffer buffer = ByteBuffer.allocate(2);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			buffer.putShort(vDiff);
			byte[] a = buffer.array();

			
		    
			cmd[offset] = a[0];
			cmd[offset + 1] = a[1];
			//int ln = cmd.length;	
			//cmd[ln-1] = XorBCCGenerating(cmd, ln-1);
			
			//double vdiff2 = littleEndianBytesToSignedInt(a,0,2);
			//double vdiff3 = littleEndianBytesToSignedInt(cmd, offset,
			//		offset + EVENT_DATA_A_PM25_SIZE);
			 * 
			 */
			double vd =  PM25(littleEndianBytesToSignedInt(cmd, offset,
					offset + EVENT_DATA_A_PM25_SIZE));
			double pm25 = alpha * beta *vd;
			//datas.put("PM2.5-2", Double.toString(pm252));
			//msg += "vd: " + vd + "\n";
			//msg += "vdiff: " + vDiff + "\n";
			//msg += "vdiff2: " + vdiff2 + "\n";
			///msg += "vdiff3: " + vdiff3 + "\n";
			//msg += "a[0]: " + a[0] + "\n";
			//msg += "a[1]: " + a[1] + "\n";
			//msg += "cmd[offset]: " + cmd[offset] + "\n";
			//msg += "cmd[offset + 1]: " + cmd[offset + 1] + "\n";
			
			//msg += "PM2.5test: " + pm25test + "\n";
			msg += "PM2.5: " + pm25 + "\n";
			//msg += "OFFset" + offset + "\n";
			//msg += "cmd.length" + cmd.length + "\n";
			//msg += "V0: " + v0 + "\n";
			//msg += "Vs: " + vs + "\n";
			
			datas.put("PM2.5", Double.toString(pm25));
			
		}
		return new SensorEvent(this, EVENT_DATA_A, cmd, msg, datas);
	}


	
	protected SensorEvent responseCMD(byte[] cmd) {

		String msg = "Command Response:";
		if (debug) {
			msg += " " + bytesToHexString(cmd);
		}
		msg += "\n";
		Map<String, String> datas = new HashMap<String, String>();
		// String hexString = bytesToHex(cmd);
		if (cmd.length != RES_CMD_PARA_SIZE + 1) {
			msg = "Parameter size is illegal!";

		} else {
			byte[] buffer = { cmd[2] };
			String CMDCode = "Command code:" + bytesToHexString(buffer);
			datas.put("Command Code", bytesToHexString(buffer));
			String CMDResult;
			if (RES_CMD_ERR == cmd[1]) {
				datas.put("Command Result", "ABNORMALLY Finished");
				CMDResult = "Command operation finnished ABNORMALLY!";
			} else if (RES_CMD_NOR == cmd[1]) {
				CMDResult = "Command operation finnished NORMALLY!";
				datas.put("Command Result", "NORMALLY Finished");
			} else {
				CMDResult = "Undefined command operation response!";
				datas.put("Command Result", "Undefined Command");
			}

			msg += CMDResult + "\n" + CMDCode + "\n";
		}

		return new SensorEvent(this, RES_CMD, cmd, msg, datas);
	}
	protected SensorEvent responseGPS(byte[] cmd) {

		String msg = "GPS Response:";
		Map<String, String> datas = new HashMap<String, String>();
		if (debug) {
			msg += " " + bytesToHexString(cmd);
		}
		msg += "\n";
		if (cmd.length != RES_GPS_PARA_SIZE + 1) {
			msg += "Parameter size is illegal!";
		} else {
			if (cmd[1] == RES_GPS_FINISHED) {
				msg += "GPS data is available!";
			} else if (cmd[1] == RES_GPS_NOTFINISH) {
				msg += "GPS data is unavailable!";
			} else {
				msg += "Undefined GPS status!";
			}
		}
		msg += "\n";
		return new SensorEvent(this, RES_GPS, cmd, msg, datas);
	}

	protected SensorEvent responseVS(byte[] cmd) {

		String msg = "Vs Response:";
		if (debug) {
			msg += " " + bytesToHexString(cmd);
		}
		msg += "\n";
		Map<String, String> datas = new HashMap<String, String>();
		if (cmd.length != RES_VS_PARA_SIZE + 1) {
			msg = "Parameter size is illegal!";
		} else {
			int offset = 1;

			// byte[] buffer = Arrays.copyOfRange(cmd, offset,
			// offset += RES_VS_VOL_SIZE);

			int Vs = bytesToInt(cmd, offset, offset += RES_VS_VOL_SIZE);
			// byte[] buffer = Arrays.copyOfRange(cmd, offset,
			// offset += RES_VS_TEMP_SIZE);
			double VsTemp = bytesToSignedInt(cmd, offset,
					offset += RES_VS_TEMP_SIZE) * 0.1;
			/*
			 * DecimalFormat formatter = new DecimalFormat("##.##",
			 * DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			 * formatter.setRoundingMode(RoundingMode.HALF_UP);
			 * 
			 * String vsTemp = formatter.format(vsTempInt);
			 */
			msg += "Vs value: " + Vs + "\n";
			msg += "Vs temprature: " + VsTemp + "\n";
			datas.put("Vs", Integer.toString(Vs));
			datas.put("Vs Temperature", String.valueOf(VsTemp));
		}
		return new SensorEvent(this, RES_VS, cmd, msg, datas);
	}

	protected SensorEvent responseINFO(byte[] cmd) {

		String msg = "Sensor information Response:";
		if (debug) {
			msg += " " + bytesToHexString(cmd);
		}
		Map<String, String> datas = new HashMap<String, String>();
		msg += "\n";
		// String hexString = bytesToHex(cmd);
		if (cmd.length != RES_INFO_PARA_SIZE + 1) {
			msg += "Parameter size is illegal!";

		} else {
			int offset = 1;

			String serialNo = bytesToUTF8String(cmd, offset,
					offset += RES_INFO_SERIAL_NO_SIZE);
			msg += "Serial No: " + serialNo + "\n";
			datas.put("Serial Number", serialNo);
			// TODO confirm softversion format
			// BigInteger bi = new BigInteger(buffer);
			// String softVersion = bi.toString();
			/*
			 * String softVersion = bytesToUTF8String(cmd, offset, offset +=
			 * RES_INFO_SOFT_VERSION_SIZE);
			 */
			String softVersion = bytesToUTF8String(cmd, offset,
					offset += RES_INFO_SOFT_VERSION_SIZE);

			msg += "Software vesion: " + softVersion + "\n";
			datas.put("Software Version", softVersion);
			String modelName = bytesToUTF8String(cmd, offset,
					offset += RES_INFO_MODEL_SIZE);
			datas.put("Model Name", modelName);
			msg += "Sensor model: " + modelName + "\n";

		}

		return new SensorEvent(this, RES_INFO, cmd, msg, datas);

	}

}
