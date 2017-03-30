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

import jp.ac.keio.sfc.ht.carsensor.protocol.CarSensorException;
import jp.ac.keio.sfc.ht.carsensor.protocol.RawSensorData;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEvent;
import jp.ac.keio.sfc.ht.sox.protocol.Transducer;
import jp.ac.keio.sfc.ht.sox.protocol.TransducerValue;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxConnection;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxDevice;

public class ConvertorToSoxHybrid extends ConvertorToSox {

	private static final String CLASS_NAME = "ConvertorToSoxHybrid";
	SoxDevice lowSpeedDvice;

	public ConvertorToSoxHybrid(Socket _socket, boolean _debug, int _publishRate) {
		super();

		socket = _socket;
		debug = _debug;
		publishRate = _publishRate;

		if (socket != null) {
			try {
				inFromClient = new ObjectInputStream(socket.getInputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// connectToDevice();

	}

	public ConvertorToSoxHybrid(Socket _socket, ObjectInputStream _inFromClient, SoxDevice _high, SoxDevice _low,
			boolean _debug) {
		super(_socket, _inFromClient, _high, _debug);

		// TODO Auto-generated constructor stub
		lowSpeedDvice = _low;
	}

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
		while (true) {
			try {
				data = (RawSensorData) inFromClient.readObject();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				infoMSG("Connection from " + socket.getInetAddress() + " is interrupted!");
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
														// publishment.
							break;
						} else {
							continue;
						}

					case EVENT_DATA_A:// Other data are stored into the
										// lastVaules list

						lastValues = values;
						continue;
					default:
						continue;

					}
					lastValues.addAll(gpsValues);
					lowSpeedDvice.publishValues(lastValues);
					lastValues = null;
					gpsValues = new LinkedList<TransducerValue>();
					hasLongitude = false;
					hasSpeed = false;
					lowCount = 0;

				}

				debugMSG(se.toString());
			} catch (CarSensorException e) {
				System.err.println(e.getMessage());
			} catch (NotConnectedException e) {
				// TODO Auto-generated catch block
				System.err.println(e.getMessage());
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
			lowSpeedDvice = findSoxDevice(sensorNo);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			device = findSoxDevice(sensorNo + "_100Hz");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		debugMSG("done!");
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
			ObjectInputStream out = new ObjectInputStream(connectionSocket.getInputStream());
			Thread thread = new Thread(new ConvertorToSox(connectionSocket, out, device, true));
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
}
