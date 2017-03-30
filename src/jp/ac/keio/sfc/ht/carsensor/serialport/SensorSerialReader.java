/**
 * Copyright (C) 2015  @author Yin Chen 
 * Keio University, Japan
 */
package jp.ac.keio.sfc.ht.carsensor.serialport;

import gnu.io.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jp.ac.keio.sfc.ht.carsensor.Sensor;
import jp.ac.keio.sfc.ht.carsensor.protocol.RawSensorData;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEvent;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEventListener;

public class SensorSerialReader extends Sensor implements SerialPortEventListener, AutoCloseable, Runnable {

	static String classFileName = "[SensorSerialReader]";
	final public int TIME_OUT = 10000;
	final public int BAUD_RATE = 115200;
	protected InputStream in;
	protected OutputStream out;
	SerialPort serialPort = null;
	private List<SensorEventListener> sensorEventListenerList = new LinkedList<SensorEventListener>();
	private BlockingQueue<RawSensorData> rawSensorDataQueue = new LinkedBlockingQueue<RawSensorData>();

	public SensorSerialReader(String _portName, boolean _debuggable) throws UnsupportedCommOperationException,
			NoSuchPortException, PortInUseException, IOException, TooManyListenersException {

		super();
		this.debuggable = _debuggable;

		serialPort = openSerialPort(_portName, TIME_OUT); // open the
															// serial
		in = serialPort.getInputStream(); // name
		// in = new BufferedInputStream(serialPort.getInputStream());
		// in.mark(1);
		out = serialPort.getOutputStream();

		// (new Thread(new SerialWriter(out))).start();
		// this.stopSensor();
		serialPort.addEventListener(this);
		serialPort.notifyOnDataAvailable(true);

		(new Thread(this)).start();

	}

	/**
	 * @param portName
	 *            : path to the port file. E.g., /dev/ttyACM0
	 * @throws UnsupportedCommOperationException
	 *             , NoSuchPortException,PortInUseException,
	 *             IOException,TooManyListenersException
	 * 
	 */
	public SensorSerialReader(String portName) throws UnsupportedCommOperationException, NoSuchPortException,
			PortInUseException, IOException, TooManyListenersException {
		this(portName, false);

	}

