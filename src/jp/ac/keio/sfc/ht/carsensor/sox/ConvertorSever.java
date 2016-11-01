package jp.ac.keio.sfc.ht.carsensor.sox;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import jp.ac.keio.sfc.ht.carsensor.SensorCMD;
import jp.ac.keio.sfc.ht.carsensor.protocol.RawSensorData;
import jp.ac.keio.sfc.ht.carsensor.sox.ConvertorToSoxHybrid;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxConnection;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxDevice;

public class ConvertorSever implements AutoCloseable{

// properties
	
	final static String SENSOR_NAMING_PREFIX = "carsensor";
	final static int CONECTION_TIME_OUT = 1000 * 1;
	private static int MAX_CONVERTOR_NO = 100;
	
	
	private static final String CLASS_NAME = "ConvertorSever";
	private static boolean debug = false;
	private static int port = 6222;
	private static int sensorPerSoxconnection = 10;
	private static boolean CONFIG_FLAG = false; // CONFIG_FLAG = true indicates
												// that command
												// line parameters will be used
												// rather than those of
												// configure file.
	//sox server related 
	protected static String soxServer = "sox.ht.sfc.keio.ac.jp"; // hostname of the server to which the sensor datas are published
	protected static String soxUser = "guest";                   // username of the sox server
	protected static String soxPasswd = "miroguest";			 // pw for the username   
	
	
	
	static int publish_rate = 100;                               // publish rate in Hz (1 ~ 100)
	//BlockingQueue<Socket> socketQueue = new LinkedBlockingQueue<Socket>();
	ServerSocket welcomeSocket = null;


