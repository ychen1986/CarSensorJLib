/**
 * Copyright (C) 2015  @author Yin Chen 
 * Keio University, Japan
 */

package jp.ac.keio.sfc.ht.carsensor;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public abstract class SensorCMD extends Object {



	/**
	 * Communication protocol header.
	 */
	final public static byte PROTOCOL_HEADER =  (byte) 0x9a;
	/**
	 * Command Code: Request sensor information. 
	 */
	final public static byte CMD_GET_INFO = (byte)0x10;
	/**
	 * Command Code: Response to sensor information request.
	 */
	final public static byte RES_INFO = (byte) 0x90;
	/**
	 * Parameter size of RES_INFO
	 */
	final public static int  RES_INFO_PARA_SIZE = 18;
	final public static int  RES_INFO_SERIAL_NO_SIZE = 4;
	final public static int  RES_INFO_SOFT_VERSION_SIZE = 4;
	final public static int  RES_INFO_MODEL_SIZE = 10;
	
	
	final public static byte CMD_SENSOR = (byte) 0x13;
	final public static byte CMD_SENSOR_START = (byte) 0xff;
	final public static byte CMD_SENSOR_STOP = (byte) 0x00;
	final public static byte CMD_WITH_FAN = (byte) 0x00;
	final public static byte CMD_WITHOUT_FAN = (byte) 0x01;
	
	final public static byte RES_CMD = (byte) 0x8f;
	final public static int  RES_CMD_PARA_SIZE = 2;	
	final public static byte RES_CMD_NOR = (byte) 0x00;
	final public static byte RES_CMD_ERR = (byte) 0x01;
	
	final public static byte CMD_GET_VS = (byte) 0x3d;
	final public static byte RES_VS = (byte) 0xbd;
	final public static int  RES_VS_PARA_SIZE = 4;
	final public static int  RES_VS_VOL_SIZE = 2;
	final public static int  RES_VS_TEMP_SIZE = 2;
	
	
	final public static byte CMD_GET_GPS = (byte) 0x3c;
	final public static byte RES_GPS = (byte) 0xbc;
	final public static int  RES_GPS_PARA_SIZE = 1;
	final public static byte RES_GPS_FINISHED = (byte) 0x01;
	final public static byte RES_GPS_NOTFINISH = (byte) 0x00;
	
	
	
	final public static byte EVENT_DATA_A = (byte) 0x8a;
	final public static int  EVENT_DATA_A_PARA_SIZE = 36;
	final public static int  EVENT_DATA_A_SERO_SIZE = 2;
	final public static int  EVENT_DATA_A_DATA_INDEX_SIZE = 3;
	final public static int  EVENT_DATA_A_ERR_FLAG_SIZE = 1;
	final public static int  EVENT_DATA_A_ACC_X_SIZE = 2;
	final public static int  EVENT_DATA_A_ACC_Y_SIZE = 2;
	final public static int  EVENT_DATA_A_ACC_Z_SIZE = 2;
	final public static int  EVENT_DATA_A_ANVE_X_SIZE = 2;
	final public static int  EVENT_DATA_A_ANVE_Y_SIZE = 2;
	final public static int  EVENT_DATA_A_ANVE_Z_SIZE = 2;
	final public static int  EVENT_DATA_A_GEMG_X_SIZE = 2;
	final public static int  EVENT_DATA_A_GEMG_Y_SIZE = 2;
	final public static int  EVENT_DATA_A_GEMG_Z_SIZE = 2;
	final public static int  EVENT_DATA_A_ATOMS_PRSR_SIZE = 2;
	final public static int  EVENT_DATA_A_ATOMS_HUM_SIZE = 2;
	final public static int  EVENT_DATA_A_ATOMS_TEMP_SIZE = 2;
	final public static int  EVENT_DATA_A_UV_SIZE = 2;
	final public static int  EVENT_DATA_A_ILLU_SIZE = 2;
	final public static int  EVENT_DATA_A_PM25_SIZE = 2;
	
	final public static byte EVENT_DATA_B = (byte) 0x8b;
	final public static int  EVENT_DATA_B_PARA_SIZE = 10;
	final public static int  EVENT_DATA_B_SERO_SIZE = 2;
	final public static int  EVENT_DATA_B_DATA_INDEX_SIZE = 3;
	final public static int  EVENT_DATA_B_ERR_FLAG_SIZE = 1;
	final public static int  EVENT_DATA_B_ATOMS_TEMP_SIZE = 2;
	final public static int  EVENT_DATA_B_PM25_SIZE = 2;
	
	
	final public static byte EVENT_DATA_C = (byte) 0x8C;
	//final public static int  EVENT_DATA_C_PARA_SIZE = 606;
	final public static int  EVENT_DATA_C_SERO_SIZE = 2;
	final public static int  EVENT_DATA_C_ERR_FLAG_SIZE = 1;
	final public static int  EVENT_DATA_C_GPS_DATA_SIZE = 1;
	
	final public static int  EVENT_DATA_C_DATA_INDEX_SIZE = 3;
	//final public static int  EVENT_DATA_C_DATA_SIZE = 600;
	
/**
 *  Removed from version 0.1 2015/12/12
 */	
/*	final public static byte EVENT_DATA_ERR = (byte) 0x87;
	final public static int  EVENT_DATA_ERR_PARA_SIZE = 6;
	final public static int  EVENT_DATA_ERR_SERO_SIZE = 2;
	final public static int  EVENT_DATA_ERR_DATA_INDEX_SIZE = 3;
	final public static int  EVENT_DATA_ERR_ERR_SIZE = 1;*/
	
	public static Map<Byte,String> EVENT_DATA_ERR_ERRS = ImmutableMap.<Byte, String>builder()
															.put(new Byte((byte)0x01),"[Sensor Error: acceleration and angular velocity sensor.]")
															.put(new Byte((byte)0x02),"[Sensor Error: geomagnetism sensor.]")
															.put(new Byte((byte)0x04),"[Sensor Error: atmospheric pressure sensor.]")
															.put(new Byte((byte)0x08),"[Sensor Error: thermo-hygrometer sensor.]")
															.put(new Byte((byte)0x10),"[Sensor Error: UV and illuminance sensor.]")
															//.put(new Byte((byte)0x20),"[Sensor Error: PM2.5 sensor.]")
															.put(new Byte((byte)0x40),"[Sensor Error: GSP sensor.]")
															.build();
	
	public static String identifyErrFlag(byte err){
		String errMSG=null;
		
		for(Map.Entry<Byte, String> entry : EVENT_DATA_ERR_ERRS.entrySet()){
			if((err & entry.getKey()) != 0){
				if(errMSG != null){
					errMSG += entry.getValue();
				}else{
					errMSG = entry.getValue();
				}
				errMSG+="\n";
			}
		}
		
		
		return errMSG;
		
	}
	
	final public static byte EVENT_GPS_START = (byte) 0x88;
	final public static int  EVENT_GPS_START_PARA_SIZE = 1;
	

	
	public static boolean isCommand(byte code) {
		switch (code){
		case CMD_GET_INFO: return true;
		case CMD_SENSOR: return true;
		case CMD_GET_VS: return true;
		case CMD_GET_GPS: return true;
		default: return false;
		}
	}
	
	public static boolean isResponse(byte code) {
		switch (code){
		case RES_INFO: return true;
		case RES_CMD: return true;
		case RES_VS: return true;
		case RES_GPS: return true;
		default: return false;
		}
	}
	
	public static boolean isEvent(byte code) {
		switch (code){
		case EVENT_DATA_A: return true;
		case EVENT_DATA_B: return true;
		case EVENT_DATA_C: return true;
		/**
		 *  Removed from version 0.1 2015/12/12
		 */	
		//case EVENT_DATA_ERR: return true;
		case EVENT_GPS_START: return true;
		default: return false;
		}
	}
	

	
	/**
	 * Request Sensor Information
	 */
	final public static byte[] GET_SENSOR_INFOMATION = {PROTOCOL_HEADER,CMD_GET_INFO, (byte)0x00, (byte)0x8a};
	
	/**
	 * Start Sensor with Fan Working
	 */
	final public static byte[] START_SENSOR_WITH_FAN ={PROTOCOL_HEADER,CMD_SENSOR,CMD_SENSOR_START,CMD_WITH_FAN, (byte)0x76};
	/**
	 * Stop Sensor with Fan Working
	 */
	final public static byte[] STOP_SENSOR_WITH_FAN ={PROTOCOL_HEADER,CMD_SENSOR,CMD_SENSOR_STOP,CMD_WITH_FAN,(byte)0x89};
	/**
	 * Start Sensor without Fan Working
	 */
	final public static byte[] START_SENSOR_WITHOUT_FAN ={PROTOCOL_HEADER,CMD_SENSOR,CMD_SENSOR_START,CMD_WITHOUT_FAN,(byte)0x77};
	/**
	 * Stop Sensor without Fan Working
	 */
	final public static byte[] STOP_SENSOR_WITHOUT_FAN ={PROTOCOL_HEADER,CMD_SENSOR,CMD_SENSOR_STOP,CMD_WITHOUT_FAN,(byte)0x88};
	
	final public static byte[] GET_GPS_STATUS = {PROTOCOL_HEADER,CMD_GET_GPS,(byte)0x00,(byte)PROTOCOL_HEADER^CMD_GET_GPS};
	final public static byte[] GET_VS_VALUE = {PROTOCOL_HEADER,CMD_GET_VS,(byte)0x00,(byte)PROTOCOL_HEADER^CMD_GET_VS};
	

	

	
	
	protected static boolean BCCCheck(byte[] raw) {
		return BCCCheck(raw, raw.length);
	}
	/**
	 * check the BBC value of a byte array with length len. 
	 * The final byte (i.e., raw[len-1]) stores the bitwise xor of 
	 * the proceeding len-1 bytes.
	 * @param raw 
	 * @param len
	 * @return true if bitwise xor value of the len-1 bytes equals
	 *  the last byte; otherwise false
	 */
	protected static boolean BCCCheck(byte[] raw, int len) {		
		if ((raw == null) || len < 1 ){
			//TODO throw exception 
			return false;
		}
		if (0 == XorBCCGenerating(raw, len )) {
			return true;
		} else
			return false;
	}

	protected  static byte XorBCCGenerating(byte[] raw) {
		int xor = 0;
		for (byte each : raw) {
			xor = (byte) xor ^ each;
		}
		return (byte) xor;
	}

	protected static byte XorBCCGenerating(byte[] raw, int len) {
		int xor = 0;
		for (int i = 0; i < len; i++) {
			xor = (byte) xor ^ raw[i];
		}
		return (byte) xor;
	}
	


	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHexString(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	public static String byteToHexString(byte byteChar) {
	    char[] hexChars = new char[2];
	        int v = byteChar & 0xFF;
	        hexChars[0] = hexArray[v >>> 4];
	        hexChars[1] = hexArray[v & 0x0F];

	    return new String(hexChars);
	}
	public static String bytesToHexString(byte[] bytes, int len) {
	    char[] hexChars = new char[len * 2];
	    for ( int j = 0; j < len; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static String  bytesToDecimalString(byte[] bytes, int from, int to){
		byte[] buffer = Arrays.copyOfRange(bytes, from,
				to);
		BigInteger bi = new BigInteger(1,buffer);
		return bi.toString();
	}
	public static String  littleEndianBytesToDecimalString(byte[] bytes, int from, int to){
		byte[] buffer = Arrays.copyOfRange(bytes, from,
				to);
		Utility.reverseBytes(buffer);
		BigInteger bi = new BigInteger(1,buffer);
		String text = String.format("%03d", bi);
		return text;
	}
	public static String  bytesToSignedDecimalString(byte[] bytes, int from, int to){
		byte[] buffer = Arrays.copyOfRange(bytes, from,
				to);
		BigInteger bi = new BigInteger(buffer);
		return bi.toString();
	}
	public static String  littleEndianBytesToSignedDecimalString(byte[] bytes, int from, int to){
		byte[] buffer = Arrays.copyOfRange(bytes, from,
				to);
		Utility.reverseBytes(buffer);
		BigInteger bi = new BigInteger(buffer);
		return bi.toString();
	}

	public static int  littleEndianBytesToInt(byte[] bytes, int from, int to){
		byte[] buffer = Arrays.copyOfRange(bytes, from,
				to);
		Utility.reverseBytes(buffer);
		BigInteger bi = new BigInteger(1,buffer);
		return bi.intValue();
	}
	public static int  bytesToInt(byte[] bytes, int from, int to){
		byte[] buffer = Arrays.copyOfRange(bytes, from,
				to);
		BigInteger bi = new BigInteger(1,buffer);
		return bi.intValue();
	}
	public static int  littleEndianBytesToSignedInt(byte[] bytes, int from, int to){
		byte[] buffer = Arrays.copyOfRange(bytes, from,
				to);
		Utility.reverseBytes(buffer);
		BigInteger bi = new BigInteger(buffer);
		return bi.intValue();
	}
	public static int  bytesToSignedInt(byte[] bytes, int from, int to){
		byte[] buffer = Arrays.copyOfRange(bytes, from,
				to);
		BigInteger bi = new BigInteger(buffer);
		return bi.intValue();
	}
	public static String littleEndianBytesToUTF8String(byte[] bytes, int from, int to){
		byte[] buffer = Arrays.copyOfRange(bytes, from,
				to);
		Utility.reverseBytes(buffer);
		String msg="";
		try {
			msg+= new String(buffer, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			
			e.printStackTrace();
			msg += "The Character Encoding is not supported.\n";
			msg += e.getMessage();
		}
		return msg;
	}
	public static String nullEndianBytesToUTF8String(byte[] bytes, int from, int to){
		int end = from;
		while(bytes[end] != 0x00 && end <= to){
			end++;
		}
		byte[] buffer = Arrays.copyOfRange(bytes, from,
				end-1);
		String msg="";
		try {
			msg+= new String(buffer, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			
			e.printStackTrace();
			msg += "The Character Encoding is not supported.\n";
			msg += e.getMessage();
		}
		return msg;
	}
	public static String bytesToUTF8String(byte[] bytes, int from, int to){
		byte[] buffer = Arrays.copyOfRange(bytes, from,
				to);
		String msg="";
		try {
			msg+= new String(buffer, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			
			e.printStackTrace();
			msg += "The Character Encoding is not supported.\n";
			msg += e.getMessage();
		}
		return msg;
	}
	public static double acceleration(int raw){
		return raw*32000.0/65535;
	}
	public static double angularVelocity(int raw){
		return raw*4000.0/65535;
	}
	public static double geomagnetism(int raw){
		return raw*9600.0/65535;
	}
	public static double atmosphericPressure(int raw){
		return raw*860.0/65535 + 250;
	}
	public static double atmosphericHumidity(int raw){
		return (raw-896)/64.0;
	}
	public static double atmosphericTemperature(int raw){
		return (raw-2096)/50.0;
	}
	public static double UV_A(int raw){
		return raw*0.0635;
	} 
	public static double illuminance(int raw){
		return raw*20.0;
	}
	
	public static double PM25(int raw){
		//TODO revise
		return raw * 6600 / 4095;
	}
	
}