	/**
	 * @param serialPortName
	 *            : name of the port to open
	 * @param timeout
	 *            : maximum waiting time
	 * @return:
	 * @throws UnsupportedCommOperationException
	 * @throws NoSuchPortException
	 * @throws PortInUseException
	 * @throws IOException
	 */
	private SerialPort openSerialPort(String serialPortName, int timeout)
			throws UnsupportedCommOperationException, NoSuchPortException, PortInUseException, IOException {

		System.setProperty("gnu.io.rxtx.SerialPorts", serialPortName);
		@SuppressWarnings("rawtypes")
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();
		// CommPortIdentifier id = null;
		while (portList.hasMoreElements()) {
			System.out.println(portList.nextElement());
		}

		CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(serialPortName);
		SerialPort port = (SerialPort) portId.open(this.getClass().getName(), timeout);
		port.setSerialPortParams(BAUD_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
		return port;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gnu.io.SerialPortEventListener#serialEvent(gnu.io.SerialPortEvent)
	 */
	public void serialEvent(SerialPortEvent event) {
		// System.out.println("Serial event occurred!");

		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

		while (true) {
			// long coming_time = System.currentTimeMillis();
			try {

				// System.out.println("A new data frame comes at "+coming_time);
				readCommand(in);

				/*
				 * if (data == null) { System.out
				 * .println("No more response or event obtained from sensor!");
				 * return; } else {
				 * 
				 * rawSensorDataQueue.put(data);
				 * 
				 * }
				 */
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// long finished_time = System.currentTimeMillis();
			// System.out.println("The data frame is enqueued at "+
			// finished_time + ". Processed time: " + (finished_time -
			// coming_time));
		}

	}

	public void sendCommand(byte[] cmd) throws IOException {
		out.write(cmd);
		out.flush();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Closeable#close()
	 */
	public void close() {
		try {
			stopSensor();
			clearSensorEventListener();
			if (debuggable) {

				System.out.println("Close serial in/output streams...");

				in.close();
				out.close();
			}
			if (debuggable) {
				System.out.println("Close seril port...");
				serialPort.close();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void addSensorEventListener(SensorEventListener lsnr) throws TooManyListenersException {
		if (!sensorEventListenerList.contains(lsnr)) {
			if (!sensorEventListenerList.add(lsnr)) {
				throw new TooManyListenersException("Adding sensor event failed!");
			}
		}
	}

	public void removeSensorEventListener(SensorEventListener lsnr) {
		sensorEventListenerList.remove(lsnr);
	}

	public void clearSensorEventListener() {
		if (debuggable) {
			System.out.println("Clear all sensor event listeners!...");
		}
		sensorEventListenerList.clear();
	}

	protected void triggerEventHandler(SensorEvent ev) throws Exception {
		for (SensorEventListener lsnr : sensorEventListenerList) {
			lsnr.handleSensorEvent(ev);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jp.ac.keio.sfc.ht.carsensor.SensorCMD#readCommand(java.io.InputStream)
	 */
	// @Override
	/*
	 * public byte[] readCommand(InputStream in) throws IOException {
	 * 
	 * int BUFF_MAX = 1024; int data; //TODO improve this function byte[]
	 * respBuffer = new byte[BUFF_MAX]; int len = 0; respBuffer[len++] =
	 * PROTOCOL_HEADER; int expected_len = -1; while (((data = in.read()) > -1))
	 * { // buffer bytes until reaching a PROTOCOL_HEADER try { if
	 * (PROTOCOL_HEADER != (byte) data) { if(len == 1){ // if the sensor is the
	 * test version, then the length of the event ABC // should be getParaSize +
	 * 2 (including the first cmd byte and the last bcc byte ); // else, it is
	 * getParaSize + 1. So, we set // the expected length of the command as
	 * follows. The expected_len is used to // void (expected in most case) the
	 * 9A byte (i.e., the protocol head ) appearing in the data field.
	 * expected_len = getParaSize((byte)data) + 2;
	 * 
	 * } if (len >= BUFF_MAX) { // Meet a over-long (>BUFF_MAX) response // and
	 * throw a exception len = 1; System.out .println(classFileName +
	 * " Over long reponse"); throw new
	 * Exception("Over-long response! Length > " + BUFF_MAX);
	 * 
	 * } respBuffer[len++] = (byte) data; } else { // TODO revise //
	 * System.out.println("Protocol Head Found!"); if (1 == len) { // Two
	 * PROTOCOL_HEADER bytes continue; } if(len <= expected_len){
	 * 
	 * respBuffer[len++] = (byte) data; continue; } byte next = (byte)
	 * in.read(); if(next == -1){ break; } if(!isResponse(next) &&
	 * !isEvent(next)){ respBuffer[len++] = (byte) next; continue; }
	 * 
	 * // copy the response to a new array byte[] response =
	 * Arrays.copyOfRange(respBuffer, 0, len); if (debuggable) {
	 * System.out.println(classFileName + " New Response Received: " +
	 * bytesToHexString(response));
	 * 
	 * }
	 * 
	 * if (BCCCheck(response)) { if (debuggable) {
	 * System.out.println(classFileName + " BCC check succeeded!");
	 * 
	 * } return response; } else { System.out.println(classFileName +
	 * " BCC check failed!\n" + bytesToHexString(response)); len = 1; }
	 * 
	 * } } catch (Exception e) { e.printStackTrace();
	 * 
	 * } try{ if (PROTOCOL_HEADER != (byte) data) { continue; }else {
	 * 
	 * data = in.read(); if(isEvent((byte)data) || isResponse((byte)data)){ byte
	 * cmd_code = (byte) data;
	 * 
	 * int len = getParaSize(cmd_code); if(len == -1){ byte[] hex = new byte[1];
	 * hex[0] = cmd_code; Exception ex = new
	 * Exception("Parameter size not found: " +bytesToHexString(hex) ); throw
	 * ex; } byte[] resp = new byte[len + 3]; int readLen = 0; while(readLen >
	 * -1){ readLen = in.read(resp, readLen + 2, len+1); if(readLen == len+1){
	 * resp[0] = PROTOCOL_HEADER; resp[1] = cmd_code; return resp; } else if(
	 * readLen > -1 && readLen <= len + 1){ len = len - readLen;
	 * Thread.sleep(100);
	 * 
	 * } }
	 * 
	 * 
	 * }
	 * 
	 * 
	 * } }catch (Exception e){ e.printStackTrace(); }
	 * 
	 * 
	 * 
	 * } return null; }
	 */

	static int BUFF_MAX = 1024;
	static byte[] respBuffer = new byte[BUFF_MAX];

	int data;
	long timestamp;
	int start;
	int length;
	static int EVENT_DATA_C_HEAD_SIZE = 1 + EVENT_DATA_C_SERO_SIZE + EVENT_DATA_C_DATA_INDEX_SIZE
			+ EVENT_DATA_C_ERR_FLAG_SIZE;

	public void readCommand(InputStream in) throws IOException {

		// System.out.println("Command reading begins at "+
		// System.currentTimeMillis());
		data = -1;
		timestamp = 0;
		length = 0;
		int remaining_len = -1;
		while (((data = in.read()) > -1)) {
			// buffer bytes until reaching a PROTOCOL_HEADER
			if (PROTOCOL_HEADER != (byte) data) {

				continue;
			} else {
				// System.out.println("Protocol Head found at "+
				// System.currentTimeMillis());
				timestamp = System.currentTimeMillis();
				// System.out.println("Data Parsing begins at "+
				// System.currentTimeMillis());
				respBuffer[length++] = (byte) data; // input the protocol head
													// into buffer
				data = in.read(); // read the cmd code,e.g., 0x8A

				// Get the expected length of next command;
				try {
					// System.out.println("Getting remaining_len begins at "+
					// System.currentTimeMillis());

					// * Version check is removed. 2016/11/09

					// int softVersion = 1;
					// if (this.softwareVersion != null) {
					// softVersion = Integer.parseInt(this.softwareVersion);
					// }
					// if (softVersion < 1) {
					// it is software version 0
					// remaining_len = getParaSize((byte)data) + 2;
					// } else {

					if (EVENT_DATA_C != (byte) data) { // the frame is not an
														// event data C

						remaining_len = getParaSize((byte) data) + 1;
						// System.out.println("function" + remaining_len);
						// System.out.println("Map " + (ParaSizeMap.get(data) +
						// 1));
						// remaining_len = ParaSizeMap.get(data)+1;
						// System.out.println("None-C event: remaining_len
						// determined as "+remaining_len + " at: " +
						// System.currentTimeMillis());
					} else {// the frame is an event data C
						int head_len = EVENT_DATA_C_SERO_SIZE + EVENT_DATA_C_DATA_INDEX_SIZE
								+ EVENT_DATA_C_ERR_FLAG_SIZE;

						// loop read the header
						for (int i = 0; i <= head_len; i++) {
							respBuffer[length++] = (byte) data;
							data = in.read();
						}
						/*
						 * {// batch read in.read(respBuffer, length, head_len);
						 * length += head_len; data = in.read(); }
						 */

						remaining_len = data + 1;
						// System.out.println("C event: remaining_len determined
						// at: "+ System.currentTimeMillis());
					}
					// loop read
					remaining_len += length;
					// } // Version check
					respBuffer[length++] = (byte) data; // Buffer the next byte
														// into repsBuffer
														// including the
														// protocol head and BBC

					// System.out.println("Reading the remaining_len begins at
					// "+ System.currentTimeMillis());
					// loop read
					while (length <= remaining_len) {
						data = in.read();
						respBuffer[length++] = (byte) data;
					}
					/*
					 * {// batch read in.read(respBuffer, length,
					 * remaining_len); length += remaining_len; }
					 */

					// System.out.println("BCCCheck begins at "+
					// System.currentTimeMillis());
					if (BCCCheck(respBuffer, length)) {
						if (debuggable) {
							System.out.println(classFileName + " BCC check succeeded!");

						}
						// System.out.println("Copying response begins at "+
						// System.currentTimeMillis());
						byte[] response = Arrays.copyOfRange(respBuffer, 1, length - 1);
						if (debuggable) {
							System.out.println(classFileName + " New Response Received: " + bytesToHexString(response));

						}
						// System.out.println("Data Parsing finished at "+
						// System.currentTimeMillis());
						// return new RawSensorData(response,currentTime);
						rawSensorDataQueue.put(new RawSensorData(response, timestamp));
						return;
					} else {
						System.out
								.println(classFileName + " BCC check failed!\n" + bytesToHexString(respBuffer, length));
						length = 0;
					}

				} catch (Exception e) {
					e.printStackTrace();

				}
			}
		}
		// System.out.println("Data Parsing finished at "+
		// System.currentTimeMillis());
		return;
	}

	/*
	 * public void readCommand(InputStream in) throws IOException {
	 * 
	 * 
	 * timestamp = System.currentTimeMillis(); // record the current time
	 * System.out.println("Command reading  begins at "+ timestamp); // for
	 * debug start = 0; if(1 == length){// A solo 9A came in last time.
	 * System.out.println(classFileName + " Solo 9A. Add suceeding bytes.");
	 * length = in.read(respBuffer,start+1,BUFF_MAX-1) + 1; // obtain the
	 * succeeding data as many as BUFF_MAX }else{
	 * System.out.println(classFileName +
	 * "Not Solo 9A. Read suceeding bytes as a new cmd."); length =
	 * in.read(respBuffer,start,BUFF_MAX); // obtain a new byte array as many as
	 * BUFF_MAX }
	 * 
	 * 
	 * if (0 == length){ // No data any more return !
	 * System.out.println("No more available data: "+
	 * System.currentTimeMillis()); return; } System.out.println(classFileName +
	 * " New Response Received: " + bytesToHexString(respBuffer,length)); // for
	 * debug
	 * 
	 * if ( PROTOCOL_HEADER == respBuffer[start] ){ // A protocol head is
	 * recognized
	 * 
	 * if(1 == length ) { //A solo 9A came separately with the succeeding bytes.
	 * //Return to wait the suceeding bytes return; }
	 * 
	 * if(!BCCCheck(respBuffer,length)) { //Do error check with nor BCC
	 * checksum. // Notice that event multiple commands come as the same time,
	 * the checksum still work. System.out.println(classFileName +
	 * " BCC check failed!\n" + bytesToHexString(respBuffer,length)); return;
	 * }else{ System.out.println(classFileName + " BCC check suceeded!\n" +
	 * bytesToHexString(respBuffer,length)); }
	 * 
	 * System.out.println(classFileName + " length of buffered bytes: " +
	 * length); for(int remaining_len = 0; start < length; ){
	 * 
	 * Parse the bytes into sensor response or cmds.
	 * 
	 * 
	 * System.out.println(classFileName + " Start = " + start); start++; // move
	 * to cmd code byte cmd_code = respBuffer[start]; // get the cmd code
	 * System.out.println(classFileName + " Get parameter size for CMD code" +
	 * byteToHexString(cmd_code)); if (EVENT_DATA_C != cmd_code) { // the frame
	 * is not an event data C
	 * 
	 * try {
	 * 
	 * remaining_len = 1 + getParaSize(cmd_code); } catch (Exception e) { //
	 * TODO Auto-generated catch block e.printStackTrace(); return; }
	 * System.out.println("None-C event: remaining_len determined as "+
	 * remaining_len + " at: "+ System.currentTimeMillis()); } else {// the
	 * frame is an event data C
	 * 
	 * remaining_len = EVENT_DATA_C_HEAD_SIZE + (int)respBuffer[start +
	 * EVENT_DATA_C_HEAD_SIZE] + 1;
	 * 
	 * System.out.println("C event: remaining_len determined as "+ remaining_len
	 * + " at: "+ System.currentTimeMillis()); }
	 * 
	 * System.out.println("Copy from " + start + " to " + (start + remaining_len
	 * - 1)); byte[] response = Arrays.copyOfRange(respBuffer, start,
	 * remaining_len); // copy from cmd_code to the end of the cmd without
	 * protocol head and checksum start = start + remaining_len + 1; // move to
	 * next head_protocol (if exits) System.out.println(classFileName +
	 * "A new cmd is copied: " + bytesToHexString(response)); try {
	 * rawSensorDataQueue.put(new RawSensorData(response,timestamp)); } catch
	 * (InterruptedException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); }
	 * 
	 * }
	 * 
	 * 
	 * return; }else{ // not startting with protocol head. Drop it. return;
	 * 
	 * }
	 * 
	 * 
	 * }
	 */
	public String toString() {
		String msg = super.toString();
		msg += "Serial port: " + serialPort + "\n";
		msg += "Baud rate: " + BAUD_RATE + "\n";
		return msg;
	}

	@Override
	public void run() {
		SensorEvent sev;

		while (true) {

			try {
				RawSensorData data = rawSensorDataQueue.take();// get raw data
																// received from
																// sensor

				sev = parse(data); // parse the raw data into a sensor event
									// where the PM 2.5 data is corrected.
				triggerEventHandler(sev);

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}
}