	protected static void parseOptions(String[] args) {
		
		String usage =  "Usage: java -jar ConvertorServer.jar  -o <port> -d <deviceNoPerSoxconnection> -n <MAX_CONVERTOR_NO> -s <soxServer>  -u <soxUser> -p <soxPasswd> -r <publishRate> -debug <true/flase>";
		if (args.length == 0) {
			System.err.println("ERROR: arguments required!");
			System.err
					.println(usage);
			System.exit(1);
		}
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-s")) {
				soxServer = args[++i];
			} else if (args[i].equals("-o")) {
				port = Integer.parseInt(args[++i]) ;
			} else if (args[i].equals("-d")) {
				sensorPerSoxconnection = Integer.parseInt(args[++i]) ;
			} else if (args[i].equals("-n")) {
				MAX_CONVERTOR_NO = Integer.parseInt(args[++i]) ;
			} else if (args[i].equals("-u")) {
				soxUser = args[++i];
			} else if (args[i].equals("-p")) {
				soxPasswd = args[++i];
			} else if (args[i].equals("-r")) {
				publish_rate = Integer.parseInt(args[++i]);
			} else if(args[i].equals("-debug")){
				String debugPara= args[++i];
				if (debugPara.equals("true")){
					debug = true;
				}
			}else {
				System.err.println("ERROR: invalid option " + args[i]);
				System.err
						.println(usage);
				System.exit(1);
			}
		}
	}

	public ConvertorSever(){
		this(false);
	}
	
	public ConvertorSever(boolean _debug) {
		// construct a connection to sox serve
		this.debug = _debug;

		//

		// Construct server socket
		
		try {
			debugMSG("Creating new server socket at port: " + this.port);
			// Create a server socket
			welcomeSocket = new ServerSocket(port);
			debugMSG("done!");
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			debugMSG("fail!");
			e2.printStackTrace();
			close();
			System.exit(-1);
		}
		Socket connectionSocket = null;
		//ObjectInputStream in = null ;


//		for (int i = 0;  i <= CONVERTOR_NO; i++){ // create all the required sox devices
			
//			debugMSG("Creat convertor thread..." + i);
//			ConvertorToSoxHybrid convertor = new ConvertorToSoxHybrid(null,soxServer,soxUser,soxPasswd,debug,publish_rate);
//			Thread thread = new Thread(convertor);
//			convertorMap.put("", thread);
//			debugMSG("done!");
			
			
			
//		}
		connectToSoxDevices(MAX_CONVERTOR_NO, sensorPerSoxconnection,soxServer,soxUser,soxPasswd );
		
		
		ExecutorService executor = Executors.newFixedThreadPool(MAX_CONVERTOR_NO);
		int requestNO = 1;
		while (true) {
			
			try {
				
				debugMSG("Waiting for client request...");
				
				connectionSocket = welcomeSocket.accept();
				//socketQueue.put(connectionSocket);
				debugMSG("Request: "+ (requestNO++) + " from " +connectionSocket.getInetAddress() );
				/*					
				in = new ObjectInputStream(connectionSocket.getInputStream());
				
				RawSensorData data = (RawSensorData) in.readObject();
				
				String sensorNo = getSensorNO(data);
				debugMSG("Sensor Number:" + sensorNo);
				debugMSG("Creat sox device...");
				SoxDevice device = findSoxDevice(sensorNo);
				debugMSG("done!");*/
				debugMSG("Creat convertor thread...");
				//connectionSocket.setSoTimeout(CONECTION_TIME_OUT);
				//ConvertorToSox convertor = new ConvertorToSox(connectionSocket,false,publish_rate);
				//ConvertorToSoxHybrid convertor = new ConvertorToSoxHybrid(connectionSocket,false,publish_rate);
				//ConvertorToSoxHybrid convertor = new ConvertorToSoxHybrid(connectionSocket,soxServer,soxUser,soxPasswd,debug,publish_rate);
				//Thread thread = new Thread(convertor);
				Runnable convertor = new ConvertorToSoxHybrid(connectionSocket,debug,publish_rate);								
				executor.execute(convertor);
				debugMSG("done!");
				
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				debugMSG("fail!");
				e.printStackTrace();
			}
			
			
			
			
			

		}
		
	}
	public static void connectToSoxDevices( int no, int dps, String _soxServer, String _soxUser, String _soxPasswd){
		
		
		ExecutorService executor = Executors.newFixedThreadPool(10);
		SoxConnection[] cons = new  SoxConnection[ (no+dps-1) / dps ];
		try {
			for(int  i = 0; i < cons.length; i++){
			
				//cons[i] = new SoxConnection(_soxServer, _soxUser, _soxPasswd, false);
				cons[i] = new SoxConnection(_soxServer, false);
			
			}
		} catch (SmackException | IOException | XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		for (int i = 0;  i < no; i++){ // create all the required sox devices
			
//			debugMSG("Creat convertor thread..." + i);
//			ConvertorToSoxHybrid convertor = new ConvertorToSoxHybrid(null,soxServer,soxUser,soxPasswd,debug,publish_rate);
//			Thread thread = new Thread(convertor);
//			convertorMap.put("", thread);
//			debugMSG("done!");						
			
			String deviceName = ConvertorToSoxHybrid.sensorNaming(String.format("%03d",i));
			SoxConnection con = cons[i / dps];
			Runnable connector1 = new DeviceConnector(con,deviceName);
			Runnable connector2 = new DeviceConnector(con,deviceName+"_100Hz");
			executor.execute(connector1);
			executor.execute(connector2);			
		}
		
		
		
	}
	static class DeviceConnector implements Runnable{
		private String deviceName;
		private SoxConnection con;
		public  DeviceConnector(SoxConnection _con, String _deviceName){
			deviceName = _deviceName;
			con = _con;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(deviceName != null && con != null){
				for (int i = 0; i < 5; i++) {
					try {
						if (i == 0) {
							debugMSG("Connect to device " + deviceName + "...");
						}
						SoxDevice dev = new SoxDevice(con, deviceName);
						debugMSG("done!");
						ConvertorToSoxHybrid.publishDeviceMap.put(deviceName, dev);
						return;
					} catch (Exception e) {
						debugMSG("failed!");				
						e.printStackTrace();

					}
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		}
		
		
		
	}



	/**
	 * 
	 * @param data: a raw  data sent from a sensor
	 * @return the text of the serial number 
	 */
	private static String getSensorNO(RawSensorData data) {
		// TODO Auto-generated method stub
		byte [] cmd = new byte[SensorCMD.EVENT_DATA_A_SERO_SIZE];
		for(int i =0; i < cmd.length; i++){
			cmd[i] = data.cmd[1+i];
		}
		
		
		return SensorCMD.littleEndianBytesToDecimalString(cmd,0,cmd.length);
	}

	public static void main(String[] args) {
		// parse cmd line args
		parseOptions(args);
		new ConvertorSever(debug);
	}

	


	static void debugMSG(String msg) {
		if (debug) {
			Date now = new Date();
			SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");//dd/MM/yyyy
			
			String strDate = sdfDate.format(now);
			   
			System.out.println("[" + sdfDate.format(now)+ "] " + "["
					+ CLASS_NAME + "] " + msg);
		}
	}


	@Override
	public void close() {
		// TODO Auto-generated method stub
		if(welcomeSocket != null){
			try {
				welcomeSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	

}
