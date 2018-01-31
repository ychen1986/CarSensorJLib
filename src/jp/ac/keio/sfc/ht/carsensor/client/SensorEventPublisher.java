package jp.ac.keio.sfc.ht.carsensor.client;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.TooManyListenersException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.ac.keio.sfc.ht.carsensor.protocol.RawSensorData;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEvent;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEventListener;
import jp.ac.keio.sfc.ht.carsensor.serialport.SensorSerialReader;
import jp.ac.keio.sfc.ht.carsensor.client.exceptions.*;

public class SensorEventPublisher implements SensorEventListener {
	static {
        // set a system property such that Simple Logger will include timestamp
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        // set a system property such that Simple Logger will include timestamp in the given format
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "dd-MM-yy HH:mm:ss");
        
    }
	final Logger logger = LoggerFactory.getLogger(SensorEventPublisher.class);
	public static String SERIAL_PORT = "/dev/sensor";
	public static String SERVER = "carsensor.ht.sfc.keio.ac.jp";
	public static int PORT = 6222;
	private SensorSerialReader sensor = null;
	
	private BlockingQueue<RawSensorData> dataQueue = new LinkedBlockingQueue<RawSensorData>();
	private boolean publish = false; // if publish = true; the data read from
										// serial port will be enqueued into
										// dataQueue.
										// See public void
										// handleSensorEvent(SensorEvent ev)



	private void connectToSensor() throws SensorConnectingFailedException {

		try {

			logger.info("Connect to sensor...");
			sensor = new SensorSerialReader(SERIAL_PORT);
			logger.info("Done...");

			return;
		} catch (Exception e) {
			throw new SensorConnectingFailedException("Failed to connect to " + SERIAL_PORT, e);
		}

	}

	private void startSensing() throws SensorInitializationErrorException {

		// sensor.stopSensor();

		logger.info("Initialize Sensor...");
		
		//if sensor.
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		if(!sensor.isEmptyDataQueue()){
			logger.info("Sensor running detected!");
			try {
				sensor.stopSensor();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				throw new SensorInitializationErrorException("Stop sensor failed.", e);
				
			}
			logger.error("Detect sensor running!");
			
			sensor.clearDataQueue();
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		logger.debug("Add sensor event listener...");
		
		try {
			sensor.addSensorEventListener(this);
		} catch (TooManyListenersException e1) {
			// TODO Auto-generated catch block
			throw new SensorInitializationErrorException("Add sensor event listener failes.", e1);
		}
		logger.debug("Done...");
		
		
		try {
			
			
			sensor.getSensorInfo();
			sensor.getVS();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new SensorInitializationErrorException("Obtaining Sensor Information failed!", e);
		}
		if (sensor.isInitialized()) {
			try {
				sensor.startSensor();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				throw new SensorInitializationErrorException("Starting sensing failed!", e);
			}
		} else {
			throw new SensorInitializationErrorException("Sensor is not initialized!");
		}

		// Thread.sleep(1000);
		// sensor.startSensorWithoutFan();
		logger.debug("Done!");

	}

	static String USAGE_STRING="Usage: java -jar SensorEventPublisher.jar -s <Server>  -p <Port>  -sp <serialPort> -debug <true/flase>";
	
	final  void parseOptions(String[] args) {
		if (args.length == 0) {
			logger.error(USAGE_STRING);
			System.exit(1);
		}
		logger.debug("Parse parameters...");
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-s")) {
				SERVER = args[++i];
			} else if (args[i].equals("-p")) {
				PORT = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-sp")) {
				SERIAL_PORT = args[++i];
			} else {
				logger.error("ERROR: invalid option " + args[i]);
				logger.error(USAGE_STRING);
				System.exit(1);
			}
		}
	}

	public SensorEventPublisher(String[] args) {

		parseOptions(args);
		// connect to the sensor's serial port and start the sensor

		try {
			logger.debug("Connect to sensors...");
			connectToSensor();
			logger.debug("Start sensing...");
			startSensing();
			logger.debug("Done!");
		} catch (SensorConnectingFailedException | SensorInitializationErrorException e2) {
			// TODO Auto-generated catch block
			logger.error("Connection to sensor failed.",e2);
			reboot();

		}

		// Add 10 seconds delay as requested in the specification of
		// sensor
		logger.debug("Insert 10 seconds delay...");
		try {
			Thread.sleep(10 * 1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		publish = true;
		Socket socket = null;
		ObjectOutputStream out = null;

		int reconNo = 0;
		while (true) {// 1st Loop. If network connection is down, reconnect it
			if (reconNo > 10) {// Network is not connected.
				logger.error("Network is not connected!");
				try {
					handlingNetworkDisconnectionEvent();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;

			}
			try {

				// Connect to the server
				if (reconNo == 0) {
					logger.info("Connect to Server " + SERVER + ":" + PORT + "...");

				} else {
					logger.info("Reconnect to Server " + SERVER + ":" + PORT + " " + reconNo + "...");

				}
				socket = new Socket(SERVER, PORT);
				socket.setSoTimeout(1000*10);
				logger.info("done!");
				reconNo = 0; // Connection succeeds! Set the reconNo to 0;
				// Create input and output streams to read from and write to the
				// server
				out = new ObjectOutputStream(socket.getOutputStream());

				// Loop! Read data from the queue and publish it to the server
				publish(out);

			} catch (SensorDataNotComeException e) {
				// No more data obtained from the sensor. It is deduced the
				// sensor comes to a halt. Reboot!

				logger.error("No more data from the sensor", e);
				try {
					if (out != null) {
						out.close();
					}
					if (socket != null) {
						socket.close();
					}

				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();

				} finally {
					reboot();
				}

			} catch (PublshingIOException | IOException e) {
				// Network connection is in error. Try to reconnect the network.
				logger.error(e.getMessage(),e);

				// publish = false;
				// dataQueue.clear();
				try {
					if (out != null) {
						out.close();
					}
					if (socket != null) {
						socket.close();
					}

				} catch (IOException e1) {
					// TODO Auto-generated catch block
					logger.error(e1.getMessage(),e1);

				} finally {
					reconNo += 1; // Increase reconNo;
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						logger.error(e1.getMessage(),e1);
					}
				}

			}

		}
	}

	private void publish(ObjectOutputStream out) throws SensorDataNotComeException, PublshingIOException {

		RawSensorData data;

		while (true) {// 2nd loop; Output the data to proxy server
			try {
				data = dataQueue.poll(1, TimeUnit.MINUTES);
				if (null == data) {
					throw new SensorDataNotComeException(
							"Timeout: 1 min. No data comes from the sensor" + sensor.getSerial());
				}
				logger.debug(data.toString());
				out.writeObject(data);
				out.flush();
				logger.debug("Done!");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new PublshingIOException("Publishing Failed!", e);
			}

		}

	}

	private void handlingNetworkDisconnectionEvent() throws IOException {
		// TODO Auto-generated method stub
		logger.error("Netowrk is not connected");		
		reboot();
		System.exit(0);
	}

	private void reboot() {
		logger.error("Reboot system!");
		Runtime runtime = Runtime.getRuntime();
		try {
			
			runtime.exec("shutdown -r +1");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(-1);

	}

	@Override
	public void handleSensorEvent(SensorEvent ev) throws Exception {
		// Enqueue the sensor and corresponding time stamp
		if (publish) {

			dataQueue.put(new RawSensorData(ev.getCmd(), ev.getTimestamp()));
		}

	}

	public static void main(String[] args) {
		new SensorEventPublisher(args);

	}

}
