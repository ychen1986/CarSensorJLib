package jp.ac.keio.sfc.ht.carsensor.protocol;

import java.io.Serializable;

import jp.ac.keio.sfc.ht.carsensor.SensorCMD;
import jp.ac.keio.sfc.ht.carsensor.Utility;

public final class RawSensorData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public RawSensorData(byte[] _cmd, long _time) {
		cmd = _cmd;
		time = _time;
	};

	public String toString() {
		return SensorCMD.bytesToHexString(cmd) + " "
				+ Utility.getFormatedTimestamp(time);
	};
	public byte getCMDCode(){
		return cmd[0];
	}
	public final byte[] cmd;
	public final long time;
	public String getSensorSerialNo() {
		byte [] seriNo = new byte[SensorCMD.EVENT_DATA_A_SERO_SIZE];
		for(int i =0; i < seriNo.length; i++){
			seriNo[i] = cmd[1+i];
		}
		
		
		return SensorCMD.littleEndianBytesToDecimalString(seriNo,0,seriNo.length);
	}
}
