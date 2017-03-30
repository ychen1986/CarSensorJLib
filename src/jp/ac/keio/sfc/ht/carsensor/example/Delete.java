package jp.ac.keio.sfc.ht.carsensor.example;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.PublishModel;

import jp.ac.keio.sfc.ht.sox.protocol.Device;
import jp.ac.keio.sfc.ht.sox.protocol.DeviceType;
import jp.ac.keio.sfc.ht.sox.protocol.Transducer;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxConnection;

public class Delete {

	public static void main(String[] args) {
		new Delete();

	}

	public Delete() {

		String prefix = "carsensor";
		String sensorName;
		SoxConnection con;

		//
		// for(int i=1; i<= 100; i++){
		// sensorName = prefix + String.format("%03d", i);
		sensorName = "carsensor_test";
		try {
			System.out.println("Connecting to Sox");
			con = new SoxConnection("133.27.171.88", "htcarsensor", "carsensor", false);
			return;
			// System.out.println("Delete device " + sensorName);
			// con.deleteNode(sensorName);
			// System.out.println("Done");
		} catch (NoResponseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMPPErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		// }

	}

}