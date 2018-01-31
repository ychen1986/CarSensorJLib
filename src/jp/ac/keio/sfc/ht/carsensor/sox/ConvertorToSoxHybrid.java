package jp.ac.keio.sfc.ht.carsensor.sox;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.ac.keio.sfc.ht.carsensor.protocol.CarSensorException;
import jp.ac.keio.sfc.ht.carsensor.protocol.RawSensorData;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEvent;
import jp.ac.keio.sfc.ht.sox.protocol.Transducer;
import jp.ac.keio.sfc.ht.sox.protocol.TransducerValue;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxConnection;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxDevice;

public class ConvertorToSoxHybrid extends ConvertorToSox {

		
	final static Logger logger = LoggerFactory.getLogger(ConvertorToSoxHybrid.class);
	SoxDevice lowSpeedDvice;

	public ConvertorToSoxHybrid(Socket _socket, String _soxServer, String _soxUser, String _soxPasswd, boolean _debug,
			int _publishRate) {
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

		// connectToDevice();

	}
	
//	public ConvertorToSoxHybrid(Socket _socket, boolean _debug, int _publishRate) {
//		super();
//
//		socket = _socket;
//		debug = _debug;
//		publishRate = _publishRate;
//
//		if (socket != null) {
//			try {
//				inFromClient = new ObjectInputStream(socket.getInputStream());
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//
//		
//
//	}

//	public ConvertorToSoxHybrid(Socket _socket, ObjectInputStream _inFromClient, SoxDevice _high, SoxDevice _low,
//			boolean _debug) {
//		super(_socket, _inFromClient, _high, _debug);
//
//		// TODO Auto-generated constructor stub
//		lowSpeedDvice = _low;
//	}

	@Override
	void publish() {

		RawSensorData data = null;
		SensorEvent se = null;
		int highCount = 0;
		int lowCount = 0;
		List<TransducerValue> lastValues = null;
		List<TransducerValue> gpsValues = new LinkedList<TransducerValue>();
		;
		boolean hasLongitude = false;
		boolean hasSpeed = false;
		double sumOfPM25 = 0;
		int count_sumOfPM25 = 0;
		
		while (true) {
			try {
				data = (RawSensorData) inFromClient.readObject();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.info("Connection from " + socket.getInetAddress() + " is interrupted!");
				return;
			}
			try {
				se = parseDataEvent(data);
				List<TransducerValue> values = se.toTranducerValueList();
				// se.toTranducerValueList();

				if (device != null) {
					switch (se.getEventType()) {
					case EVENT_DATA_C:// A gps Event is always published.
						break;

					case EVENT_DATA_A:// Other data are published with rate =
										// publishRate
						if (++highCount % (SENSOR_SAMPLE_RATE / publishRate) == 0) {
							highCount = 0;
							break;
						} else {

							continue;
						}
					}
					device.publishValues(values);
					highCount = 0;
				}

				if (lowSpeedDvice != null) {

					switch (se.getEventType()) {
					case EVENT_DATA_C:

						for (TransducerValue value : values) {
							
							
							
							if (value.getId().equals("Longitude")) {
								hasLongitude = true;
							}
							if (value.getId().equals("Serial Number") || value.getId().equals("Data Index")) {
								continue;
							}
							if (value.getId().equals("Speed")) {
								hasSpeed = true;
							}
							gpsValues.add(value);
						}
						if (hasLongitude && hasSpeed) {// A gps Event with
														// long/lat will incur a
														// publishing.
							break;
						} else {
							continue;
						}

					case EVENT_DATA_A:// Other data are stored into the
									  // lastVaules list; PM2.5 is accmulated into sumOfPM25

						lastValues = values;
						for (TransducerValue value : values) {
							
							if (value.getId().equals("PM2.5")){
								double pm25 = Double.parseDouble(value.getRawValue()); // Obtain the pm2.5 data;
								if (pm25 < 0){
									pm25 = 0; // if the pm2.5 data is negative, set it to 0;
								}
								
								sumOfPM25 += pm25;  // accumulation 
								count_sumOfPM25++;
								
							}
							if( count_sumOfPM25 >= 100){ // if more than 100 sets of data are accumulated, clear them.
								count_sumOfPM25 = 0;
								sumOfPM25 = 0;
							}
							
							
						}
						
						continue;
					default:
						continue;

					}
					double avePM25 = 0;
					if(count_sumOfPM25 != 0){
						avePM25 = sumOfPM25/count_sumOfPM25;
						sumOfPM25 = 0;
						count_sumOfPM25 = 0;
					}
					
					for (TransducerValue value : lastValues) {
						
						if (value.getId().equals("PM2.5")){
							value.setRawValue(Double.toString(avePM25));
							
							
						}						
						
					}
					
					lastValues.addAll(gpsValues);
					lowSpeedDvice.publishValues(lastValues);
					lastValues = null;
					gpsValues = new LinkedList<TransducerValue>();
					hasLongitude = false;
					hasSpeed = false;
					lowCount = 0;

				}

				logger.debug(se.toString());
			} catch (CarSensorException e) {
				logger.error(e.getMessage());
			} catch (NotConnectedException e) {
				// TODO Auto-generated catch block
				logger.error(e.getMessage());
				reconnectToDevice();
			}

		}
	}

	@Override
	protected void reconnectToDevice() {
		// TODO Auto-generated method stub
		publishDeviceMap.remove(sensorNaming(sensorNo));
		publishDeviceMap.remove(sensorNaming(sensorNo) + "_100Hz");
		connectToDevice();

	}

	@Override
	protected void connectToDevice() {

		logger.debug("Creat sox device...");

		RawSensorData data = null;

		try {
			data = (RawSensorData) inFromClient.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			logger.error("",e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("",e);
		}

		sensorNo = getSensorNO(data);
		logger.debug("Sensor Number:" + sensorNo);

		try {
			lowSpeedDvice = findSoxDevice(sensorNo);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("",e);
		}
		try {
			device = findSoxDevice(sensorNo + "_100Hz");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("",e);
		}
		logger.debug("done!");
	}

	public static void main(String[] args) {

		ServerSocket welcomeSocket = null;
		try {
			logger.info("Waiting at port 6222...");
			welcomeSocket = new ServerSocket(6222);
			Socket connectionSocket = null;
			connectionSocket = welcomeSocket.accept();
			String soxServer = "nictsox-lv2.ht.sfc.keio.ac.jp";
			//String soxDevice = "car";
			//SoxConnection con = new SoxConnection(soxServer, false);
			//SoxDevice device = new SoxDevice(con, soxDevice);
			ObjectInputStream out = new ObjectInputStream(connectionSocket.getInputStream());
			Thread thread = new Thread(new ConvertorToSoxHybrid(connectionSocket,soxServer,"guest","miroguest",true,100));
			thread.start();

		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
