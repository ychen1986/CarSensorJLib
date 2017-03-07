/**
 * Copyright (C) 2015  @author Yin Chen 
 * Keio University, Japan
 */

package jp.ac.keio.sfc.ht.carsensor.protocol;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;

import jp.ac.keio.sfc.ht.carsensor.SensorCMD;
import jp.ac.keio.sfc.ht.carsensor.Utility;
import jp.ac.keio.sfc.ht.sox.protocol.TransducerValue;

public class SensorEvent extends EventObject {

	private byte eventType;
	private static final long serialVersionUID = 1L;
	private byte[] cmd;
	private long timestamp;
	private String msg;

	private Map<String, String> datas;

	public SensorEvent(Object source, byte _eventType, byte[] _cmd, String _msg, Map<String, String> _datas,
			long _timestamp) {
		super(source);
		this.msg = _msg;
		this.cmd = _cmd;
		this.datas = _datas;
		this.eventType = _eventType;
		this.timestamp = _timestamp;
	}

	public SensorEvent(byte _eventType, byte[] _cmd, String _msg, Map<String, String> _datas, long _timestamp) {

		this(new Object(), _eventType, _cmd, _msg, _datas, _timestamp);

	}

	public SensorEvent(Object source, byte _eventType, byte[] _cmd, String _msg, Map<String, String> _datas) {

		super(source);

		this.msg = _msg;
		this.cmd = _cmd;
		this.datas = _datas;
		this.eventType = _eventType;
		this.timestamp = System.currentTimeMillis();
	}

	public SensorEvent(byte _eventType, byte[] _cmd, String _msg, Map<String, String> _datas) {
		this(new Object(), _eventType, _cmd, _msg, _datas);
	}

	public byte getEventType() {
		return eventType;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getTimestampString() {

		return Utility.getFormatedTimestamp(timestamp);
	}

	public byte[] getCmd() {
		return cmd;
	}

	public String getMsg() {
		return msg;
	}

	public Map<String, String> getDatas() {
		return datas;

	}

	public boolean isResponse() {
		return SensorCMD.isResponse(eventType);
	}

	public boolean isEvent() {
		return SensorCMD.isEvent(eventType);
	}

	public void setTimestamp(long time) {
		timestamp = time;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		SensorEvent cloned = (SensorEvent) super.clone();
		cloned.timestamp = this.timestamp;
		cloned.datas = this.datas;
		cloned.eventType = this.eventType;
		return cloned;

	}

	public String getSensorSerialNo() {
		String no = datas.get("Serial Number");
		if (no == null) {
			return "Serial number not found!";
		} else {
			return no;
		}
	}

	@Override
	public String toString() {
		return "SensorEvent [msg=\n" + msg + getTimestampString() + "\n]";
	}

	public List<TransducerValue> toTranducerValueList() {
		// TODO Auto-generated method stub
		List<TransducerValue> values = new ArrayList<TransducerValue>();
		for (Map.Entry<String, String> e : datas.entrySet()) {
			TransducerValue value = new TransducerValue();
			value.setTimestamp(getTimestampString());
			value.setId(e.getKey());
			// value.setTypedValue(e.getValue());
			value.setRawValue(e.getValue());
			values.add(value);
		}
		return values;
	}

}
