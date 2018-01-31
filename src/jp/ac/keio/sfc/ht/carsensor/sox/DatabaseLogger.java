package jp.ac.keio.sfc.ht.carsensor.sox;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.ac.keio.sfc.ht.carsensor.Utility;
import jp.ac.keio.sfc.ht.sox.protocol.TransducerValue;
import jp.ac.keio.sfc.ht.sox.soxlib.event.SoxEvent;

public class DatabaseLogger extends Monitor implements Runnable {
	final static Logger logger = LoggerFactory.getLogger(DatabaseLogger.class);
	 String sox;
	 String deviceName;
	 String beacon_mac;
	 String db_userName;
	 String db_password;
	 String db_url;
	 String db_driver;
	 Connection conn = null;
	 BlockingQueue<SoxEvent> data_buffer;
	
	public DatabaseLogger(String sox, String deviceName, String beacon_mac, String db_url, String db_driver, String db_userName,String db_password) {
		
		super(sox, deviceName);
		this.sox = sox;
		this.deviceName = deviceName;
		this.db_driver = db_driver;
		this.db_password = db_password;
		this.db_url = db_url;
		this.db_userName = db_userName;
		this.beacon_mac = beacon_mac;
		
		this.data_buffer = new LinkedBlockingQueue<SoxEvent> ();
		
		
		
		try {
			Class.forName(db_driver).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			logger.error("Loading database driver failed",e);
			System.exit(1);
		}
		
		new Thread(this).run();
		
		
	}

	public static void main(String[] args) {
		String sox = null;
		String deviceName = null;
		String beacon_mac = null;
		String db_url = null;
		String db_driver = null;
		String db_userName = null;
		String db_password = null;
		// Parse the arguments -
		for (int i=0; i<args.length; i++) {
			// Check this is a valid argument
			if (args[i].startsWith("-")) {
				String arg = args[i];
				// Handle arguments that take no-value
				switch(arg) {
					case "-help": case "?":	printHelp(); return;
				}

				// Now handle the arguments that take a value and
				// ensure one is specified
				if (i == args.length -1 || args[i+1].charAt(0) == '-') {
					System.out.println("Missing value for argument: "+args[i]);
					printHelp();
					return;
				}
				switch(arg) {
					case "-s": sox = args[++i];               break;
					case "-d": deviceName = args[++i];               break;
					case "-b": beacon_mac = args[++i];               break;
					case "-du": db_userName = args[++i];               break;
					case "-dz": db_password = args[++i];               break;
					case "-durl": db_url = args[++i];               break;
					case "-ddr": db_driver = args[++i];               break;
					default:
						System.out.println("Unrecognised argument: "+args[i]);
						printHelp();
						return;
				}
			} else {
				System.out.println("Unrecognised argument: "+args[i]);
				printHelp();
				return;
			}

		}
		@SuppressWarnings("unused")
		DatabaseLogger recorder = new DatabaseLogger(sox, deviceName,beacon_mac, db_url, db_driver, db_userName, db_password);
	}
	private static void printHelp(){
	      System.out.println(
		          "Syntax:\n\n" +
		              "    DatabaseLogger [-help] " +
		              "\n\n Sox Options \n"+
		              "    -s  soxserver" +
		              "    -d  soxdevice" +
		              "     \n\n Database Options \n" +
		              "     -u Username \n" +
		              "     -z Password \n" +
		              "     -durl Database URL \n" +
		              "     -ddr Database driver \n" +
		              "     -du Database Username \n" +
		              "     -dz Database Password \n" +
		              "     \n\n SSL Options \n" +
		              "    -v  SSL enabled; true - (default is false) "
		          );
	}
		
	@Override
	public void handlePublishedSoxEvent(SoxEvent e) {
		
		
		data_buffer.add(e);
		
	
	}

	
	@Override
	public void run() {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		SoxEvent data = null;
		BigDecimal lat;
		BigDecimal lng;
		String sensor_name;
		long time;
		final String sql = "INSERT INTO `omimamori_DB`.`Beacon_Location` (`lat`,`lng`,`beacon_mac`,`sensor_name`,`time`) "+
		"VALUES(?,?,?,?,?)";
		while(true){
			
			try {
				if( data == null ){
					// if mqttdata is null then take a new element from data_buffer
					// it is not, then it means a SQL insert operation failed, 
					// so we need to try it again. The mqttdata is update in the condition 
					// of while loop below.
					data = data_buffer.take();
				}

				
				if(conn == null || conn.isClosed()){
					conn =  DriverManager.getConnection(this.db_url,this.db_userName,this.db_password);
				}
				
				
				do{
				  
					lat = null;
					lng = null;
					time = 0;
					sensor_name = null;
					List<TransducerValue> values = data.getTransducerValues();
					sensor_name = data.getDevice().getName();
					logger.debug("Data received from data node:" + sensor_name);

					
					for (TransducerValue value : values) {
						
						switch(value.getId()){
							case "Latitude": lat = new BigDecimal(value.getRawValue());  value.getTimestamp(); break;
							case "Longitude": lng = new BigDecimal(value.getRawValue()); 
							try {
								time = Utility.getUNIXTime(value.getTimestamp()) ;
							} catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} break;	
							default: break;
						}


					}
					if(lat == null || lng == null || sensor_name == null || time == 0){
						// data field is missing. Ignore and continue;
					
						continue;
					}

					pstmt = conn.prepareStatement(sql);
					pstmt.setBigDecimal(1, lat);
					pstmt.setBigDecimal(2, lng);;
					pstmt.setString(3,this.beacon_mac);
					pstmt.setString(4,sensor_name);
					pstmt.setTimestamp(5,new Timestamp(time));
				
					logger.debug(pstmt.toString());

					pstmt.execute();
					
					logger.debug("Done!");	
					
					
				}while((data = data_buffer.poll(1,TimeUnit.MINUTES)) != null);// insert until the buffer is empty within 1 minite.
				logger.info("No more data is avaiable. Close database connection.");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logger.error("",e);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.error("Connecting to database "+this.db_url+" failed",e);
			}finally{
				try { rs.close(); } catch (Exception e) { /* ignored */ } finally{};
			    try { pstmt.close(); } catch (Exception e) { /* ignored */ }finally{};
			    try { conn.close(); } catch (Exception e) { /* ignored */ }finally{};
			}
		}
		
	}

}
