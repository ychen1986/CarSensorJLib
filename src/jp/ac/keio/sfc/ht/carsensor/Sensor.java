package jp.ac.keio.sfc.ht.carsensor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.ac.keio.sfc.ht.carsensor.protocol.RawSensorData;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEvent;

public abstract class Sensor extends SensorCMD {
	protected int serial;
	protected String softwareVersion;
	protected String modelName;
	protected int Vs = -100;
	protected double VsTemp = 0.0;
	
	protected boolean GPS_Status = false;

	final Logger logger = LoggerFactory.getLogger(Sensor.class);
	
	public boolean isGPSAvailable() {
		return GPS_Status;
	}

	public int getSerial() {
		return serial;
	}

	public Sensor() {
		// TODO Auto-generated constructor stub
		super();

	}

	/***
	 * 
	 * @return true if softwareVersion, modelName, Vs, VsTemp are all
	 *         initialized; false, otherwise.
	 */
	public boolean isInitialized() {
		if (softwareVersion != null && modelName != null && Vs != -100 && VsTemp != 0.0)
			return true;
		else
			return false;

	}

	public String toString() {
		String msg = "";
		msg += "Serial number: " + serial + "\n";
		msg += "Software version: " + softwareVersion + "\n";
		msg += "Model name: " + modelName + "\n";
		msg += "Premeasured Vs value: " + Vs + "\n";
		msg += "Premeasured temperature: " + VsTemp + "\n";
		msg += "Debug status: " + logger.isDebugEnabled() + "\n";
		msg += "GSP status: " + GPS_Status + "\n";

		return msg;

	}

	/**
	 * 
	 * @param data:
	 *            a RawSensorData object containing the raw byte array and the
	 *            timestamp
	 * @return a SensorEvent according to data
	 * @throws Exception
	 */
	protected SensorEvent parse(RawSensorData data) throws Exception {

		SensorEvent sev = null;
		if (isResponse(data.getCMDCode())) {
			sev = parseResponse(data);
		} else if (isEvent(data.getCMDCode())) {
			sev = parseEvent(data);
		} else {
			throw new Exception("Invalid command code: " + bytesToHexString(data.cmd));
		}
		return sev;
	}

	public void init() {
		try {
			stopSensorWithFan();
			Thread.sleep(1000);
			getSensorInfo();
			Thread.sleep(1000);
			getVS();
			// Thread.sleep(5000);
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			logger.error("Sensor init failed",e);
		}

	}

	public abstract void sendCommand(byte[] cmd) throws IOException;

	public void sendCommandWithBCC(byte[] cmd) throws IOException {
		byte[] bcc = { XorBCCGenerating(cmd) };
		sendCommand(cmd);
		sendCommand(bcc);
	}

