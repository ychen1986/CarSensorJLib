package jp.ac.keio.sfc.ht.carsensor.sox;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import jp.ac.keio.sfc.ht.carsensor.Utility;
import jp.ac.keio.sfc.ht.sox.protocol.TransducerValue;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxConnection;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxDevice;
import jp.ac.keio.sfc.ht.sox.soxlib.event.SoxEvent;
import jp.ac.keio.sfc.ht.sox.soxlib.event.SoxEventListener;

public class Monitor implements SoxEventListener {

	public Monitor(String sox, String deviceName) {
		// TODO Auto-generated constructor stub
		SoxConnection soxConnection = null;
		try {
			 soxConnection = new SoxConnection(sox, false);
		} catch (SmackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SoxDevice device = null;
		try {
			 device = new SoxDevice(soxConnection, deviceName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		device.subscribe();
		device.addSoxEventListener(this);
		
	}

	@Override
	public void handlePublishedSoxEvent(SoxEvent e) {
		// TODO Auto-generated method stub
		List<TransducerValue> values = e.getTransducerValues();
		long arrival_time = e.getTime();
		long delay = 0;
		String timestamp = null;
		System.out.println("[info]data received from data node:" + e.getDevice().getName());
		for(TransducerValue value: values){
			System.out.println(value.getId());
			System.out.println(value.getRawValue());
			System.out.println(value.getTypedValue());
			System.out.println(value.getTimestamp());
//			try{
//				timestamp = value.getTimestamp();
//				long send_time = Utility.getUNIXTime(timestamp);
//				delay = arrival_time - send_time;
//				break;
//				
//			}catch(ParseException pe){
//				pe.printStackTrace();
//				continue;
//			}
			
			
		}
//		System.out.println(Utility.getFormatedTimestamp(arrival_time)+"    "+ delay);

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String node, sox;
		sox = "soxfujisawa.ht.sfc.keio.ac.jp";
		node = "carsensor004";
		//node = args[0];
		//node = "FujisawaCarSensorRaw3";
		//node = "FujisawaCarSensorRaw";
		if(args.length != 0 ){
			
			sox = args[0];
			node = args[1];
			
		}else{
			System.out.println("Usage: <soxserver> <nodename>");
			System.exit(-1);
		}
		new Monitor(sox, node);
		while (true) {
			try {
				Thread.sleep(1000*60);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			};
		}
	}

}
