package jp.ac.keio.sfc.ht.carsensor.client;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jp.ac.keio.sfc.ht.carsensor.protocol.RawSensorData;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEvent;
import jp.ac.keio.sfc.ht.carsensor.protocol.SensorEventListener;
import jp.ac.keio.sfc.ht.carsensor.serialport.SensorSerialReader;
import jp.ac.keio.sfc.ht.carsensor.client.exceptions.*;

public class SensorEventPublisher implements SensorEventListener {
	private static boolean debug = false;
	public static String CLASS_NAME = "SensorEventPublisher";
	public static String SERIAL_PORT = "/dev/tty.usbmodem311";
	public static String SERVER = "carsensor.ht.sfc.keio.ac.jp";
	public static int PORT = 6222;
	private SensorSerialReader sensor = null;
	private BlockingQueue<RawSensorData> dataQueue = new LinkedBlockingQueue<RawSensorData>();
	private boolean publish = false; // if publish = true; the data read from serial port will be enqueued into dataQueue. 
									 //See public void handleSensorEvent(SensorEvent ev) 

	/*private static class CMDPlusTime {
		public CMDPlusTime(byte[] _cmd, long _time) {
			cmd = _cmd;
			time = _time;
		};

		public String toSring() {
			return SensorCMD.bytesToHexString(cmd) + " "
					+ Utility.getFormatedTimestamp(time);
		};

		byte[] cmd;
		long time;
	};*/

	static void debugMSG(String msg) {
		if (debug) {
			Date now = new Date();
			SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");//dd/MM/yyyy
			
			//String strDate = sdfDate.format(now);
			   
			System.out.println("[" + sdfDate.format(now)+ "] " + "["
					+ CLASS_NAME + "] " + msg);
		}
	}
	

	private void connectToSensor() throws SensorConnectingFailedException {
		
			try {

				debugMSG("Connect to sensor...");
				sensor = new SensorSerialReader(SERIAL_PORT, debug);
				debugMSG("Done...");
				debugMSG("Add sensor event listener...");
				sensor.addSensorEventListener(this);
				debugMSG("Done...");				
				return;
			} catch (Exception e) {
				throw new SensorConnectingFailedException("Failed to connect to " + SERIAL_PORT, e);
			}
		
	}

	private void startSensing() throws SensorInitializationErrorException {
		
			
				//sensor.stopSensor();
				
				debugMSG("Initialize Sensor...");
				try {
					sensor.getSensorInfo();
					sensor.getVS();
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					throw new SensorInitializationErrorException ("Otaining Sensor Information failed!", e);
				}
				if ( sensor.isInitialized()){
					try {
						sensor.startSensor();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						throw new SensorInitializationErrorException ("Starting sensing failed!", e);
					}
				}else {
					throw new SensorInitializationErrorException ("Sensor is not initialized!");
				}										
				
				
				//Thread.sleep(1000);
				//sensor.startSensorWithoutFan();
				debugMSG("Done!");
				
			
		

	}

	protected static void parseOptions(String[] args) {
		if (args.length == 0) {
			System.err
					.println("Usage: java -jar SensorEventPublisher.jar -s <Server>  -p <Port>  -sp <serialPort> -debug <true/flase>");
			System.exit(1);
		}
		debugMSG("Parse parameters...");
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-s")) {
				SERVER = args[++i];
			} else if (args[i].equals("-p")) {
				PORT = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-sp")) {
				SERIAL_PORT = args[++i];
			} else if (args[i].equals("-debug")) {
				String debugPara = args[++i];
				if (debugPara.equals("true")) {
					debug = true;
				}
			} else {
				System.err.println("ERROR: invalid option " + args[i]);
				System.err
						.println("Usage: java -jar SensorEventPublisher.jar -s <Server>  -p <Port>  -sp <serialPort> -debug <true/flase>");
				System.exit(1);
			}
		}
	}

	public SensorEventPublisher(String[] args) {
		
		parseOptions(args);
		// connect to the sensor's serial port and start the sensor
		
		try {
			debugMSG("Connect to sensors...");
			connectToSensor();
			debugMSG("Start sensing...");
			startSensing();
			debugMSG("Done!");
		} catch (SensorConnectingFailedException | SensorInitializationErrorException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			reboot();
			
		}

		
		// Add 10 seconds delay as requested in the specification of
		// sensor
		debugMSG("Insert 10 seconds delay...");
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
			if(reconNo > 10 ){// if 
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
				if( reconNo == 0){
					System.out.print("Connect to Server " + SERVER + ":" + PORT + "...");

				}else{
					System.out.print("Reconnect to Server " + SERVER + ":" + PORT +" "+ reconNo + "...");

				}
				socket = new Socket(SERVER, PORT);
				System.out.println("done!");
				reconNo = 0; // Connection succeeds! Set the reconNo to 0;
				// Create input and output streams to read from and write to the
				// server
				out = new ObjectOutputStream(
						socket.getOutputStream());

				// Loop! Read data from the queue and publish it to the server
				publish(out);

			}catch (SensorDataNotComeException e){
				// No more data obtained from the sensor. It is deduced the sensor comes to a halt. Reboot!
				
				e.printStackTrace();
				try {
					if(out != null){
						out.close();
					}
					if(socket != null){
					   socket.close();
					}
										
				} catch ( IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					
				}finally{
					reboot();
				}
				
			} 
			catch ( PublshingIOException  | IOException e ) {
				// Network connection is in error. Try to reconnect the network.
				e.printStackTrace();
				
				//publish = false;
				//dataQueue.clear();
				try {
					if(out != null){
						out.close();
					}
					if(socket != null){
					   socket.close();
					}
										
				} catch ( IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					
				}finally{
					reconNo += 1; // Increase reconNo;
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}				
				
				
			}
			
		}
	}

	
	private void publish(ObjectOutputStream out) throws SensorDataNotComeException, PublshingIOException{
		
		
		RawSensorData data;
		
			
			
			while(true)
			{// 2nd loop; Output the data to proxy server
				try {
					data = dataQueue.poll(1, TimeUnit.MINUTES);
					if (null == data ){
						throw new SensorDataNotComeException("Timeout: 1 min. No data comes from the sensor" + sensor.getSerial());
					}
					debugMSG(data.toString());
					out.writeObject(data);
					out.flush();
					debugMSG("Done!");
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
		System.err.println("Netowrk connnecting failed!");
		System.err.println("Reboot system!");
		reboot();
		System.exit(0);
	}

	private void reboot(){
		
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