	public void getSensorInfo() throws IOException {
		logger.info("Get sensor machine information...");
		sendCommand(GET_SENSOR_INFOMATION);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void getVS() throws IOException {
		logger.info("Get Vs value...");
		sendCommand(GET_VS_VALUE);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void getGPSStatus() throws IOException {
		logger.info("Get GPS status...");
		sendCommand(GET_GPS_STATUS);
	}

	public void startSensor() throws IOException {
		startSensorWithFan();
	}

	public void startSensorWithFan() throws IOException {
		logger.info("Start sensing with fan working...");
		sendCommand(START_SENSOR_WITH_FAN);
	}

	public void stopSensorWithFan() throws IOException {
		logger.info("Stop sensing...");
		sendCommand(STOP_SENSOR_WITH_FAN);

	}

	public void startSensorWithoutFan() throws IOException {
		logger.info("Start sensing without fan working...");
		sendCommand(START_SENSOR_WITHOUT_FAN);
	}

	public void stopSensor() throws IOException {
		
			logger.debug("Stop sensing!...");
	
		stopSensorWithFan();
	}

	protected static final Map<Byte, Integer> ParaSizeMap;
	static {
		ParaSizeMap = new HashMap<Byte, Integer>();
		ParaSizeMap.put(EVENT_DATA_A, EVENT_DATA_A_PARA_SIZE);
		ParaSizeMap.put(EVENT_DATA_B, EVENT_DATA_B_PARA_SIZE);
		ParaSizeMap.put(RES_INFO, RES_INFO_PARA_SIZE);
		ParaSizeMap.put(RES_VS, RES_VS_PARA_SIZE);
		ParaSizeMap.put(RES_CMD, RES_CMD_PARA_SIZE);
		ParaSizeMap.put(RES_GPS, RES_GPS_PARA_SIZE);
		ParaSizeMap.put(EVENT_GPS_START, EVENT_GPS_START_PARA_SIZE);
	}

	public static int getParaSize(byte cmd) throws Exception {
		switch (cmd) {
		case EVENT_DATA_A:
			return EVENT_DATA_A_PARA_SIZE;

		case EVENT_DATA_B:
			return EVENT_DATA_B_PARA_SIZE;
		case RES_INFO:
			return RES_INFO_PARA_SIZE;
		case RES_VS:
			return RES_VS_PARA_SIZE;
		case RES_CMD:
			return RES_CMD_PARA_SIZE;
		case RES_GPS:
			return RES_GPS_PARA_SIZE;
		case EVENT_GPS_START:
			return EVENT_GPS_START_PARA_SIZE;
		// TODO don't forget to add code here if a new response or event is
		// added.
		default:
			byte[] cmds = { cmd };
			throw new Exception("Command code " + bytesToHexString(cmds) + "not found!");

		}
	}

	protected SensorEvent responseGPS(RawSensorData data) {
		byte[] cmd = data.cmd;
		String msg = "GPS Response:";
		Map<String, String> datas = new HashMap<String, String>();
		if (logger.isDebugEnabled()) {
			msg += " " + bytesToHexString(cmd);
		}
		msg += "\n";
		if (cmd.length != RES_GPS_PARA_SIZE + 1) {
			msg += "Parameter size is illegal!";
		} else {
			if (cmd[1] == RES_GPS_FINISHED) {
				msg += "GPS data is available!";
				GPS_Status = true;
			} else if (cmd[1] == RES_GPS_NOTFINISH) {
				msg += "GPS data is unavailable!";
				GPS_Status = false;
			} else {
				msg += "Undefined GPS status!";
				GPS_Status = false;
			}
		}
		msg += "\n";
		return new SensorEvent(this, RES_GPS, cmd, msg, datas, data.time);
	}

	protected SensorEvent responseVS(RawSensorData data) {
		byte[] cmd = data.cmd;
		String msg = "Vs Response:";
		if (logger.isDebugEnabled()) {
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
			int softVersion = 1;
			if (this.softwareVersion != null) {
				softVersion = Integer.parseInt(this.softwareVersion);
			}
			if (softVersion < 1) {
				this.Vs = bytesToInt(cmd, offset, offset += RES_VS_VOL_SIZE);
				// byte[] buffer = Arrays.copyOfRange(cmd, offset,
				// offset += RES_VS_TEMP_SIZE);
				this.VsTemp = bytesToSignedInt(cmd, offset, offset += RES_VS_TEMP_SIZE) * 0.1;

			} else {
				this.Vs = littleEndianBytesToInt(cmd, offset, offset += RES_VS_VOL_SIZE);
				// byte[] buffer = Arrays.copyOfRange(cmd, offset,
				// offset += RES_VS_TEMP_SIZE);
				this.VsTemp = littleEndianBytesToSignedInt(cmd, offset, offset += RES_VS_TEMP_SIZE) * 0.1;
			}

			/*
			 * DecimalFormat formatter = new DecimalFormat("##.##",
			 * DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			 * formatter.setRoundingMode(RoundingMode.HALF_UP);
			 * 
			 * String vsTemp = formatter.format(vsTempInt);
			 */
			msg += "Vs value: " + this.Vs + "\n";
			msg += "Vs temprature: " + this.VsTemp + "\n";
			datas.put("Vs", Integer.toString(Vs));
			datas.put("Vs Temperature", String.valueOf(VsTemp));
		}
		return new SensorEvent(this, RES_VS, cmd, msg, datas);
	}

	protected SensorEvent responseINFO(RawSensorData data) {
		byte[] cmd = data.cmd;
		String msg = "Sensor information Response:";
		if (logger.isDebugEnabled()) {
			msg += " " + bytesToHexString(cmd);
		}
		Map<String, String> datas = new HashMap<String, String>();
		msg += "\n";
		// String hexString = bytesToHex(cmd);
		if (cmd.length != RES_INFO_PARA_SIZE + 1) {
			msg += "Parameter size is illegal!";

		} else {
			int offset = 1;

			String serialNo = bytesToUTF8String(cmd, offset, offset += RES_INFO_SERIAL_NO_SIZE);
			this.serial = Integer.parseInt(serialNo);
			msg += "Serial No: " + serialNo + "\n";
			datas.put("Serial Number", serialNo);
			// TODO confirm softversion format
			// BigInteger bi = new BigInteger(buffer);
			// String softVersion = bi.toString();
			/*
			 * String softVersion = bytesToUTF8String(cmd, offset, offset +=
			 * RES_INFO_SOFT_VERSION_SIZE);
			 */
			String softVersion = bytesToUTF8String(cmd, offset, offset += RES_INFO_SOFT_VERSION_SIZE);
			this.softwareVersion = softVersion;
			msg += "Software vesion: " + softVersion + "\n";
			datas.put("Software Version", softVersion);
			String modelName = bytesToUTF8String(cmd, offset, offset += RES_INFO_MODEL_SIZE);
			this.modelName = modelName;
			datas.put("Model Name", modelName);
			msg += "Sensor model: " + modelName + "\n";

		}

		return new SensorEvent(this, RES_INFO, cmd, msg, datas, data.time);

	}

	protected SensorEvent responseCMD(RawSensorData data) {
		byte[] cmd = data.cmd;
		String msg = "Command Response:";
		if (logger.isDebugEnabled()) {
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

		return new SensorEvent(this, RES_CMD, cmd, msg, datas, data.time);
	}

	protected SensorEvent GPSEventSTART(RawSensorData data) {
		// TODO Auto-generated method stub
		byte[] cmd = data.cmd;
		String msg = "GPS Start Event:";
		Map<String, String> datas = new HashMap<String, String>();
		if (logger.isDebugEnabled()) {
			msg += " " + bytesToHexString(cmd);
		}
		msg += "\n";
		// String hexString = bytesToHex(cmd);
		if (cmd.length != EVENT_GPS_START_PARA_SIZE + 1) {
			msg += "Parameter size is illegal!";

		} else {
			if (0 == cmd[1]) {
				GPS_Status = true;
				msg += "GPS data is available.\n";
			}
		}
		return new SensorEvent(this, EVENT_GPS_START, cmd, msg, datas);
	}

	/**
	 * @param cmd
	 * @return SensorEvent containing data and message
	 */
	protected SensorEvent dataEventA(RawSensorData data) {
		String msg = "Data Event A:";
		byte[] cmd = data.cmd;
		if (logger.isDebugEnabled()) {
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
			String serialNo = littleEndianBytesToDecimalString(cmd, offset, offset += EVENT_DATA_A_SERO_SIZE);
			msg += "Serial number: " + serialNo + "\n";
			datas.put("Serial Number", serialNo);
			String dataIndex = littleEndianBytesToDecimalString(cmd, offset, offset += EVENT_DATA_A_DATA_INDEX_SIZE);
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
			double accX = acceleration(littleEndianBytesToSignedInt(cmd, offset, offset += EVENT_DATA_A_ACC_X_SIZE));
			msg += "Acceleration_X: " + accX + "\n";
			datas.put("Acceleration X", Double.toString(accX));

			double accY = acceleration(littleEndianBytesToSignedInt(cmd, offset, offset += EVENT_DATA_A_ACC_Y_SIZE));
			msg += "Acceleration_Y: " + accY + "\n";
			datas.put("Acceleration Y", Double.toString(accY));

			double accZ = acceleration(littleEndianBytesToSignedInt(cmd, offset, offset += EVENT_DATA_A_ACC_Z_SIZE));
			msg += "Acceleration_Z: " + accZ + "\n";
			datas.put("Acceleration Z", Double.toString(accZ));

			/***
			 * Parse and convert angular velocity data
			 */
			double anve_X = angularVelocity(
					littleEndianBytesToSignedInt(cmd, offset, offset += EVENT_DATA_A_ANVE_X_SIZE));
			msg += "Angular_Velocity_X: " + anve_X + "\n";
			datas.put("Angular Velocity X", Double.toString(anve_X));

			double anve_Y = angularVelocity(
					littleEndianBytesToSignedInt(cmd, offset, offset += EVENT_DATA_A_ANVE_Y_SIZE));
			msg += "Angular_Velocity_Y: " + anve_Y + "\n";
			datas.put("Angular Velocity Y", Double.toString(anve_Y));

			double anve_Z = angularVelocity(
					littleEndianBytesToSignedInt(cmd, offset, offset += EVENT_DATA_A_ANVE_Z_SIZE));
			msg += "Angular_Velocity_Z: " + anve_Z + "\n";
			datas.put("Angular Velocity Z", Double.toString(anve_Z));

			/***
			 * Parse and convert geomagnetism data
			 */
			double geomagX = geomagnetism(
					littleEndianBytesToSignedInt(cmd, offset, offset += EVENT_DATA_A_GEMG_X_SIZE));
			msg += "Geomagnetism_X: " + geomagX + "\n";
			datas.put("Geomagnetism X", Double.toString(geomagX));

			double geomagY = geomagnetism(
					littleEndianBytesToSignedInt(cmd, offset, offset += EVENT_DATA_A_GEMG_Y_SIZE));
			msg += "Geomagnetism_Y: " + geomagY + "\n";
			datas.put("Geomagnetism Y", Double.toString(geomagY));

			double geomagZ = geomagnetism(
					littleEndianBytesToSignedInt(cmd, offset, offset += EVENT_DATA_A_GEMG_Z_SIZE));
			msg += "Geomagnetism_Z: " + geomagZ + "\n";
			datas.put("Geomagnetism Z", Double.toString(geomagZ));
			/***
			 * Parse and convert atmospheric and environmental data data
			 */
			double atmoPress = atmosphericPressure(
					littleEndianBytesToInt(cmd, offset, offset += EVENT_DATA_A_ATOMS_PRSR_SIZE));
			msg += "Atmospheric Pressures: " + atmoPress + "\n";
			datas.put("Atmospheric Pressures", Double.toString(atmoPress));

			double atmoHum = atmosphericHumidity(
					littleEndianBytesToInt(cmd, offset, offset += EVENT_DATA_A_ATOMS_HUM_SIZE));
			msg += "Atmospheric Humidity: " + atmoHum + "\n";
			datas.put("Atmospheric Humidity", Double.toString(atmoHum));

			double atmoTemp = atmosphericTemperature(
					littleEndianBytesToInt(cmd, offset, offset += EVENT_DATA_A_ATOMS_TEMP_SIZE));
			msg += "Atmospheric Temperature: " + atmoTemp + "\n";
			datas.put("Atmospheric Temperature", Double.toString(atmoTemp));

			double uv_A = UV_A(littleEndianBytesToSignedInt(cmd, offset, offset += EVENT_DATA_A_UV_SIZE));
			msg += "UV: " + uv_A + "\n";
			datas.put("UV", Double.toString(uv_A));

			double illu = illuminance(littleEndianBytesToSignedInt(cmd, offset, offset += EVENT_DATA_A_ILLU_SIZE));
			msg += "Illuminace: " + illu + "\n";
			datas.put("Illuminace", Double.toString(illu));

			// PM2.5
			double v0 = PM25(littleEndianBytesToInt(cmd, offset, offset + EVENT_DATA_A_PM25_SIZE));
			double alpha = 0.6;
			double beta;
			if (atmoHum > 50.0) {
				beta = 1 - 0.01467 * (atmoHum - 50);
			} else {
				beta = 1;
			}
			double vs = 0;
			if (this.Vs > -50) {
				if (atmoTemp <= 40) {
					vs = this.Vs - 6 * (this.VsTemp - atmoTemp);
				} else {
					vs = this.Vs - 1.5 * (this.VsTemp - atmoTemp);
				}
			}
			// double pm25 = alpha * beta * (v0 - vs);

			short vDiff = (short) ((v0 - vs) * 4095 / 6600);

			ByteBuffer buffer = ByteBuffer.allocate(2);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			buffer.putShort(vDiff);
			byte[] a = buffer.array();

			cmd[offset] = a[0];
			cmd[offset + 1] = a[1];
			// int ln = cmd.length;
			// cmd[ln-1] = XorBCCGenerating(cmd, ln-1);

			// double vdiff2 = littleEndianBytesToSignedInt(a,0,2);
			// double vdiff3 = littleEndianBytesToSignedInt(cmd, offset,
			// offset + EVENT_DATA_A_PM25_SIZE);
			double vd = PM25(littleEndianBytesToSignedInt(cmd, offset, offset + EVENT_DATA_A_PM25_SIZE));
			double pm25 = alpha * beta * vd;
			// datas.put("PM2.5-2", Double.toString(pm252));
			// msg += "vd: " + vd + "\n";
			// msg += "vdiff: " + vDiff + "\n";
			// msg += "vdiff2: " + vdiff2 + "\n";
			/// msg += "vdiff3: " + vdiff3 + "\n";
			// msg += "a[0]: " + a[0] + "\n";
			// msg += "a[1]: " + a[1] + "\n";
			// msg += "cmd[offset]: " + cmd[offset] + "\n";
			// msg += "cmd[offset + 1]: " + cmd[offset + 1] + "\n";

			// msg += "PM2.5test: " + pm25test + "\n";
			// msg += "this.Vs: " + this.Vs + "\n";
			// msg += "this.VsTemp: " + this.VsTemp + "\n";
			msg += "PM2.5: " + pm25 + "\n";
			// msg += "OFFset" + offset + "\n";
			// msg += "cmd.length" + cmd.length + "\n";
			// msg += "V0: " + v0 + "\n";
			// msg += "Vs: " + vs + "\n";

			datas.put("PM2.5", Double.toString(pm25));

		}
		return new SensorEvent(this, EVENT_DATA_A, cmd, msg, datas, data.time);
	}

	protected SensorEvent dataEventC(RawSensorData data) {
		// TODO Auto-generated method stub
		byte[] cmd = data.cmd;
		String msg = "Data Event C:";
		if (logger.isDebugEnabled()) {
			msg += " " + bytesToHexString(cmd);
		}
		msg += "\n";
		Map<String, String> datas = new HashMap<String, String>();
		// String hexString = bytesToHex(cmd);

		int offset = 1;
		String serialNo = littleEndianBytesToDecimalString(cmd, offset, offset += EVENT_DATA_C_SERO_SIZE);
		msg += "Serial number: " + serialNo + "\n";
		datas.put("Serial Number", serialNo);
		String dataIndex = littleEndianBytesToDecimalString(cmd, offset, offset += EVENT_DATA_C_DATA_INDEX_SIZE);

		msg += "Data index: " + dataIndex + "\n";
		datas.put("Data Index", dataIndex);
		String gpsData = "";

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
		gpsData = bytesToUTF8String(cmd, offset, cmd.length - 1);

		datas.put("GPS Sentence", gpsData);

		msg += "GPS data: " + gpsData + "\n";

		return new SensorEvent(this, EVENT_DATA_C, cmd, msg, datas, data.time);

	}

	protected SensorEvent dataEventB(RawSensorData data) {
		byte[] cmd = data.cmd;
		String msg = "Data Event B:";
		if (logger.isDebugEnabled()) {
			msg += " " + bytesToHexString(cmd);
		}
		msg += "\n";
		Map<String, String> datas = new HashMap<String, String>();
		// String hexString = bytesToHex(cmd);
		if (cmd.length != EVENT_DATA_B_PARA_SIZE + 1 && cmd.length != EVENT_DATA_B_PARA_SIZE) {
			msg += "Parameter size is illegal!";

		} else {
			int offset = 1;
			String serialNo = littleEndianBytesToDecimalString(cmd, offset, offset += EVENT_DATA_B_SERO_SIZE);
			msg += "Serial number: " + serialNo + "\n";
			datas.put("Serial Number", serialNo);
			String dataIndex = littleEndianBytesToDecimalString(cmd, offset, offset += EVENT_DATA_B_DATA_INDEX_SIZE);
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

			double pm25 = PM25(littleEndianBytesToInt(cmd, offset, offset += EVENT_DATA_A_PM25_SIZE));
			msg += "PM2.5: " + pm25 + "\n";
			datas.put("PM2.5", Double.toString(pm25));
			double atmoTemp = atmosphericTemperature(
					littleEndianBytesToInt(cmd, offset, offset += EVENT_DATA_B_ATOMS_TEMP_SIZE));
			msg += "Atmospheric Temperature: " + atmoTemp + "\n";
			datas.put("Atmospheric Temperature", Double.toString(atmoTemp));

		}
		return new SensorEvent(this, EVENT_DATA_B, cmd, msg, datas, data.time);

	}

	/**
	 * Removed from version 0.1 2015/12/12
	 */
	/*
	 * private SensorEvent dataEventERR(byte[] cmd) { // TODO Auto-generated
	 * method stub String msg = "Data Event Exception:"; Map<String, String>
	 * datas = new HashMap<String, String>(); if (debuggable) { msg += " " +
	 * bytesToHexString(cmd); } msg += "\n"; // String hexString =
	 * bytesToHex(cmd); if (cmd.length != EVENT_DATA_ERR_PARA_SIZE + 1) { msg +=
	 * "Parameter size is illegal!";
	 * 
	 * } else { int offset = 1; String serialNo =
	 * littleEndianBytesToDecimalString(cmd, offset, offset +=
	 * EVENT_DATA_A_SERO_SIZE); msg += "Serial number: " + serialNo + "\n";
	 * datas.put("Serial Number", serialNo); String dataIndex =
	 * littleEndianBytesToDecimalString(cmd, offset, offset +=
	 * EVENT_DATA_A_DATA_INDEX_SIZE); msg += "Data index: " + dataIndex + "\n";
	 * datas.put("Data Index", dataIndex);
	 * 
	 * byte errCode = cmd[offset]; byte[] err = { errCode }; String errMsg =
	 * EVENT_DATA_ERR_ERRS.get(errCode); msg += "Error message: " + errMsg;
	 * datas.put("Error Code:", bytesToHexString(err));
	 * datas.put("Error Message:", errMsg);
	 * 
	 * } return new SensorEvent(this, EVENT_DATA_ERR, msg, datas); }
	 */
	/**
	 * @param in
	 * @return
	 * @return a byte array containing the next response from the InputStream in
	 * @throws IOException
	 */
	public abstract void readCommand(InputStream in) throws IOException;

	protected SensorEvent parseResponse(RawSensorData data) {
		// String msg = "";

		SensorEvent sev = null;
		logger.debug("Sensor Response Received!\n");
		switch (data.getCMDCode()) {
		case RES_INFO:
			sev = responseINFO(data);
			break;
		case RES_CMD:
			sev = responseCMD(data);
			break;
		case RES_VS:
			sev = responseVS(data);
			break;
		case RES_GPS:
			sev = responseGPS(data);
			break;
		default:
			logger.error("Error: Undefined response code!");
			break;
		}
		if (sev != null) {
			logger.debug(sev.getMsg());
		}
		return sev;

	}

	protected SensorEvent parseEvent(RawSensorData data) {
		// TODO Auto-generated method stub

		SensorEvent sev = null;
		// String msg="";
		switch (data.getCMDCode()) {
		case EVENT_DATA_A:
			sev = dataEventA(data);
			break;
		case EVENT_DATA_B:
			sev = dataEventB(data);
			break;
		case EVENT_DATA_C:
			sev = dataEventC(data);
			break;
		/**
		 * Removed from version 0.1 2015/12/12
		 */
		/*
		 * case EVENT_DATA_ERR: sev = dataEventERR(cmd); break;
		 */
		case EVENT_GPS_START:
			sev = GPSEventSTART(data);
			break;
		default:
			logger.error("Error: Undefined response code! ");
			break;
		}
		if ( sev != null) {
			logger.debug(sev.getMsg());
		}
		return sev;
	}

}
