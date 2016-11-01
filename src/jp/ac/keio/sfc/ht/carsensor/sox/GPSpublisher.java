package jp.ac.keio.sfc.ht.carsensor.sox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import jp.ac.keio.sfc.ht.sox.protocol.TransducerValue;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxConnection;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxDevice;

public class GPSpublisher {

	static BufferedReader in = null;
	
	public GPSpublisher() throws Exception {

		SoxConnection con = new SoxConnection("sox.ht.sfc.keio.ac.jp",  false); //anonymous login
		con.deleteNode("carsensor115");
		con.deleteNode("carsensor115_100Hz");
		//SoxConnection con = new SoxConnection("sox.ht.sfc.keio.ac.jp", "guest","miroguest", false);
		
		SoxDevice soxDevice = new SoxDevice(con, "carsensor001");
		
		
		
		//SoxDevice soxDevice = new SoxDevice(con, "testNode","takusox.ht.sfc.keio.ac.jp"); //you can specify another SOX server where the node exists

//		/** Getting Device Meta Data **/
//		Device deviceInfo = soxDevice.getDevice();
//
//		System.out.println("[Device Meta Info] ID:" + deviceInfo.getId()
//				+ ", Name:" + deviceInfo.getName() + " Type:"
//				+ deviceInfo.getDeviceType().toString());
//		List<Transducer> transducerList = deviceInfo.getTransducers();
//		for (Transducer t : transducerList) {
//			System.out.println("[Transducer] Name:" + t.getName() + ", ID:"
//					+ t.getId() + ", unit:" + t.getUnits() + ", minValue:"
//					+ t.getMinValue() + ", maxValue:" + t.getMaxValue());
//		}
		


		while (true) {
		
			Thread.sleep(100);
			String line = in.readLine();
			if(line == null){
				break;
			}
			String[] paras = line.split("\t");
			String latitude = paras[0];
			String longtitude = paras[1];
			
			System.out.println(latitude +","+longtitude);
			List<TransducerValue> valueList = new ArrayList<TransducerValue>();

			TransducerValue value2 = new TransducerValue();
			value2.setId("Latitude");
			value2.setRawValue(latitude); 
			value2.setTypedValue(latitude);
			value2.setCurrentTimestamp();
			
			valueList.add(value2);

			TransducerValue value3 = new TransducerValue();
			value3.setId("Longitude");
			value3.setRawValue(longtitude); 
			value3.setTypedValue(longtitude); 
			value3.setCurrentTimestamp();
			
			
			
			soxDevice.publishValues(valueList);
			
			System.out.println("Published !");
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}


	public static void main(String[] args) throws IOException  {
		// TODO Auto-generated method stub
		SoxConnection con;
		
		for (int i = 176 ; i <= 176; i ++){
			try {
				String sensorName = "carsensor" + Integer.toString(i);
				con = new SoxConnection("sox.ht.sfc.keio.ac.jp","guest","miroguest", true);
				System.out.println(sensorName);
				//System.out.println(x);
				con.deleteNode(sensorName);
				//con.deleteNode(sensorName+"_100Hz");
			} catch (SmackException | XMPPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //anonymous login
		}
		
		
//		File file = new File(args[0]);
//        if(!file.exists()){
//            System.err.println("File "+file+" doesn't exist.");
//            return;
//        }
//        
//        in = new BufferedReader(new FileReader(file));
//        try {
//			new GPSpublisher();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

}
