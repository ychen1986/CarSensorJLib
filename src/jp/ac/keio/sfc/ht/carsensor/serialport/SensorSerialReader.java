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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.ac.keio.sfc.ht.carsensor.Sensor;
import jp.ac.keio.sfc.ht.carsensor.client.SensorEventPublisher;
import jp.ac.keio.sfc.ht.carsensor.protocol.RawSensorData;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEvent;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEventListener;

public class SensorSerialReader extends Sensor implements SerialPortEventListener, AutoCloseable, Runnable {

	final Logger logger = LoggerFactory.getLogger(SensorSerialReader.class);
	final public int TIME_OUT = 10000;
	final public int BAUD_RATE = 115200;
	protected InputStream in;
	protected OutputStream out;
	SerialPort serialPort = null;
	private List<SensorEventListener> sensorEventListenerList = new LinkedList<SensorEventListener>();
	private BlockingQueue<RawSensorData> rawSensorDataQueue = new LinkedBlockingQueue<RawSensorData>();

	public SensorSerialReader(String _portName) throws UnsupportedCommOperationException,
			NoSuchPortException, PortInUseException, IOException, TooManyListenersException {

		super();
		
		serialPort = openSerialPort(_portName, TIME_OUT); // open the serial
		in = serialPort.getInputStream();

		out = serialPort.getOutputStream();

		serialPort.addEventListener(this);
		
		serialPort.notifyOnDataAvailable(true);

		(new Thread(this)).start();

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
		//while (portList.hasMoreElements()) {
		//	logger.info(portList.nextElement());
		//}

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
			
			try {
				
				readCommand(in);

			} catch (Exception e) {
				logger.error("",e);
			}
			
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
			logger.info("Close serial in/output streams...");
	
			in.close();
			out.close();
			
			logger.info("Close seril port...");
			serialPort.close();


		} catch (IOException e) {
			 
			logger.error("Closing Publishing failed.",e);

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
		logger.info("Clear all sensor event listeners!...");
		sensorEventListenerList.clear();
	}

	protected void triggerEventHandler(SensorEvent ev) throws Exception {
		for (SensorEventListener lsnr : sensorEventListenerList) {
			lsnr.handleSensorEvent(ev);
		}
	}

	
	
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
						logger.debug( " BCC check succeeded!");

		
						// System.out.println("Copying response begins at "+
						// System.currentTimeMillis());
						byte[] response = Arrays.copyOfRange(respBuffer, 1, length - 1);
						logger.debug(" New Response Received: " + bytesToHexString(response));

						// System.out.println("Data Parsing finished at "+
						// System.currentTimeMillis());
						// return new RawSensorData(response,currentTime);
						rawSensorDataQueue.put(new RawSensorData(response, timestamp));
						return;
					} else {
						logger.info( " BCC check failed!\n" + bytesToHexString(respBuffer, length));
						length = 0;
					}

				} catch (Exception e) {
					logger.error("",e);

				}
			}
		}
		// System.out.println("Data Parsing finished at "+
		// System.currentTimeMillis());
		return;
	}

	
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
				logger.error("",e);
				
			} catch (Exception e) {
				logger.error("",e);
				
			}

		}

	}
}
